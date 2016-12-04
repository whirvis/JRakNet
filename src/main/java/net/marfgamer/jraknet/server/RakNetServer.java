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
package net.marfgamer.jraknet.server;

import static net.marfgamer.jraknet.protocol.MessageIdentifier.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.exception.NoListenerException;
import net.marfgamer.jraknet.identifier.Identifier;
import net.marfgamer.jraknet.protocol.Reliability;
import net.marfgamer.jraknet.protocol.login.ConnectionBanned;
import net.marfgamer.jraknet.protocol.login.IncompatibleProtocol;
import net.marfgamer.jraknet.protocol.login.OpenConnectionRequestOne;
import net.marfgamer.jraknet.protocol.login.OpenConnectionRequestTwo;
import net.marfgamer.jraknet.protocol.login.OpenConnectionResponseOne;
import net.marfgamer.jraknet.protocol.login.OpenConnectionResponseTwo;
import net.marfgamer.jraknet.protocol.message.CustomPacket;
import net.marfgamer.jraknet.protocol.message.acknowledge.Acknowledge;
import net.marfgamer.jraknet.protocol.message.acknowledge.AcknowledgeReceipt;
import net.marfgamer.jraknet.protocol.status.UnconnectedPing;
import net.marfgamer.jraknet.protocol.status.UnconnectedPong;
import net.marfgamer.jraknet.session.GeminusRakNetPeer;
import net.marfgamer.jraknet.session.RakNetClientSession;
import net.marfgamer.jraknet.session.RakNetSession;
import net.marfgamer.jraknet.session.RakNetState;
import net.marfgamer.jraknet.util.RakNetUtils;

/**
 * This class is used to easily create servers using the RakNet protocol
 *
 * @author MarfGamer
 */
public class RakNetServer implements GeminusRakNetPeer, RakNetServerListener {

	// Server data
	private final long guid;
	private final long timestamp;
	private final int port;
	private final int maxConnections;
	private final int maximumTransferUnit;
	private boolean broadcastingEnabled;
	private Identifier identifier;

	// Networking data
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetServerHandler handler;

	// Session data
	private Channel channel;
	private volatile RakNetServerListener listener;
	private volatile boolean running; // Allow other threads to modify this
	private final ConcurrentHashMap<InetSocketAddress, RakNetClientSession> sessions;

	public RakNetServer(int port, int maxConnections, int maximumTransferUnit, Identifier identifier) {
		// Set server data
		this.guid = new Random().nextLong();
		this.timestamp = System.currentTimeMillis();
		this.port = port;
		this.maxConnections = maxConnections;
		this.maximumTransferUnit = maximumTransferUnit;
		this.broadcastingEnabled = true;
		this.identifier = identifier;

		// Initiate bootstrap data
		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetServerHandler(this);

		// Set listener
		this.listener = this;

		// Create session map
		this.sessions = new ConcurrentHashMap<InetSocketAddress, RakNetClientSession>();

		// Check maximum transfer unit
		if (this.maximumTransferUnit < RakNet.MINIMUM_TRANSFER_UNIT) {
			throw new IllegalArgumentException(
					"Maximum transfer unit can be no smaller than " + RakNet.MINIMUM_TRANSFER_UNIT + "!");
		}
	}

	public RakNetServer(int port, int maxConnections, int maximumTransferUnit) {
		this(port, maxConnections, maximumTransferUnit, null);
	}

	public RakNetServer(int port, int maxConnections) {
		this(port, maxConnections, RakNetUtils.getMaximumTransferUnit());
	}

	public RakNetServer(int port, int maxConnections, Identifier identifier) {
		this(port, maxConnections);
		this.identifier = identifier;
	}

	/**
	 * Returns the server's networking protocol version
	 * 
	 * @return The server's networking protocol version
	 */
	public int getProtocolVersion() {
		return RakNet.SERVER_NETWORK_PROTOCOL;
	}

	/**
	 * Returns the server's globally unique ID (GUID)
	 * 
	 * @return The server's globally unique ID
	 */
	public long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * Returns the server's timestamp (how long ago it started in milliseconds)
	 * 
	 * @return The server's timestamp
	 */
	public long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	/**
	 * Returns the port the server is bound to
	 * 
	 * @return The port the server is bound to
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Returns the maximum amount of connections the server can handle at once
	 * 
	 * @return The maximum amount of connections the server can handle at once
	 */
	public int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * Returns the maximum transfer unit
	 * 
	 * @return The maximum transfer unit
	 */
	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Enables/disables server broadcasting
	 * 
	 * @param enabled
	 *            Whether or not the server will broadcast
	 */
	public void setBroadcastingEnabled(boolean enabled) {
		this.broadcastingEnabled = enabled;
	}

	/**
	 * Returns whether or not broadcasting is enabled
	 * 
	 * @return Whether or not broadcasting is enabled
	 */
	public boolean getBroadcastingEnabled() {
		return this.broadcastingEnabled;
	}

	/**
	 * Returns the identifier the server uses for discovery
	 * 
	 * @return The identifier the server uses for discovery
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Sets the server's identifier used for discovery
	 * 
	 * @param identifier
	 *            The new identifier
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	/**
	 * Returns the server's listener
	 * 
	 * @return The server's listener
	 */
	public RakNetServerListener getListener() {
		return this.listener;
	}

	/**
	 * Sets the server's listener
	 * 
	 * @param listener
	 *            The new listener
	 * @return The server
	 */
	public RakNetServer setListener(RakNetServerListener listener) {
		if (listener == null) {
			throw new NullPointerException("Listener must not be null!");
		}
		this.listener = listener;
		return this;
	}

	/**
	 * Returns the sessions connected to the server
	 * 
	 * @return The sessions connected to the server
	 */
	public RakNetClientSession[] getSessions() {
		return sessions.values().toArray(new RakNetClientSession[sessions.size()]);
	}

	/**
	 * Returns the amount of sessions connected to the server
	 * 
	 * @return The amount of sessions connected to the server
	 */
	public int getSessionCount() {
		return sessions.size();
	}

	/**
	 * Returns whether or not the server has a session with the specified
	 * address
	 * 
	 * @param address
	 *            The address to check
	 * @return Whether or not the server has a session with the specified
	 *         address
	 */
	public boolean hasSession(InetSocketAddress address) {
		return sessions.containsKey(address);
	}

	/**
	 * Returns whether or not the server has a session with the specified
	 * globally unique ID
	 * 
	 * @param guid
	 *            The globally unique ID to check
	 * @return Whether or not the server has a session with the specified
	 *         Globally Unique ID
	 */
	public boolean hasSession(long guid) {
		for (RakNetClientSession session : sessions.values()) {
			if (session.getGloballyUniqueId() == guid) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a session connected to the server by their address
	 * 
	 * @param address
	 *            The address of the session
	 * @return A session connected to the server by their address
	 */
	public RakNetClientSession getSession(InetSocketAddress address) {
		return sessions.get(address);
	}

	/**
	 * Returns a session connected to the server by their Globally Unique ID
	 * 
	 * @param guid
	 *            The Globally Unique ID of the session
	 * @return A session connected to the server by their address
	 */
	public RakNetClientSession getSession(long guid) {
		for (RakNetClientSession session : sessions.values()) {
			if (session.getGloballyUniqueId() == guid) {
				return session;
			}
		}
		return null;
	}

	@Override
	public void sendMessage(long guid, Reliability reliability, int channel, Packet packet) {
		if (this.hasSession(guid)) {
			this.getSession(guid).sendMessage(reliability, channel, packet);
		}
	}

	/**
	 * Removes a session from the server with the specified reason
	 * 
	 * @param address
	 *            The address of the session
	 * @param reason
	 *            The reason the session was removed
	 */
	public void removeSession(InetSocketAddress address, String reason) {
		if (sessions.containsKey(address)) {
			RakNetClientSession session = sessions.get(address);
			if (session.getState() == RakNetState.CONNECTED) {
				listener.onClientDisconnect(session, reason);
			}
			session.sendMessage(Reliability.UNRELIABLE, ID_DISCONNECTION_NOTIFICATION);
			sessions.remove(address);
		}
	}

	/**
	 * Removes a session from the server
	 * 
	 * @param address
	 *            The address of the session
	 */
	public void removeSession(InetSocketAddress address) {
		this.removeSession(address, "Disconnected from server");
	}

	/**
	 * Removes a session from the server with the specified reason
	 * 
	 * @param session
	 *            The session to remove
	 * @param reason
	 *            The reason the session was removed
	 */
	public void removeSession(RakNetClientSession session, String reason) {
		this.removeSession(session.getAddress(), reason);
	}

	/**
	 * Removes a session from the server
	 * 
	 * @param session
	 *            The session to remove
	 */
	public void removeSession(RakNetClientSession session) {
		this.removeSession(session, "Disconnected from server");
	}

	/**
	 * Blocks the address and disconnects all the clients on the address with
	 * the specified reason for the specified amount of time
	 * 
	 * @param address
	 *            The address to block
	 * @param reason
	 *            The reason the address was blocked
	 * @param time
	 *            How long the address will blocked in milliseconds
	 */
	public void blockAddress(InetAddress address, String reason, long time) {
		for (InetSocketAddress clientAddress : sessions.keySet()) {
			if (clientAddress.getAddress().getAddress().equals(address)) {
				this.removeSession(clientAddress, reason);
			}
		}
		handler.blockAddress(address, time);
	}

	/**
	 * Blocks the address and disconnects all the clients on the address for the
	 * specified amount of time
	 * 
	 * @param address
	 *            The address to block
	 * @param time
	 *            How long the address will blocked in milliseconds
	 */
	public void blockAddress(InetAddress address, long time) {
		this.blockAddress(address, "Blocked", time);
	}

	/**
	 * Unblocks the specified address
	 * 
	 * @param address
	 *            The address to unblock
	 */
	public void unblockAddress(InetAddress address) {
		handler.unblockAddress(address);
	}

	/**
	 * Blocks the address and disconnects all the clients on the address with
	 * the specified reason for the specified amount of time
	 * 
	 * @param address
	 *            The address to block
	 * @param reason
	 *            The reason the address was blocked
	 * @param time
	 *            How long the address will blocked in milliseconds
	 */
	public void blockAddress(InetSocketAddress address, String reason, long time) {
		this.blockAddress(address.getAddress(), reason, time);
	}

	/**
	 * Blocks the address and disconnects all the clients on the address for the
	 * specified amount of time
	 * 
	 * @param address
	 *            The address to block
	 * @param time
	 *            How long the address will blocked in milliseconds
	 */
	public void blockAddress(InetSocketAddress address, long time) {
		this.blockAddress(address, "Blocked", time);
	}

	/**
	 * Unblocks the specified address
	 * 
	 * @param address
	 *            The address to unblock
	 */
	public void unblockAddress(InetSocketAddress address) {
		this.unblockAddress(address.getAddress());
	}

	/**
	 * Returns whether or not the specified address is blocked
	 * 
	 * @param address
	 *            The address to check
	 * @return Whether or not the specified address is blocked
	 */
	public boolean addressBlocked(InetAddress address) {
		return handler.addressBlocked(address);
	}

	/**
	 * Called whenever the handler catches an exception in Netty
	 * 
	 * @param address
	 *            The address that caused the exception
	 * @param cause
	 *            The exception caught by the handler
	 */
	protected void handleHandlerException(InetSocketAddress address, Throwable cause) {
		if (this.hasSession(address)) {
			this.removeSession(address, cause.getClass().getName());
		}
		listener.onHandlerException(address, cause);
	}

	/**
	 * Handles a packet received by the handler
	 * 
	 * @param packet
	 *            The packet to handle
	 * @param sender
	 *            The address of the sender
	 */
	protected void handlePacket(RakNetPacket packet, InetSocketAddress sender) {
		short packetId = packet.getId();

		if (packetId == ID_UNCONNECTED_PING || packetId == ID_UNCONNECTED_PING_OPEN_CONNECTIONS) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();

			// Make sure parameters match and that broadcasting is enabled
			if ((packetId == ID_UNCONNECTED_PING || sessions.size() < this.maxConnections)
					&& this.broadcastingEnabled == true) {
				ServerPing pingEvent = new ServerPing(sender, identifier);
				listener.handlePing(pingEvent);

				if (ping.magic == true && pingEvent.getIdentifier() != null) {
					UnconnectedPong pong = new UnconnectedPong();
					pong.pingId = ping.timestamp;
					pong.pongId = this.getTimestamp();
					pong.identifier = pingEvent.getIdentifier();

					pong.encode();
					this.sendRawMessage(pong, sender);
				}
			}
		} else if (packetId == ID_OPEN_CONNECTION_REQUEST_1) {
			OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne(packet);
			connectionRequestOne.decode();

			if (connectionRequestOne.magic == true) {
				// Are there any problems?
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (connectionRequestOne.protocolVersion != this.getProtocolVersion()) {
						// Incompatible protocol!
						IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol();
						incompatibleProtocol.networkProtocol = this.getProtocolVersion();
						incompatibleProtocol.serverGuid = this.guid;
						incompatibleProtocol.encode();
						this.sendRawMessage(incompatibleProtocol, sender);
					} else {
						// Everything passed! One last check...
						if (connectionRequestOne.maximumTransferUnit <= this.maximumTransferUnit) {
							OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne();
							connectionResponseOne.serverGuid = this.guid;
							connectionResponseOne.maximumTransferUnit = connectionRequestOne.maximumTransferUnit;
							connectionResponseOne.encode();
							this.sendRawMessage(connectionResponseOne, sender);
						}
					}
				} else {
					this.sendRawMessage(errorPacket, sender);
				}
			}
		} else if (packetId == ID_OPEN_CONNECTION_REQUEST_2) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo(packet);
			connectionRequestTwo.decode();

			if (!connectionRequestTwo.failed() && connectionRequestTwo.magic == true) {
				// Are there any problems?
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (this.hasSession(connectionRequestTwo.clientGuid)) {
						// This client is already connected!
						this.sendRawMessage(ID_ALREADY_CONNECTED, sender);
					} else {
						// Everything passed! One last check...
						if (connectionRequestTwo.maximumTransferUnit <= this.maximumTransferUnit) {
							// Create response
							OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo();
							connectionResponseTwo.serverGuid = this.guid;
							connectionResponseTwo.clientAddress = sender;
							connectionResponseTwo.maximumTransferUnit = connectionRequestTwo.maximumTransferUnit;
							connectionResponseTwo.encryptionEnabled = false;
							connectionResponseTwo.encode();

							if (!connectionResponseTwo.failed()) {
								// Call event
								this.getListener().onClientPreConnect(sender);

								// Create session
								RakNetClientSession clientSession = new RakNetClientSession(this,
										System.currentTimeMillis(), connectionRequestTwo.clientGuid,
										connectionRequestTwo.maximumTransferUnit, channel, sender);
								sessions.put(sender, clientSession);

								// Send response, we are ready for login!
								this.sendRawMessage(connectionResponseTwo, sender);
							}
						}
					}
				} else {
					this.sendRawMessage(errorPacket, sender);
				}
			}
		} else if (packetId >= ID_CUSTOM_0 && packetId <= ID_CUSTOM_F) {
			if (sessions.containsKey(sender)) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleCustom0(custom);
			}
		} else if (packetId == ID_SND_RECEIPT_ACKED || packetId == ID_SND_RECEIPT_LOSS) {
			if (sessions.containsKey(sender)) {
				AcknowledgeReceipt acknowledgeReceipt = new AcknowledgeReceipt(packet);
				acknowledgeReceipt.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleAcknowledgeReceipt(acknowledgeReceipt);
			}
		} else if (packetId == Acknowledge.ACKNOWLEDGED || packetId == Acknowledge.NOT_ACKNOWLEDGED) {
			if (sessions.containsKey(sender)) {
				Acknowledge acknowledge = new Acknowledge(packet);
				acknowledge.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleAcknowledge(acknowledge);
			}
		}
	}

	/**
	 * Validates the sender during login to make sure there are no problems
	 * 
	 * @param sender
	 *            The address of the packet sender
	 * @return The packet to respond with if there was an error
	 */
	private RakNetPacket validateSender(InetSocketAddress sender) {
		// Checked throughout all login
		if (this.hasSession(sender)) {
			return new RakNetPacket(ID_ALREADY_CONNECTED);
		} else if (this.getSessionCount() >= this.maxConnections) {
			// We have no free connections!
			return new RakNetPacket(ID_NO_FREE_INCOMING_CONNECTIONS);
		} else if (this.addressBlocked(sender.getAddress())) {
			// Address is blocked!
			ConnectionBanned connectionBanned = new ConnectionBanned();
			connectionBanned.serverGuid = this.guid;
			connectionBanned.encode();
			return connectionBanned;
		}

		// There were no errors
		return null;
	}

	/**
	 * Sends a raw message to the specified address
	 * 
	 * @param packet
	 *            The packet to send
	 * @param address
	 *            The address to send the packet to
	 */
	private void sendRawMessage(Packet packet, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	/**
	 * Sends a single ID to the specified address
	 * 
	 * @param packetId
	 *            The ID of the packet to send
	 * @param address
	 *            The address to send the packet to
	 */
	private void sendRawMessage(int packetId, InetSocketAddress address) {
		this.sendRawMessage(new RakNetPacket(packetId), address);
	}

	/**
	 * Starts the server
	 * 
	 * @throws NoListenerException
	 *             Thrown if the listener has not yet been set
	 */
	public void start() throws NoListenerException {
		// Make sure we have an adapter
		if (listener == null) {
			throw new NoListenerException("Unable to start server, there is no listener!");
		}

		// Create bootstrap and bind the channel
		try {
			bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = bootstrap.bind(port).sync().channel();
			this.running = true;
		} catch (InterruptedException e) {
			e.printStackTrace();
			this.running = false;
		}

		// Notify API
		listener.onServerStart();

		// Update system
		while (this.running == true) {
			for (RakNetClientSession session : sessions.values()) {
				try {
					// Update session and make sure it isn't DOSing us
					session.update();
					if (session.getPacketsReceivedThisSecond() >= RakNet.MAX_PACKETS_PER_SECOND) {
						this.blockAddress(session.getAddress(), "Too many packets",
								RakNetSession.MAX_PACKETS_PER_SECOND_BLOCK);
					}
				} catch (Exception e) {
					// An error related to the session occurred, remove it!
					this.removeSession(session, e.getMessage());
				}
			}
		}
	}

	/**
	 * Starts the server on it's own Thread
	 * 
	 * @return The Thread the server is running on
	 */
	public synchronized Thread startThreaded() {
		// Give the thread a reference
		RakNetServer server = this;

		// Create thread and start it
		Thread thread = new Thread() {
			@Override
			public synchronized void run() {
				try {
					server.start();
				} catch (Exception e) {
					server.getListener().onThreadException(e);
				}
			}
		};
		thread.start();

		// Return the thread so it can be modified
		return thread;
	}

	/**
	 * Stops the server
	 */
	public void stop() {
		this.running = false;
		for (RakNetClientSession session : sessions.values()) {
			this.removeSession(session, "Server shutdown");
		}
		sessions.clear();
		this.getListener().onServerShutdown();
	}

}
