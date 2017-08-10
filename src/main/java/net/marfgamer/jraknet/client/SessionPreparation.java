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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet.client;

import static net.marfgamer.jraknet.protocol.MessageIdentifier.*;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetException;
import net.marfgamer.jraknet.RakNetLogger;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.ConnectionType;
import net.marfgamer.jraknet.protocol.MessageIdentifier;
import net.marfgamer.jraknet.protocol.login.ConnectionBanned;
import net.marfgamer.jraknet.protocol.login.IncompatibleProtocol;
import net.marfgamer.jraknet.protocol.login.OpenConnectionResponseOne;
import net.marfgamer.jraknet.protocol.login.OpenConnectionResponseTwo;
import net.marfgamer.jraknet.session.RakNetServerSession;

/**
 * Used by the <code>RakNetClient</code> to easily store data during login and
 * create the session when the client is connected.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class SessionPreparation {

	// Preparation data
	private final String loggerName;
	private final RakNetClient client;
	private final int initialMaximumTransferUnit;
	public RakNetException cancelReason;

	// Server data
	public long guid = -1;
	public int maximumTransferUnit = -1;
	public ConnectionType connectionType = null;
	public InetSocketAddress address = null;
	public boolean loginPackets[] = new boolean[2];

	/**
	 * Constructs a <code>SessionPreperation</code> with the specified
	 * <code>RakNetClient</code> and initial maximum transfer unit.
	 * 
	 * @param client
	 *            the <code>RakNetClient</code> that is logging into the server.
	 * @param initialMaximumTransferUnit
	 *            the initial maximum transfer unit.
	 */
	public SessionPreparation(RakNetClient client, int initialMaximumTransferUnit) {
		this.loggerName = "session planner #" + client.getGloballyUniqueId();
		this.client = client;
		this.initialMaximumTransferUnit = initialMaximumTransferUnit;
	}

	/**
	 * Handles the specified packet and automatically updates the preparation
	 * data.
	 * 
	 * @param packet
	 *            the packet to handle.
	 */
	public void handleMessage(RakNetPacket packet) {
		short packetId = packet.getId();
		if (packetId == ID_OPEN_CONNECTION_REPLY_1) {
			OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne(packet);
			connectionResponseOne.decode();

			if (connectionResponseOne.magic != true) {
				this.cancelReason = new LoginFailureException(client, "MAGIC failed to validate");
			} else if (connectionResponseOne.maximumTransferUnit < RakNet.MINIMUM_TRANSFER_UNIT) {
				this.cancelReason = new LoginFailureException(client,
						"Maximum transfer unit is is lower than the minimum");
			} else if (connectionResponseOne.maximumTransferUnit > this.initialMaximumTransferUnit) {
				this.cancelReason = new LoginFailureException(client,
						"Server maximum transfer unit is higher than the client can handle");
			} else {
				this.maximumTransferUnit = connectionResponseOne.maximumTransferUnit;
				this.guid = connectionResponseOne.serverGuid;
				this.loginPackets[0] = true;
				RakNetLogger.debug(loggerName, "Applied maximum transfer unit and globally unique ID from "
						+ MessageIdentifier.getName(packetId) + " packet");
			}
		} else if (packetId == ID_OPEN_CONNECTION_REPLY_2) {
			OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo(packet);
			connectionResponseTwo.decode();

			if (connectionResponseTwo.failed()) {
				this.cancelReason = new LoginFailureException(client,
						connectionResponseTwo.getClass().getSimpleName() + " packet failed to decode");
			} else if (connectionResponseTwo.magic != true) {
				this.cancelReason = new LoginFailureException(client, "MAGIC failed to validate");
			} else if (connectionResponseTwo.serverGuid != this.guid) {
				this.cancelReason = new LoginFailureException(client, "Server responded with invalid GUID");
			} else if (connectionResponseTwo.maximumTransferUnit > this.maximumTransferUnit) {
				this.cancelReason = new LoginFailureException(client,
						"Server maximum transfer unit is higher than the client can handle");
			} else {
				this.loginPackets[1] = true;
				this.maximumTransferUnit = connectionResponseTwo.maximumTransferUnit;
				this.connectionType = connectionResponseTwo.connectionType;
				RakNetLogger.debug(loggerName,
						"Applied maximum transfer unit from " + MessageIdentifier.getName(packetId) + " packet");
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
	 * @return <code>true</code> if the session has enough data to be created
	 */
	public boolean readyForSession() {
		// It was cancelled, why are we finishing?
		if (cancelReason != null) {
			return false;
		}

		// Not all of the data has been set
		if (this.guid == -1 || this.maximumTransferUnit == -1 || this.address == null) {
			return false;
		}

		// Not all of the packets needed to connect have been handled
		for (boolean handled : loginPackets) {
			if (handled == false) {
				return false;
			}
		}

		// Nothing returned false, everything is ready
		return true;
	}

	/**
	 * Creates the session with the data set during login.
	 * 
	 * @param channel
	 *            the channel the session will send data through.
	 * @return the newly created session.
	 */
	public RakNetServerSession createSession(Channel channel) {
		if (!this.readyForSession()) {
			return null;
		}
		RakNetLogger.info(loggerName,
				"Created server session using globally unique ID " + guid + " and maximum transfer unit with size of "
						+ maximumTransferUnit + " bytes (" + (maximumTransferUnit * 8) + " bits) for server address "
						+ address);
		return new RakNetServerSession(this.client, this.connectionType, this.guid, this.maximumTransferUnit, channel,
				this.address);
	}

}
