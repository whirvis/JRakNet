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
package com.whirvis.jraknet.client;

import static com.whirvis.jraknet.RakNetPacket.*;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.peer.RakNetServerSession;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.connection.ConnectionBanned;
import com.whirvis.jraknet.protocol.connection.IncompatibleProtocolVersion;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseTwo;

import io.netty.channel.Channel;

/**
 * Used by the {@link RakNetClient} to better handle server connection and
 * login.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public class SessionPlanner {

	private final Logger log;
	private final RakNetClient client;
	private final int initialMaximumTransferUnit;
	private final int maximumMaximumTransferUnit;
	public RakNetException cancelReason;
	public long guid = -1;
	public int maximumTransferUnit = -1;
	public ConnectionType connectionType = null;
	public InetSocketAddress address = null;
	public boolean loginPackets[] = new boolean[2];

	/**
	 * Creates a session planner.
	 * 
	 * @param client
	 *            the client that is logging into the server.
	 * @param initialMaximumTransferUnit
	 *            the initial maximum transfer unit size.
	 * @param maximumMaximumTransferUnit
	 *            the maximum transfer unit with the highest size.
	 */
	public SessionPlanner(RakNetClient client, int initialMaximumTransferUnit, int maximumMaximumTransferUnit) {
		this.log = LogManager
				.getLogger("jraknet-client-session-planner-" + Long.toHexString(client.getGloballyUniqueId()));
		this.client = client;
		this.initialMaximumTransferUnit = initialMaximumTransferUnit;
		this.maximumMaximumTransferUnit = maximumMaximumTransferUnit;
	}

	/**
	 * Handles the packet and automatically updates the planning data.
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

				// Validate maximum transfer unit
				if (this.maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
					this.cancelReason = new LoginFailureException(client, "Invalid maximum transfer unit size");
				} else {
					this.guid = connectionResponseOne.serverGuid;
					this.loginPackets[0] = true;
					log.debug("Applied maximum transfer unit " + maximumTransferUnit + " and globally unique ID " + guid
							+ " from " + getName(packetId) + " packet");
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
					log.warn("Server responded with lower maximum transfer unit than agreed upon earlier");
				} else if (connectionResponseTwo.maximumTransferUnit > this.maximumMaximumTransferUnit) {
					this.maximumTransferUnit = connectionResponseTwo.maximumTransferUnit;
					log.warn("Server responded with higher maximum transfer unit than agreed upon earlier");
				}
				this.connectionType = connectionResponseTwo.connectionType;
				log.debug("Applied maximum transfer unit from " + getName(packetId) + " packet");
			}
		} else if (packetId == ID_ALREADY_CONNECTED) {
			this.cancelReason = new AlreadyConnectedException(client, address);
		} else if (packetId == ID_NO_FREE_INCOMING_CONNECTIONS) {
			this.cancelReason = new NoFreeIncomingConnectionsException(client, address);
		} else if (packetId == ID_CONNECTION_BANNED) {
			ConnectionBanned connectionBanned = new ConnectionBanned(packet);
			connectionBanned.decode();
			if (connectionBanned.magic != true) {
				this.cancelReason = new LoginFailureException(client, "MAGIC failed to validate");
			} else if (connectionBanned.serverGuid == this.guid) {
				this.cancelReason = new ConnectionBannedException(client, address);
			}
		} else if (packetId == ID_INCOMPATIBLE_PROTOCOL_VERSION) {
			IncompatibleProtocolVersion incompatibleProtocol = new IncompatibleProtocolVersion(packet);
			incompatibleProtocol.decode();
			if (incompatibleProtocol.serverGuid == this.guid) {
				this.cancelReason = new IncompatibleProtocolException(client, address, RakNet.CLIENT_NETWORK_PROTOCOL,
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
			return false; // Session planning cancelled
		} else if (guid == -1 || this.maximumTransferUnit == -1 || this.address == null) {
			return false; // Not enough data set
		}
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
	 * @throws IllegalStateException
	 *             if a session cannot yet be created with the current
	 *             information that the planner has.
	 */
	public RakNetServerSession createSession(Channel channel) {
		if (!this.readyForSession()) {
			throw new IllegalStateException("Session cannot yet be created");
		}
		log.debug("Created server session using globally unique ID " + Long.toHexString(guid).toUpperCase()
				+ " and maximum transfer unit with size of " + maximumTransferUnit + " bytes ("
				+ (maximumTransferUnit * 8) + " bits) for server address " + address);
		return new RakNetServerSession(client, connectionType, guid, maximumTransferUnit, channel, address);
	}

	@Override
	public String toString() {
		return "SessionPreparation [initialMaximumTransferUnit=" + initialMaximumTransferUnit
				+ ", maximumMaximumTransferUnit=" + maximumMaximumTransferUnit + ", cancelReason=" + cancelReason
				+ ", guid=" + guid + ", maximumTransferUnit=" + maximumTransferUnit + ", connectionType="
				+ connectionType + ", address=" + address + "]";
	}

}
