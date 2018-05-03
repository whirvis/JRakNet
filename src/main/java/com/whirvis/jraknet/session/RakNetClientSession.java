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
 * Copyright (c) 2016-2018 Whirvis T. Wheatley
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
package com.whirvis.jraknet.session;

import static com.whirvis.jraknet.protocol.MessageIdentifier.*;

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
import com.whirvis.jraknet.server.RakNetServerListener;

import io.netty.channel.Channel;

/**
 * This class represents a client connection and handles the login sequence
 * packets.
 *
 * @author Whirvis T. Wheatley
 */
public class RakNetClientSession extends RakNetSession {

	private final RakNetServer server;
	private final long timeCreated;
	private long timestamp;

	/**
	 * Constructs a <code>RakNetClientSession</code> with the specified
	 * <code>RakNetServer</code>, the time the server was created, globally
	 * unique ID, maximum transfer unit, <code>Channel</code>, and address.
	 * 
	 * @param server
	 *            the <code>RakNetServer</code>.
	 * @param timeCreated
	 *            the time the server was created.
	 * @param connectionType
	 *            the connection type of the session.
	 * @param guid
	 *            the globally unique ID.
	 * @param maximumTransferUnit
	 *            the maximum transfer unit
	 * @param channel
	 *            the <code>Channel</code>.
	 * @param address
	 *            the address.
	 */
	public RakNetClientSession(RakNetServer server, long timeCreated, ConnectionType connectionType, long guid,
			int maximumTransferUnit, Channel channel, InetSocketAddress address) {
		super(connectionType, guid, maximumTransferUnit, channel, address);
		this.server = server;
		this.timeCreated = timeCreated;
	}

	/**
	 * @return the server this session is connected to.
	 */
	public RakNetServer getServer() {
		return this.server;
	}

	/**
	 * @return the time this session was created.
	 */
	public long getTimeCreated() {
		return this.timeCreated;
	}

	/**
	 * @return the client's timestamp.
	 */
	public long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	@Override
	public void onAcknowledge(Record record, EncapsulatedPacket packet) {
		for (RakNetServerListener listener : server.getListeners()) {
			listener.onAcknowledge(this, record, packet);
		}
	}

	@Override
	public void onNotAcknowledge(Record record, EncapsulatedPacket packet) {
		for (RakNetServerListener listener : server.getListeners()) {
			listener.onNotAcknowledge(this, record, packet);
		}
	}

	@Override
	public void handleMessage(RakNetPacket packet, int channel) {
		short packetId = packet.getId();

		if (packetId == ID_CONNECTION_REQUEST && this.getState() == RakNetState.DISCONNECTED) {
			ConnectionRequest request = new ConnectionRequest(packet);
			request.decode();

			if (request.clientGuid == this.getGloballyUniqueId() && request.useSecurity != true) {
				ConnectionRequestAccepted requestAccepted = new ConnectionRequestAccepted();
				requestAccepted.clientAddress = this.getAddress();
				requestAccepted.clientTimestamp = request.timestamp;
				requestAccepted.serverTimestamp = server.getTimestamp();
				requestAccepted.encode();

				if (!requestAccepted.failed()) {
					this.timestamp = (System.currentTimeMillis() - request.timestamp);
					this.sendMessage(Reliability.RELIABLE_ORDERED, requestAccepted);
					this.setState(RakNetState.HANDSHAKING);
				} else {
					server.removeSession(this, "Login failed, " + ConnectionRequestAccepted.class.getSimpleName()
							+ " packet failed to encode");
				}
			} else {
				String reason = "unknown error";
				if (request.clientGuid != this.getGloballyUniqueId()) {
					reason = "client GUID did not match";
				} else if (request.useSecurity == true) {
					reason = "client has security enabled";
				}
				this.sendMessage(Reliability.RELIABLE, ID_CONNECTION_ATTEMPT_FAILED);
				this.setState(RakNetState.DISCONNECTED);
				server.removeSession(this, "Login failed, " + reason);
			}
		} else if (packetId == ID_NEW_INCOMING_CONNECTION && this.getState() == RakNetState.HANDSHAKING) {
			NewIncomingConnection clientHandshake = new NewIncomingConnection(packet);
			clientHandshake.decode();

			if (!clientHandshake.failed()) {
				this.timestamp = (System.currentTimeMillis() - clientHandshake.clientTimestamp);
				this.setState(RakNetState.CONNECTED);
				for (RakNetServerListener listener : server.getListeners()) {
					listener.onClientConnect(this);
				}
			} else {
				server.removeSession(this,
						"Login failed, " + NewIncomingConnection.class.getSimpleName() + " packet failed to decode");
			}
		} else if (packetId == ID_DISCONNECTION_NOTIFICATION) {
			server.removeSession(this, "Disconnected");
		} else {
			/*
			 * If the packet is a user packet, we use handleMessage(). If the ID
			 * is not a user packet but it is unknown to the session, we use
			 * handleUnknownMessage().
			 */
			if (packetId >= ID_USER_PACKET_ENUM) {
				for (RakNetServerListener listener : server.getListeners()) {
					listener.handleMessage(this, packet, channel);
				}
			} else {
				for (RakNetServerListener listener : server.getListeners()) {
					listener.handleUnknownMessage(this, packet, channel);
				}
			}
		}
	}

}
