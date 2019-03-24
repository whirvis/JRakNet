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
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.login.ConnectionRequest;
import com.whirvis.jraknet.protocol.login.ConnectionRequestAccepted;
import com.whirvis.jraknet.protocol.login.NewIncomingConnection;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;
import com.whirvis.jraknet.server.RakNetServer;

import io.netty.channel.Channel;

/**
 * A client connection that handles login and other client related protocols.
 *
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public final class RakNetClientPeer extends RakNetPeer {

	private final RakNetServer server;
	private long timestamp;

	/**
	 * Creates a RakNet client peer.
	 * 
	 * @param server
	 *            the server that is hosting the connection to the client.
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
	public RakNetClientPeer(RakNetServer server, ConnectionType connectionType, long guid, int maximumTransferUnit,
			Channel channel, InetSocketAddress address) {
		super(address, guid, maximumTransferUnit, connectionType, channel);
		this.server = server;
	}

	/**
	 * Returns the server this session is connected to.
	 * 
	 * @return the server this session is connected to.
	 */
	public RakNetServer getServer() {
		return this.server;
	}

	@Override
	public long getTimestamp() {
		if (this.getState() != RakNetState.LOGGED_IN) {
			return -1L;
		}
		return System.currentTimeMillis() - timestamp;
	}

	@Override
	public void handleMessage(RakNetPacket packet, int channel) {
		if (packet.getId() == ID_CONNECTION_REQUEST && this.getState() == RakNetState.DISCONNECTED) {
			ConnectionRequest request = new ConnectionRequest(packet);
			request.decode();
			if (request.clientGuid == this.getGloballyUniqueId() && request.useSecurity == false) {
				ConnectionRequestAccepted requestAccepted = new ConnectionRequestAccepted();
				requestAccepted.clientAddress = this.getAddress();
				requestAccepted.clientTimestamp = request.timestamp;
				requestAccepted.serverTimestamp = server.getTimestamp();
				requestAccepted.encode();
				if (!requestAccepted.failed()) {
					this.sendMessage(Reliability.RELIABLE_ORDERED, requestAccepted);
					this.setState(RakNetState.HANDSHAKING);
				} else {
					server.disconnectClient(this,
							"Login failed (" + requestAccepted.getClass().getSimpleName() + " failed to encode)");
				}
			} else {
				String reason = "unknown error";
				if (request.clientGuid != this.getGloballyUniqueId()) {
					reason = "client GUID does not match";
				} else if (request.useSecurity == true) {
					reason = "client has security enabled";
				}
				this.sendMessage(Reliability.UNRELIABLE, ID_CONNECTION_ATTEMPT_FAILED);
				server.disconnectClient(this, "Login failed (" + reason + ")");
			}
		} else if (packet.getId() == ID_NEW_INCOMING_CONNECTION && this.getState() == RakNetState.HANDSHAKING) {
			NewIncomingConnection clientHandshake = new NewIncomingConnection(packet);
			clientHandshake.decode();
			if (!clientHandshake.failed()) {
				this.timestamp = System.currentTimeMillis() - clientHandshake.clientTimestamp;
				this.setState(RakNetState.LOGGED_IN);
				server.callEvent(listener -> listener.onLogin(server, this));
			} else {
				server.disconnectClient(this,
						"Failed to login (" + clientHandshake.getClass().getSimpleName() + " failed to decode)");
			}
		} else if (packet.getId() == ID_DISCONNECTION_NOTIFICATION) {
			server.disconnectClient(this, "Disconnected");
		} else if (packet.getId() >= ID_USER_PACKET_ENUM) {
			server.callEvent(listener -> listener.handleMessage(server, this, packet, channel));
		} else {
			server.callEvent(listener -> listener.handleUnknownMessage(server, this, packet, channel));
		}
	}

	@Override
	public void onAcknowledge(Record record, EncapsulatedPacket packet) {
		server.callEvent(listener -> listener.onAcknowledge(server, this, record, packet));
	}

	@Override
	public void onNotAcknowledge(Record record, EncapsulatedPacket packet) {
		server.callEvent(listener -> listener.onLoss(server, this, record, packet));
	}

}
