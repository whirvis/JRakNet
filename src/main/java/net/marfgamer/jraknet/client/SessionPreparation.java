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
 * Copyright (c) 2016, 2017 MarfGamer
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
package net.marfgamer.jraknet.client;

import static net.marfgamer.jraknet.protocol.MessageIdentifier.*;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetException;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.login.ConnectionBanned;
import net.marfgamer.jraknet.protocol.login.IncompatibleProtocol;
import net.marfgamer.jraknet.protocol.login.OpenConnectionResponseOne;
import net.marfgamer.jraknet.protocol.login.OpenConnectionResponseTwo;
import net.marfgamer.jraknet.session.RakNetServerSession;

/**
 * This class is used to easily store data during login and create the session
 * when the client is connected
 *
 * @author MarfGamer
 */
public class SessionPreparation {

	// Preparation data
	private final RakNetClient client;
	private final int initialMaximumTransferUnit;
	public RakNetException cancelReason;

	// Server data
	public long guid = -1;
	public int maximumTransferUnit = -1;
	public InetSocketAddress address = null;
	public boolean loginPackets[] = new boolean[2];

	public SessionPreparation(RakNetClient client, int initialMaximumTransferUnit) {
		this.client = client;
		this.initialMaximumTransferUnit = initialMaximumTransferUnit;
	}

	/**
	 * Handles the specified packet and automatically updates the preparation
	 * data
	 * 
	 * @param packet
	 *            The packet to handle
	 */
	public void handlePacket(RakNetPacket packet) {
		short packetId = packet.getId();
		if (packetId == ID_OPEN_CONNECTION_REPLY_1) {
			OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne(packet);
			connectionResponseOne.decode();

			if (connectionResponseOne.magic == true
					&& connectionResponseOne.maximumTransferUnit >= RakNet.MINIMUM_TRANSFER_UNIT
					&& connectionResponseOne.maximumTransferUnit <= this.initialMaximumTransferUnit) {
				this.maximumTransferUnit = connectionResponseOne.maximumTransferUnit;
				this.guid = connectionResponseOne.serverGuid;
				this.loginPackets[0] = true;
				if (connectionResponseOne.useSecurity == true) {
					client.getListener().onWarning(Warning.SECURITY_ENABLED);
				}
			} else {
				this.cancelReason = new InvalidProtocolException(client);
			}
		} else if (packetId == ID_OPEN_CONNECTION_REPLY_2) {
			OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo(packet);
			connectionResponseTwo.decode();

			if (!connectionResponseTwo.failed() && connectionResponseTwo.magic == true
					&& connectionResponseTwo.serverGuid == this.guid
					&& connectionResponseTwo.maximumTransferUnit <= this.maximumTransferUnit) {
				this.loginPackets[1] = true;
				this.maximumTransferUnit = connectionResponseTwo.maximumTransferUnit;
				if (connectionResponseTwo.encryptionEnabled == true) {
					client.getListener().onWarning(Warning.ENCRYPTION_ENABLED);
				}
			} else {
				this.cancelReason = new InvalidProtocolException(client);
			}
		} else if (packetId == ID_ALREADY_CONNECTED) {
			this.cancelReason = new AlreadyConnectedException(client);
		} else if (packetId == ID_NO_FREE_INCOMING_CONNECTIONS) {
			this.cancelReason = new NoFreeIncomingConnectionsException(client);
		} else if (packetId == ID_CONNECTION_BANNED) {
			ConnectionBanned connectionBanned = new ConnectionBanned(packet);
			connectionBanned.decode();
			if (connectionBanned.serverGuid == this.guid) {
				this.cancelReason = new ConnectionBannedException(client);
			}
		} else if (packetId == ID_INCOMPATIBLE_PROTOCOL_VERSION) {
			IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol(packet);
			incompatibleProtocol.decode();

			if (incompatibleProtocol.serverGuid == this.guid) {
				this.cancelReason = new IncompatibleProtocolException(client, RakNet.CLIENT_NETWORK_PROTOCOL,
						incompatibleProtocol.networkProtocol);
			}
		}
	}

	/**
	 * Returns whether or not the session has enough data to be created to be
	 * used by the client
	 * 
	 * @return Whether or not the session has enough data to be created
	 */
	public boolean readyForSession() {
		// It was cancelled, why are we finishing?
		if (cancelReason != null) {
			return false;
		}

		// Not all of the data has been set
		if (this.guid == -1 || this.maximumTransferUnit == -1 || this.address == null) {
			System.out.println("NOt enough data");
			return false;
		}

		// Not all of the packets needed to connect have been handled
		for (boolean handled : loginPackets) {
			if (handled == false) {
				System.out.println("Unhandled packets");
				return false;
			}
		}

		// Nothing returned false, everything is ready
		return true;
	}

	/**
	 * Creates the session with the data set during login
	 * 
	 * @param channel
	 *            The channel the session will send data through
	 * @return RakNetServerSession
	 */
	public RakNetServerSession createSession(Channel channel) {
		return new RakNetServerSession(client, guid, maximumTransferUnit, channel, address);
	}

}
