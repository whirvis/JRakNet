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
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.client;

import static com.whirvis.jraknet.protocol.MessageIdentifier.*;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.MessageIdentifier;
import com.whirvis.jraknet.protocol.login.ConnectionBanned;
import com.whirvis.jraknet.protocol.login.IncompatibleProtocol;
import com.whirvis.jraknet.protocol.login.OpenConnectionResponseOne;
import com.whirvis.jraknet.protocol.login.OpenConnectionResponseTwo;
import com.whirvis.jraknet.session.RakNetServerSession;

import io.netty.channel.Channel;

/**
 * Used by the <code>RakNetClient</code> to easily store data during login and
 * create the session when the client is connected.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class SessionPreparation {

	private static final Logger LOG = LogManager.getLogger(SessionPreparation.class);

	// Preparation data
	private final String loggerName;
	private final RakNetClient client;
	private final int initialMaximumTransferUnit;
	private final int maximumMaximumTransferUnit;
	public RakNetException cancelReason;

	// Server data
	public long guid = -1;
	public int maximumTransferUnit = -1;
	public ConnectionType connectionType = null;
	public InetSocketAddress address = null;
	public boolean loginPackets[] = new boolean[2];

	/**
	 * Constructs a <code>SessionPreperation</code> with the
	 * <code>RakNetClient</code> and initial maximum transfer unit.
	 * 
	 * @param client
	 *            the <code>RakNetClient</code> that is logging into the server.
	 * @param initialMaximumTransferUnit
	 *            the initial maximum transfer unit.
	 * @param maximumMaximumTransferUnit
	 *            the maximum transfer unit with the highest size.
	 */
	public SessionPreparation(RakNetClient client, int initialMaximumTransferUnit, int maximumMaximumTransferUnit) {
		this.loggerName = "session planner #" + Long.toHexString(client.getGloballyUniqueId()).toUpperCase();
		this.client = client;
		this.initialMaximumTransferUnit = initialMaximumTransferUnit;
		this.maximumMaximumTransferUnit = maximumMaximumTransferUnit;
	}

	/**
	 * Handles the packet and automatically updates the preparation
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
			} else if (connectionResponseOne.maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
				this.cancelReason = new LoginFailureException(client,
						"Invalid maximum transfer unit size" + connectionResponseOne.maximumTransferUnit);
			} else {
				// Determine which maximum transfer unit to use
				if (connectionResponseOne.maximumTransferUnit <= this.maximumMaximumTransferUnit) {
					this.maximumTransferUnit = connectionResponseOne.maximumTransferUnit;
				} else {
					this.maximumTransferUnit = connectionResponseOne.maximumTransferUnit < this.initialMaximumTransferUnit
							? connectionResponseOne.maximumTransferUnit : this.initialMaximumTransferUnit;
				}

				// Validate maximum transfer unit and proceed
				if (this.maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
					this.cancelReason = new LoginFailureException(client, "Invalid maximum transfer unit size");
				} else {
					this.guid = connectionResponseOne.serverGuid;
					this.loginPackets[0] = true;
					LOG.debug(loggerName + " applied maximum transfer unit " + maximumTransferUnit
							+ " and globally unique ID " + guid + " from " + MessageIdentifier.getName(packetId)
							+ " packet");
				}
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
			} else if (connectionResponseTwo.maximumTransferUnit > this.maximumMaximumTransferUnit
					|| connectionResponseTwo.maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
				this.cancelReason = new LoginFailureException(client, "Invalid maximum transfer unit size");
			} else {
				this.loginPackets[1] = true;
				if (connectionResponseTwo.maximumTransferUnit < this.maximumTransferUnit) {
					this.maximumTransferUnit = connectionResponseTwo.maximumTransferUnit;
					LOG.warn("Server responded with lower maximum transfer unit than agreed upon earlier");
				} else if (connectionResponseTwo.maximumTransferUnit > this.maximumMaximumTransferUnit) {
					this.maximumTransferUnit = connectionResponseTwo.maximumTransferUnit;
					LOG.warn("Server responded with higher maximum transfer unit than agreed upon earlier");
				}
				this.connectionType = connectionResponseTwo.connectionType;
				LOG.debug(loggerName + " applied maximum transfer unit from " + MessageIdentifier.getName(packetId)
						+ " packet");
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
	 * Returns whether or not the session has enough data to be created.
	 * 
	 * @return <code>true</code> if the session has enough data to be created,
	 *         <code>false</code> otherwise.
	 */
	public boolean readyForSession() {
		if (cancelReason != null) {
			return false; // Session prepration cancelled
		} else if (this.guid == -1 || this.maximumTransferUnit == -1 || this.address == null) {
			return false; // Not enough data set
		}

		// Makes sure all needed packets have been handled
		for (boolean handled : loginPackets) {
			if (handled == false) {
				return false; // Not all of the needed packets have been handled
			}
		}
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
			return null; // Session not ready
		}
		LOG.info(loggerName + " created server session using globally unique ID " + Long.toHexString(guid).toUpperCase()
				+ " and maximum transfer unit with size of " + maximumTransferUnit + " bytes ("
				+ (maximumTransferUnit * 8) + " bits) for server address " + address);
		return new RakNetServerSession(this.client, this.connectionType, this.guid, this.maximumTransferUnit, channel,
				this.address);
	}

	@Override
	public String toString() {
		return "SessionPreparation [initialMaximumTransferUnit=" + initialMaximumTransferUnit
				+ ", maximumMaximumTransferUnit=" + maximumMaximumTransferUnit + ", cancelReason=" + cancelReason
				+ ", guid=" + guid + ", maximumTransferUnit=" + maximumTransferUnit + ", connectionType="
				+ connectionType + ", address=" + address + "]";
	}

}
