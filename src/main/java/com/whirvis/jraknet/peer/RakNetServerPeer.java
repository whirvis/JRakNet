/*
 *       _   _____            _      _   _          _
 *      | | |  __ \          | |    | \ | |        | |
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.whirvis.jraknet.peer;

import static com.whirvis.jraknet.RakNetPacket.*;

import java.net.InetSocketAddress;

import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.login.ConnectionRequestAccepted;
import com.whirvis.jraknet.protocol.login.NewIncomingConnection;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;

import io.netty.channel.Channel;

/**
 * A server connection that handles login and other server related protocols.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public final class RakNetServerPeer extends RakNetPeer implements RakNetPeerMessenger {

	private final RakNetClient client;
	private EncapsulatedPacket loginRecord;
	private long timestamp;

	/**
	 * Creates a RakNet server peer.
	 * 
	 * @param client
	 *            the client that is connected to the server.
	 * @param address
	 *            the address of the peer.
	 * @param guid
	 *            the globally unique ID of the peer.
	 * @param maximumTransferUnit
	 *            the maximum transfer unit of the peer.
	 * @param connectionType
	 *            the connection type of the peer.
	 * @param channel
	 *            the channel to communicate to the peer with.
	 */
	public RakNetServerPeer(RakNetClient client, InetSocketAddress address, long guid, int maximumTransferUnit, ConnectionType connectionType, Channel channel) {
		super(address, guid, maximumTransferUnit, connectionType, channel);
		this.client = client;

		/*
		 * By the time this object is created, handshaking has begun between the
		 * server and client to finish login after connection.
		 */
		this.setState(RakNetState.HANDSHAKING);
	}

	@Override
	public long getTimestamp() {
		if (this.isLoggedIn()) {
			return System.currentTimeMillis() - timestamp;
		}
		return -1L;
	}

	@Override
	public void handleMessage(RakNetPacket packet, int channel) {
		if (packet.getId() == ID_CONNECTION_REQUEST_ACCEPTED && this.isHandshaking()) {
			ConnectionRequestAccepted connectionRequestAccepted = new ConnectionRequestAccepted(packet);
			connectionRequestAccepted.decode();
			if (!connectionRequestAccepted.failed()) {
				NewIncomingConnection newIncomingConnection = new NewIncomingConnection();
				newIncomingConnection.serverAddress = this.getAddress();
				newIncomingConnection.clientTimestamp = connectionRequestAccepted.clientTimestamp;
				newIncomingConnection.serverTimestamp = connectionRequestAccepted.serverTimestamp;
				newIncomingConnection.encode();
				if (!newIncomingConnection.failed()) {
					this.loginRecord = this.sendMessage(Reliability.RELIABLE_ORDERED_WITH_ACK_RECEIPT, newIncomingConnection);
					this.getLogger().debug("Sent new incoming connection, waiting for acknowledgement before confirming login");
				} else {
					client.disconnect("Failed to login (" + newIncomingConnection.getClass().getSimpleName() + " failed to encode)");
				}
			} else {
				client.disconnect("Failed to login (" + connectionRequestAccepted.getClass().getSimpleName() + " failed to decode)");
			}
		} else if (packet.getId() == ID_DISCONNECTION_NOTIFICATION) {
			client.disconnect("Server disconnected");
		} else if (packet.getId() >= ID_USER_PACKET_ENUM) {
			client.callEvent(listener -> listener.handleMessage(client, this, packet, channel));
		} else {
			client.callEvent(listener -> listener.handleUnknownMessage(client, this, packet, channel));
		}
	}

	@Override
	public void onAcknowledge(Record record, EncapsulatedPacket packet) {
		if (record.equals(loginRecord.ackRecord)) {
			this.timestamp = System.currentTimeMillis();
			this.setState(RakNetState.LOGGED_IN);
			this.getLogger().info("Logged in to server with globally unique ID " + Long.toHexString(this.getGloballyUniqueId()).toUpperCase());
			client.callEvent(listener -> listener.onLogin(client, this));
		}
		client.callEvent(listener -> listener.onAcknowledge(client, this, record, packet));
	}

	@Override
	public void onNotAcknowledge(Record record, EncapsulatedPacket packet) {
		client.callEvent(listener -> listener.onLoss(client, this, record, packet));
	}

}
