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
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.login.ConnectionRequestAccepted;
import net.marfgamer.raknet.protocol.login.NewIncomingConnection;
import net.marfgamer.raknet.protocol.message.acknowledge.Record;

/**
 * This class represents a server connection and handles the login sequence
 * packets
 *
 * @author MarfGamer
 */
public class RakNetServerSession extends RakNetSession {

	private final RakNetClient client;

	public RakNetServerSession(RakNetClient client, long guid, int maximumTransferUnit, Channel channel,
			InetSocketAddress address) {
		super(guid, maximumTransferUnit, channel, address);
		this.client = client;
		this.setState(RakNetState.HANDSHAKING); // We start at the handshake
	}

	@Override
	public void onAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet) {
		client.getListener().onAcknowledge(this, record, reliability, channel, packet);
	}

	@Override
	public void onNotAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet) {
		client.getListener().onNotAcknowledge(this, record, reliability, channel, packet);
	}

	@Override
	public void handlePacket(RakNetPacket packet, int channel) {
		short packetId = packet.getId();

		if (packetId == ID_CONNECTION_REQUEST_ACCEPTED) {
			ConnectionRequestAccepted serverHandshake = new ConnectionRequestAccepted(packet);
			serverHandshake.decode();

			if (!serverHandshake.failed()) {
				NewIncomingConnection clientHandshake = new NewIncomingConnection();
				clientHandshake.serverAddress = client.getSession().getAddress();
				clientHandshake.clientTimestamp = serverHandshake.clientTimestamp;
				clientHandshake.serverTimestamp = serverHandshake.serverTimestamp;
				clientHandshake.encode();

				if (!clientHandshake.failed()) {
					this.sendMessage(Reliability.RELIABLE, clientHandshake);
					this.setState(RakNetState.CONNECTED);
					client.getListener().onConnect(this);
				} else {
					client.disconnect("Failed to connect, invalid handshake");
				}
			} else {
				client.disconnect("Failed to connect, invalid connection request acception");
			}
		} else if (packetId == ID_DISCONNECTION_NOTIFICATION) {
			this.setState(RakNetState.DISCONNECTED);
			client.disconnect("Server disconnected");
		} else if (packetId >= ID_USER_PACKET_ENUM) {
			client.getListener().handlePacket(this, packet, channel);
		}
	}

	/**
	 * Called by the client when the connection is closed
	 */
	public void closeConnection() {
		this.sendMessage(Reliability.UNRELIABLE, ID_DISCONNECTION_NOTIFICATION);
	}

}
