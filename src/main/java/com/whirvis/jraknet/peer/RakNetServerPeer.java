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
 * Copyright (c) 2016-2019 Trent Summerlin
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
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public class RakNetServerPeer extends RakNetPeer implements RakNetPeerMessenger {

	private final RakNetClient client;
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
	public RakNetServerPeer(RakNetClient client, InetSocketAddress address, long guid, int maximumTransferUnit,
			ConnectionType connectionType, Channel channel) {
		super(address, guid, maximumTransferUnit, connectionType, channel);
		this.client = client;
		this.timestamp = -1;

		/*
		 * By the time this object is created, handshaking has begun between the
		 * server and client to finish login after connection.
		 */
		this.setState(RakNetState.HANDSHAKING);
	}
	
	@Override
	public long getTimestamp() {
		if (timestamp < 0) {
			return -1L; // Not yet logged in
		}
		return System.currentTimeMillis() - this.timestamp;
	}

	@Override
	public void handleMessage(RakNetPacket packet, int channel) {
		short packetId = packet.getId();
		if (packetId == ID_CONNECTION_REQUEST_ACCEPTED && this.getState() == RakNetState.HANDSHAKING) {
			ConnectionRequestAccepted connectionRequestAccepted = new ConnectionRequestAccepted(packet);
			connectionRequestAccepted.decode();
			if (!connectionRequestAccepted.failed()) {
				NewIncomingConnection newIncomingConnection = new NewIncomingConnection();
				newIncomingConnection.serverAddress = this.getAddress();
				newIncomingConnection.clientTimestamp = connectionRequestAccepted.clientTimestamp;
				newIncomingConnection.serverTimestamp = connectionRequestAccepted.serverTimestamp;
				newIncomingConnection.encode();
				if (!newIncomingConnection.failed()) {
					this.sendMessage(Reliability.RELIABLE, newIncomingConnection);
					this.timestamp = System.currentTimeMillis();
					this.setState(RakNetState.LOGGED_IN); // TODO: Wait for ack?
					client.callEvent(listener -> listener.onLogin(client, this));
				} else {
					client.disconnect("Failed to login");
				}
			} else {
				client.disconnect("Failed to login");
			}
		} else if (packetId == ID_DISCONNECTION_NOTIFICATION) {
			this.setState(RakNetState.DISCONNECTED);
			client.disconnect("Server disconnected");
		} else if (packetId >= ID_USER_PACKET_ENUM) {
			client.callEvent(listener -> listener.handleMessage(client, this, packet, channel));
		} else {
			client.callEvent(listener -> listener.handleUnknownMessage(client, this, packet, channel));
		}
	}

	@Override
	public void onAcknowledge(Record record, EncapsulatedPacket packet) {
		client.callEvent(listener -> listener.onAcknowledge(client, this, record, packet));
	}

	@Override
	public void onNotAcknowledge(Record record, EncapsulatedPacket packet) {
		client.callEvent(listener -> listener.onNotAcknowledge(client, this, record, packet));
	}

	/**
	 * Closes the connection with the server by sending it a
	 * <code>DISCONNECTION_NOTIFICATION</code> packet.
	 */
	public void closeConnection() {
		/*
		 * Clear the send queue to make sure the disconnect packet is first in
		 * line to be sent. The disconnection notification packet has been sent,
		 * forcefully update the session to ensure the packet is sent out.
		 */
		sendQueue.clear();
		this.sendMessage(Reliability.UNRELIABLE, ID_DISCONNECTION_NOTIFICATION);
		this.update();
	}

}
