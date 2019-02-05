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
package com.whirvis.jraknet.server;

import static com.whirvis.jraknet.protocol.MessageIdentifier.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.MessageIdentifier;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.login.ConnectionBanned;
import com.whirvis.jraknet.protocol.login.IncompatibleProtocol;
import com.whirvis.jraknet.protocol.login.OpenConnectionRequestOne;
import com.whirvis.jraknet.protocol.login.OpenConnectionRequestTwo;
import com.whirvis.jraknet.protocol.login.OpenConnectionResponseOne;
import com.whirvis.jraknet.protocol.login.OpenConnectionResponseTwo;
import com.whirvis.jraknet.protocol.message.CustomPacket;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Acknowledge;
import com.whirvis.jraknet.protocol.status.UnconnectedPing;
import com.whirvis.jraknet.protocol.status.UnconnectedPong;
import com.whirvis.jraknet.session.GeminusRakNetPeer;
import com.whirvis.jraknet.session.RakNetClientSession;
import com.whirvis.jraknet.session.RakNetState;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Used to easily create servers using the RakNet protocol.
 *
 * @author Whirvis T. Wheatley
 */
public class RakNetServer implements GeminusRakNetPeer, RakNetServerListener {

	private static final Logger LOG = LogManager.getLogger(RakNetServer.class);

	// Server data
	private final long guid;
	private final long pongId;
	private final long timestamp;
	private final int port;
	private final int maxConnections;
	private final int maximumTransferUnit;
	private boolean broadcastingEnabled;
	private Identifier identifier;
	private final ConcurrentLinkedQueue<RakNetServerListener> listeners;
	private Thread serverThread;

	// Networking data
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetServerHandler handler;

	// Session data
	private Channel channel;
	private volatile boolean running;
	private final ConcurrentHashMap<InetSocketAddress, RakNetClientSession> sessions;

	/**
	 * Constructs a <code>RakNetServer</code> with the port, maximum
	 * amount connections, maximum transfer unit, and <code>Identifier</code>.
	 *
	 * @param port
	 *            the server port.
	 * @param maxConnections
	 *            the maximum amount of connections.
	 * @param maximumTransferUnit
	 *            the maximum transfer unit.
	 * @param identifier
	 *            the <code>Identifier</code>.
	 */
	public RakNetServer(int port, int maxConnections, int maximumTransferUnit, Identifier identifier) {
		// Set server data
		UUID uuid = UUID.randomUUID();
		this.guid = uuid.getMostSignificantBits();
		this.pongId = uuid.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();
		this.port = port;
		this.maxConnections = maxConnections;
		this.maximumTransferUnit = maximumTransferUnit;
		this.broadcastingEnabled = true;
		this.identifier = identifier;
		this.listeners = new ConcurrentLinkedQueue<RakNetServerListener>();

		// Initiate bootstrap data
		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetServerHandler(this);

		// Create session map
		this.sessions = new ConcurrentHashMap<InetSocketAddress, RakNetClientSession>();

		// Check maximum transfer unit
		if (this.maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
			throw new IllegalArgumentException(
					"Maximum transfer unit can be no smaller than " + RakNet.MINIMUM_MTU_SIZE);
		}
	}

	/**
	 * Constructs a <code>RakNetServer</code> with the port, maximum
	 * amount connections, and maximum transfer unit.
	 *
	 * @param port
	 *            the server port.
	 * @param maxConnections
	 *            the maximum amount of connections.
	 * @param maximumTransferUnit
	 *            the maximum transfer unit.
	 */
	public RakNetServer(int port, int maxConnections, int maximumTransferUnit) {
		this(port, maxConnections, maximumTransferUnit, null);
	}

	/**
	 * Constructs a <code>RakNetServer</code> with the port and
	 * maximum amount of connections.
	 *
	 * @param port
	 *            the server port.
	 * @param maxConnections
	 *            the maximum amount of connections.
	 */
	public RakNetServer(int port, int maxConnections) {
		this(port, maxConnections, RakNet.getMaximumTransferUnit());
	}

	/**
	 * Constructs a <code>RakNetServer</code> with the port, maximum
	 * amount connections, and <code>Identifier</code>.
	 *
	 * @param port
	 *            the server port.
	 * @param maxConnections
	 *            the maximum amount of connections.
	 * @param identifier
	 *            the <code>Identifier</code>.
	 */
	public RakNetServer(int port, int maxConnections, Identifier identifier) {
		this(port, maxConnections);
		this.identifier = identifier;
	}

	/**
	 * @return the server's networking protocol version.
	 */
	public final int getProtocolVersion() {
		return RakNet.SERVER_NETWORK_PROTOCOL;
	}

	/**
	 * @return the server's globally unique ID.
	 */
	public final long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * @return the server's timestamp.
	 */
	public final long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	/**
	 * @return the port the server is bound to.
	 */
	public final int getPort() {
		return this.port;
	}

	/**
	 * @return the maximum amount of connections the server can handle at once.
	 */
	public final int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * @return the maximum transfer unit.
	 */
	public final int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Enables/disables server broadcasting.
	 * 
	 * @param enabled
	 *            whether or not the server will broadcast.
	 */
	public final void setBroadcastingEnabled(boolean enabled) {
		this.broadcastingEnabled = enabled;
		LOG.info((enabled ? "Enabled" : "Disabled") + " broadcasting");
	}

	/**
	 * @return <code>true</code> if broadcasting is enabled.
	 */
	public final boolean isBroadcastingEnabled() {
		return this.broadcastingEnabled;
	}

	/**
	 * @return the identifier the server uses for discovery.
	 */
	public final Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Sets the server's identifier used for discovery.
	 * 
	 * @param identifier
	 *            the new identifier.
	 */
	public final void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
		LOG.info("Set identifier to \"" + identifier.build() + "\"");
	}

	/**
	 * @return the thread the server is running on if it was started using
	 *         <code>startThreaded()</code>.
	 */
	public final Thread getThread() {
		return this.serverThread;
	}

	/**
	 * @return the server's listeners.
	 */
	public final RakNetServerListener[] getListeners() {
		return listeners.toArray(new RakNetServerListener[listeners.size()]);
	}

	/**
	 * Adds a listener to the server.
	 * 
	 * @param listener
	 *            the listener to add.
	 * @return the server.
	 */
	public final RakNetServer addListener(RakNetServerListener listener) {
		// Validate listener
		if (listener == null) {
			throw new NullPointerException("Listener must not be null");
		}
		if (listeners.contains(listener)) {
			throw new IllegalArgumentException("A listener cannot be added twice");
		}
		if (listener instanceof RakNetClient && !listener.equals(this)) {
			throw new IllegalArgumentException("A server cannot be used as a listener except for itself");
		}

		// Add listener
		listeners.add(listener);
		LOG.info("Added listener " + listener.getClass().getName());

		return this;
	}

	/**
	 * Adds the server to its own set of listeners, used when extending the
	 * <code>RakNetServer</code> directly.
	 * 
	 * @return the server.
	 */
	public final RakNetServer addSelfListener() {
		this.addListener(this);
		return this;
	}

	/**
	 * Removes a listener from the server.
	 * 
	 * @param listener
	 *            the listener to remove.
	 * @return the server.
	 */
	public final RakNetServer removeListener(RakNetServerListener listener) {
		boolean hadListener = listeners.remove(listener);
		if (hadListener == true) {
			LOG.info("Removed listener " + listener.getClass().getName());
		} else {
			LOG.warn("Attempted to removed unregistered listener " + listener.getClass().getName());
		}
		return this;
	}

	/**
	 * Removes the server from its own set of listeners, used when extending the
	 * <code>RakNetServer</code> directly.
	 * 
	 * @return the server.
	 */
	public final RakNetServer removeSelfListener() {
		this.removeListener(this);
		return this;
	}

	/**
	 * @return the sessions connected to the server.
	 */
	public final RakNetClientSession[] getSessions() {
		return sessions.values().toArray(new RakNetClientSession[sessions.size()]);
	}

	/**
	 * @return the amount of sessions connected to the server.
	 */
	public final int getSessionCount() {
		return sessions.size();
	}

	/**
	 * @param address
	 *            the address to check.
	 * @return true server has a session with the address.
	 */
	public final boolean hasSession(InetSocketAddress address) {
		return sessions.containsKey(address);
	}

	/**
	 * @param guid
	 *            the globally unique ID to check.
	 * @return <code>true</code> if the server has a session with the
	 *         globally unique ID.
	 */
	public final boolean hasSession(long guid) {
		for (RakNetClientSession session : sessions.values()) {
			if (session.getGloballyUniqueId() == guid) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param address
	 *            the address of the session.
	 * @return a session connected to the server by their address.
	 */
	public final RakNetClientSession getSession(InetSocketAddress address) {
		return sessions.get(address);
	}

	/**
	 * @param guid
	 *            the globally unique ID of the session.
	 * @return a session connected to the server by their address.
	 */
	public final RakNetClientSession getSession(long guid) {
		for (RakNetClientSession session : sessions.values()) {
			if (session.getGloballyUniqueId() == guid) {
				return session;
			}
		}
		return null;
	}

	@Override
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, Packet packet) {
		if (this.hasSession(guid)) {
			return this.getSession(guid).sendMessage(reliability, channel, packet);
		} else {
			throw new IllegalArgumentException("No such session with GUID");
		}
	}

	/**
	 * Removes a session from the server with the reason.
	 * 
	 * @param address
	 *            the address of the session.
	 * @param reason
	 *            the reason the session was removed.
	 */
	public final void removeSession(InetSocketAddress address, String reason) {
		if (sessions.containsKey(address)) {
			// Notify client of disconnection
			RakNetClientSession session = sessions.remove(address);
			session.sendMessage(Reliability.UNRELIABLE, ID_DISCONNECTION_NOTIFICATION);

			// Notify API
			LOG.debug("Removed session with address " + address);
			if (session.getState() == RakNetState.CONNECTED) {
				for (RakNetServerListener listener : listeners) {
					listener.onClientDisconnect(session, reason);
				}
			} else {
				for (RakNetServerListener listener : listeners) {
					listener.onClientPreDisconnect(address, reason);
				}
			}
		} else {
			LOG.warn("Attempted to remove session that had not been added to the server");
		}
	}

	/**
	 * Removes a session from the server.
	 * 
	 * @param address
	 *            the address of the session.
	 */
	public final void removeSession(InetSocketAddress address) {
		this.removeSession(address, "Disconnected from server");
	}

	/**
	 * Removes a session from the server with the reason.
	 * 
	 * @param session
	 *            the session to remove.
	 * @param reason
	 *            the reason the session was removed.
	 */
	public final void removeSession(RakNetClientSession session, String reason) {
		this.removeSession(session.getAddress(), reason);
	}

	/**
	 * Removes a session from the server.
	 * 
	 * @param session
	 *            the session to remove.
	 */
	public final void removeSession(RakNetClientSession session) {
		this.removeSession(session, "Disconnected from server");
	}

	/**
	 * Blocks the address and disconnects all the clients on the address with
	 * the reason for the amount of time.
	 * 
	 * @param address
	 *            the address to block.
	 * @param reason
	 *            the reason the address was blocked.
	 * @param time
	 *            how long the address will blocked in milliseconds.
	 */
	public final void blockAddress(InetAddress address, String reason, long time) {
		for (InetSocketAddress clientAddress : sessions.keySet()) {
			if (clientAddress.getAddress().equals(address)) {
				this.removeSession(clientAddress, reason);
			}
		}
		handler.blockAddress(address, reason, time);
	}

	/**
	 * Blocks the address and disconnects all the clients on the address for the
	 * amount of time.
	 * 
	 * @param address
	 *            the address to block.
	 * @param time
	 *            how long the address will blocked in milliseconds.
	 */
	public final void blockAddress(InetAddress address, long time) {
		this.blockAddress(address, "Blocked", time);
	}

	/**
	 * Unblocks the address.
	 * 
	 * @param address
	 *            the address to unblock.
	 */
	public final void unblockAddress(InetAddress address) {
		handler.unblockAddress(address);
	}

	/**
	 * @param address
	 *            the address to check.
	 * @return <code>true</code> if the address is blocked.
	 */
	public final boolean addressBlocked(InetAddress address) {
		return handler.addressBlocked(address);
	}

	/**
	 * Called whenever the handler catches an exception in Netty.
	 * 
	 * @param address
	 *            the address that caused the exception.
	 * @param cause
	 *            the exception caught by the handler.
	 */
	protected final void handleHandlerException(InetSocketAddress address, Throwable cause) {
		// Remove session that caused the error
		if (this.hasSession(address)) {
			this.removeSession(address, cause.getClass().getName());
		}

		// Notify API
		LOG.warn("Handled exception " + cause.getClass().getName() + " caused by address " + address);
		for (RakNetServerListener listener : listeners) {
			listener.onHandlerException(address, cause);
		}
	}

	/**
	 * Handles a packet received by the handler.
	 * 
	 * @param packet
	 *            the packet to handle.
	 * @param sender
	 *            the address of the sender.
	 */
	protected final void handleMessage(RakNetPacket packet, InetSocketAddress sender) {
		short packetId = packet.getId();

		if (packetId == ID_UNCONNECTED_PING || packetId == ID_UNCONNECTED_PING_OPEN_CONNECTIONS) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();
			if (ping.failed()) {
				return; // Bad packet, ignore
			}

			// Make sure parameters match and that broadcasting is enabled
			if ((packetId == ID_UNCONNECTED_PING || (sessions.size() < this.maxConnections || this.maxConnections < 0))
					&& this.broadcastingEnabled == true) {
				ServerPing pingEvent = new ServerPing(sender, identifier, ping.connectionType);
				for (RakNetServerListener listener : listeners) {
					listener.handlePing(pingEvent);
				}

				if (ping.magic == true && pingEvent.getIdentifier() != null) {
					UnconnectedPong pong = new UnconnectedPong();
					pong.timestamp = ping.timestamp;
					pong.pongId = this.pongId;
					pong.identifier = pingEvent.getIdentifier();

					pong.encode();
					if (!pong.failed()) {
						this.sendNettyMessage(pong, sender);
					} else {
						LOG.error(UnconnectedPong.class.getSimpleName() + " packet failed to encode");
					}
				}
			}
		} else if (packetId == ID_OPEN_CONNECTION_REQUEST_1) {
			OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne(packet);
			connectionRequestOne.decode();

			if (sessions.containsKey(sender)) {
				if (sessions.get(sender).getState().equals(RakNetState.CONNECTED)) {
					this.removeSession(sender, "Client re-instantiated connection");
				}
			}

			if (connectionRequestOne.magic == true) {
				// Are there any problems?
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (connectionRequestOne.protocolVersion != this.getProtocolVersion()) {
						// Incompatible protocol
						IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol();
						incompatibleProtocol.networkProtocol = this.getProtocolVersion();
						incompatibleProtocol.serverGuid = this.guid;
						incompatibleProtocol.encode();
						this.sendNettyMessage(incompatibleProtocol, sender);
					} else {
						// Everything passed, one last check...
						if (connectionRequestOne.maximumTransferUnit <= this.maximumTransferUnit) {
							OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne();
							connectionResponseOne.serverGuid = this.guid;
							connectionResponseOne.maximumTransferUnit = this.maximumTransferUnit;
							connectionResponseOne.encode();
							this.sendNettyMessage(connectionResponseOne, sender);
						}
					}
				} else {
					this.sendNettyMessage(errorPacket, sender);
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
						// This client is already connected
						this.sendNettyMessage(ID_ALREADY_CONNECTED, sender);
					} else {
						// Everything passed, one last check...
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
								for (RakNetServerListener listener : listeners) {
									listener.onClientPreConnect(sender);
								}

								// Create session
								RakNetClientSession clientSession = new RakNetClientSession(this,
										System.currentTimeMillis(), connectionRequestTwo.connectionType,
										connectionRequestTwo.clientGuid, connectionRequestTwo.maximumTransferUnit,
										channel, sender);
								sessions.put(sender, clientSession);

								// Send response, we are ready for login
								this.sendNettyMessage(connectionResponseTwo, sender);
							}
						}
					}
				} else {
					this.sendNettyMessage(errorPacket, sender);
				}
			}
		} else if (packetId >= ID_CUSTOM_0 && packetId <= ID_CUSTOM_F) {
			if (sessions.containsKey(sender)) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleCustom(custom);
			}
		} else if (packetId == Acknowledge.ACKNOWLEDGED || packetId == Acknowledge.NOT_ACKNOWLEDGED) {
			if (sessions.containsKey(sender)) {
				Acknowledge acknowledge = new Acknowledge(packet);
				acknowledge.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleAcknowledge(acknowledge);
			}
		}

		if (MessageIdentifier.hasPacket(packet.getId())) {
			LOG.debug("Handled internal packet with ID " + MessageIdentifier.getName(packet.getId()) + " ("
					+ packet.getId() + ")");
		} else {
			LOG.debug("Sent packet with ID " + packet.getId() + " to session handler");
		}
	}

	/**
	 * Validates the sender during login to make sure there are no problems.
	 * 
	 * @param sender
	 *            the address of the packet sender.
	 * @return the packet to respond with if there was an error.
	 */
	private final RakNetPacket validateSender(InetSocketAddress sender) {
		// Checked throughout all login
		if (this.hasSession(sender)) {
			return new RakNetPacket(ID_ALREADY_CONNECTED);
		} else if (this.getSessionCount() >= this.maxConnections && this.maxConnections >= 0) {
			// We have no free connections
			return new RakNetPacket(ID_NO_FREE_INCOMING_CONNECTIONS);
		} else if (this.addressBlocked(sender.getAddress())) {
			// Address is blocked
			ConnectionBanned connectionBanned = new ConnectionBanned();
			connectionBanned.serverGuid = this.guid;
			connectionBanned.encode();
			return connectionBanned;
		}

		// There were no errors
		return null;
	}

	/**
	 * Sends a raw message to the address. Be careful when using this
	 * method, because if it is used incorrectly it could break server sessions
	 * entirely! If you are wanting to send a message to a session, you are
	 * probably looking for the
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(com.whirvis.jraknet.protocol.Reliability, com.whirvis.jraknet.Packet)
	 * sendMessage} method.
	 * 
	 * @param buf
	 *            the buffer to send.
	 * @param address
	 *            the address to send the buffer to.
	 */
	public final void sendNettyMessage(ByteBuf buf, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(buf, address));
		LOG.debug("Sent netty message with size of " + buf.capacity() + " bytes (" + (buf.capacity() * 8) + ") to "
				+ address);
	}

	/**
	 * Sends a raw message to the address. Be careful when using this
	 * method, because if it is used incorrectly it could break server sessions
	 * entirely! If you are wanting to send a message to a session, you are
	 * probably looking for the
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(com.whirvis.jraknet.protocol.Reliability, com.whirvis.jraknet.Packet)
	 * sendMessage} method.
	 * 
	 * @param packet
	 *            the buffer to send.
	 * @param address
	 *            the address to send the buffer to.
	 */
	public final void sendNettyMessage(Packet packet, InetSocketAddress address) {
		this.sendNettyMessage(packet.buffer(), address);
	}

	/**
	 * Sends a raw message to the address. Be careful when using this
	 * method, because if it is used incorrectly it could break server sessions
	 * entirely! If you are wanting to send a message to a session, you are
	 * probably looking for the
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(com.whirvis.jraknet.protocol.Reliability, int)
	 * sendMessage} method.
	 * 
	 * @param packetId
	 *            the ID of the packet to send.
	 * @param address
	 *            the address to send the packet to.
	 */
	public final void sendNettyMessage(int packetId, InetSocketAddress address) {
		this.sendNettyMessage(new RakNetPacket(packetId), address);
	}

	/**
	 * Starts the server.
	 * 
	 * @throws RakNetException
	 *             if an error occurs during startup.
	 */
	public final void start() throws RakNetException {
		// Make sure we have a listener
		if (listeners.size() <= 0) {
			LOG.warn("Server has no listeners");
		}

		// Create bootstrap and bind the channel
		try {
			bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = bootstrap.bind(port).sync().channel();
			this.running = true;
			LOG.debug("Created and bound bootstrap");

			// Notify API
			LOG.info("Started server");
			for (RakNetServerListener listener : listeners) {
				listener.onServerStart();
			}

			// Update system
			while (this.running == true) {
				/*
				 * The order here is important, as the sleep could fail to
				 * execute (thus increasing the CPU usage dramatically) if we
				 * sleep before we execute the code in the for loop.
				 */
				try {
					Thread.sleep(0, 1); // Lower CPU usage
				} catch (InterruptedException e) {
					// Ignore this, it does not matter
				}

				for (RakNetClientSession session : sessions.values()) {
					try {
						// Update session and make sure it isn't DOSing us
						session.update();
						if (session.getPacketsReceivedThisSecond() >= RakNet.getMaxPacketsPerSecond()) {
							this.blockAddress(session.getInetAddress(), "Too many packets",
									RakNet.MAX_PACKETS_PER_SECOND_BLOCK);
						}
					} catch (Throwable throwable) {
						// An error related to the session occurred
						for (RakNetServerListener listener : listeners) {
							listener.onSessionException(session, throwable);
						}
						this.removeSession(session, throwable.getMessage());
					}
				}
			}

			// Shutdown netty
			group.shutdownGracefully().sync();
		} catch (InterruptedException e) {
			this.running = false;
			throw new RakNetException(e);
		}
	}

	/**
	 * Starts the server on its own <code>Thread</code>.
	 * 
	 * @return the <code>Thread</code> the server is running on.
	 */
	public final Thread startThreaded() {
		// Give the thread a reference
		RakNetServer server = this;

		// Create thread and start it
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					server.start();
				} catch (Throwable throwable) {
					if (server.getListeners().length > 0) {
						for (RakNetServerListener listener : server.getListeners()) {
							listener.onThreadException(throwable);
						}
					} else {
						throwable.printStackTrace();
					}
				}
			}
		};
		thread.setName("JRAKNET_SERVER_" + Long.toHexString(server.getGloballyUniqueId()).toUpperCase());
		thread.start();
		this.serverThread = thread;
		LOG.info("Started on thread with name " + thread.getName());

		// Return the thread so it can be modified
		return thread;
	}

	/**
	 * Stops the server.
	 * 
	 * @param reason
	 *            the reason the server shutdown.
	 */
	public final void shutdown(String reason) {
		// Tell the server to stop running
		this.running = false;

		// Disconnect sessions
		for (RakNetClientSession session : sessions.values()) {
			this.removeSession(session, reason);
		}
		sessions.clear();

		// Interrupt its thread if it owns one
		if (this.serverThread != null) {
			serverThread.interrupt();
		}

		// Notify API
		LOG.info("Shutdown server");
		for (RakNetServerListener listener : listeners) {
			listener.onServerShutdown();
		}
	}

	/**
	 * Stops the server.
	 */
	public final void shutdown() {
		this.shutdown("Server shutdown");
	}

	@Override
	public String toString() {
		return "RakNetServer [guid=" + guid + ", pongId=" + pongId + ", timestamp=" + timestamp + ", port=" + port
				+ ", maxConnections=" + maxConnections + ", maximumTransferUnit=" + maximumTransferUnit
				+ ", broadcastingEnabled=" + broadcastingEnabled + ", identifier=" + identifier + ", running=" + running
				+ "]";
	}

}
