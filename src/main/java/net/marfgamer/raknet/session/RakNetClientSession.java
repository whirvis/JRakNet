/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 MarfGamer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
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
package net.marfgamer.raknet.session;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.login.ConnectionRequest;
import net.marfgamer.raknet.protocol.login.ConnectionRequestAccepted;
import net.marfgamer.raknet.protocol.login.NewIncomingConnection;
import net.marfgamer.raknet.protocol.message.acknowledge.Record;
import net.marfgamer.raknet.server.RakNetServer;

/**
 * This class represents a client connection and handles the login sequence
 * packets
 *
 * @author MarfGamer
 */
public class RakNetClientSession extends RakNetSession {

	private final RakNetServer server;
	private final long timeCreated;
	private long timestamp;

	public RakNetClientSession(RakNetServer server, long timeCreated, long guid, int maximumTransferUnit,
			Channel channel, InetSocketAddress address) {
		super(guid, maximumTransferUnit, channel, address);
		this.server = server;
		this.timeCreated = timeCreated;
		// The timestamp is determined during login
	}

	/**
	 * Returns the server this session is connected to
	 * 
	 * @return The server this session is connected to
	 */
	public RakNetServer getServer() {
		return this.server;
	}

	/**
	 * Returns the time this session was created
	 * 
	 * @return The time this session was created
	 */
	public long getTimeCreated() {
		return this.timeCreated;
	}

	/**
	 * Returns the client's timestamp<br>
	 * Note: This is not determined during creation but rather during login
	 * 
	 * @return The client's timestamp
	 */
	public long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	@Override
	public void onAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet) {
		server.getListener().onAcknowledge(this, record, reliability, channel, packet);
	}

	@Override
	public void onNotAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet) {
		server.getListener().onNotAcknowledge(this, record, reliability, channel, packet);
	}

	@Override
	public void handlePacket(RakNetPacket packet, int channel) {
		short packetId = packet.getId();

		if (packetId == ID_CONNECTION_REQUEST && this.getState() == RakNetState.DISCONNECTED) {
			ConnectionRequest request = new ConnectionRequest(packet);
			request.decode();

			if (request.clientGuid == this.getGloballyUniqueId()) {
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
					server.removeSession(this, "Login failed");
				}
			} else {
				this.sendMessage(Reliability.RELIABLE_ORDERED, ID_CONNECTION_ATTEMPT_FAILED);
				this.setState(RakNetState.DISCONNECTED);
				server.removeSession(this, "Login failed");
			}
		} else if (packetId == ID_NEW_INCOMING_CONNECTION && this.getState() == RakNetState.HANDSHAKING) {
			NewIncomingConnection clientHandshake = new NewIncomingConnection(packet);
			clientHandshake.decode();

			if (!clientHandshake.failed()) {
				this.timestamp = (System.currentTimeMillis() - clientHandshake.clientTimestamp);
				this.setState(RakNetState.CONNECTED);
				server.getListener().onClientConnect(this);
			} else {
				server.removeSession(this, "Login failed");
			}
		} else if (packetId == ID_DISCONNECTION_NOTIFICATION) {
			server.removeSession(this, "Disconnected");
		} else if (packetId >= ID_USER_PACKET_ENUM) {
			server.getListener().handlePacket(this, packet, channel);
		}
	}

}
