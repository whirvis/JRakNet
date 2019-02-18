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
import java.util.function.Consumer;

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
import com.whirvis.jraknet.scheduler.Scheduler;
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
 * Used to create servers using the RakNet protocol.
 *
 * @author Whirvis T. Wheatley
 */
public class RakNetServer implements GeminusRakNetPeer, RakNetServerListener {

	/**
	 * Allows for infinite connections to the server.
	 */
	public static final int INFINITE_CONNECTIONS = -1;

	private final long guid;
	private final Logger log;
	private final long pongId;
	private final long timestamp;
	private final int port;
	private final int maxConnections;
	private final int maximumTransferUnit;
	private boolean broadcastingEnabled;
	private Identifier identifier;
	private final ConcurrentLinkedQueue<RakNetServerListener> listeners;
	private Thread serverThread;
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetServerHandler handler;
	private Channel channel;
	private volatile boolean running;
	private final ConcurrentHashMap<InetSocketAddress, RakNetClientSession> clients;

	/**
	 * Creates a RakNet server.
	 * 
	 * @param port
	 *            the server port.
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param identifier
	 *            the identifier that will be sent in response to server pings
	 *            if server broadcasting is enabled. A <code>null</code>
	 *            identifier means nothing will be sent in response to server
	 *            pings, even if server broadcasting is enabled.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 * @throws IllegalArgumentException
	 *             if the <code>maximumTransferUnit</code> size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 */
	public RakNetServer(int port, int maxConnections, int maximumTransferUnit, Identifier identifier) {
		if (maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
			throw new IllegalArgumentException(
					"Maximum transfer unit can be no smaller than " + RakNet.MINIMUM_MTU_SIZE);
		}

		// Generate server information
		UUID uuid = UUID.randomUUID();
		this.guid = uuid.getMostSignificantBits();
		this.log = LogManager.getLogger("RakNet server #" + Long.toHexString(guid).toUpperCase());
		this.pongId = uuid.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();
		this.port = port;
		this.maxConnections = maxConnections;
		this.maximumTransferUnit = maximumTransferUnit;
		this.broadcastingEnabled = true;
		this.identifier = identifier;
		this.listeners = new ConcurrentLinkedQueue<RakNetServerListener>();

		// Initiate networking
		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetServerHandler(this);
		bootstrap.handler(handler);
		this.clients = new ConcurrentHashMap<InetSocketAddress, RakNetClientSession>();

	}

	/**
	 * Returns the server's networking protocol version.
	 * 
	 * @return the server's networking protocol version.
	 */
	public final int getProtocolVersion() {
		return RakNet.SERVER_NETWORK_PROTOCOL;
	}

	/**
	 * Returns the server's globally unique ID.
	 * 
	 * @return the server's globally unique ID.
	 */
	public final long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * Returns the server's timestamp.
	 * 
	 * @return the server's timestamp.
	 */
	public final long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	/**
	 * Returns the port the server is bound to.
	 * 
	 * @return the port the server is bound to.
	 */
	public final int getPort() {
		return this.port;
	}

	/**
	 * Returns the maximum amount of connections allowed at once.
	 * 
	 * @return the maximum amount of connections allowed at once,
	 *         {@value #INFINITE_CONNECTIONS} if an infinite amount of
	 *         connections are allowed.
	 */
	public final int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * Returns the maximum transfer unit.
	 * 
	 * @return the maximum transfer unit.
	 */
	public final int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Enables/disables server broadcasting.
	 * 
	 * @param enabled
	 *            <code>true</code> to enable broadcasting, <code>false</code>
	 *            to disable broadcasting.
	 */
	public final void setBroadcastingEnabled(boolean enabled) {
		boolean wasBroadcasting = this.broadcastingEnabled;
		this.broadcastingEnabled = enabled;
		if (wasBroadcasting != enabled) {
			log.info((enabled ? "Enabled" : "Disabled") + " broadcasting");
		}
	}

	/**
	 * Returns whether or not broadcasting is enabled.
	 * 
	 * @return <code>true</code> if broadcasting is enabled, <code>false</code>
	 *         otherwise.
	 */
	public final boolean isBroadcastingEnabled() {
		return this.broadcastingEnabled;
	}

	/**
	 * Returns the identifier sent back to clients who ping the server.
	 * 
	 * @return the identifier sent back to clients who ping the server.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public final Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Sets the server's identifier used for discovery.
	 * 
	 * @param identifier
	 *            the new identifier.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public final void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
		log.info("Set identifier to \"" + identifier.build() + "\"");
	}

	/**
	 * Adds a listener to the server. Listeners are used to listen for events
	 * that occur relating to the server such as clients connecting to the
	 * server, receiving messages, and more.
	 * 
	 * @param listener
	 *            the listener to add.
	 * @return the server.
	 * @throws NullPointerEception
	 *             if the listener is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the listener is another server that is not the server
	 *             itself.
	 */
	public final RakNetServer addListener(RakNetServerListener listener) {
		if (listener == null) {
			throw new NullPointerException("Listener cannot be null");
		} else if (listener instanceof RakNetClient && !this.equals(listener)) {
			throw new IllegalArgumentException("A server cannot be used as a listener except for itself");
		} else if (listeners.contains(listener)) {
			return this; // Prevent duplicates
		}
		listeners.add(listener);
		log.info("Added listener of class " + listener.getClass().getName());
		return this;
	}

	/**
	 * Adds the server to its own set of listeners, used when extending the
	 * {@link RakNetServer} directly.
	 * 
	 * @return the server.
	 * @see #addListener(RakNetServerListener)
	 * @see com.whirvis.jraknet.server.RakNetServerListener RakNetServerListener
	 */
	public final RakNetServer addSelfListener() {
		return this.addListener(this);
	}

	/**
	 * Removes a listener from the server.
	 * 
	 * @param listener
	 *            the listener to remove.
	 * @return the server.
	 * @see com.whirvis.jraknet.server.RakNetServerListener RakNetServerListener
	 */
	public final RakNetServer removeListener(RakNetServerListener listener) {
		if (listeners.remove(listener)) {
			log.info("Removed listener of class " + listener.getClass().getName());
		} else {
			log.warn("Attempted to removed unregistered listener of class " + listener);
		}
		return this;
	}

	/**
	 * Removes the server from its own set of listeners, used when extending the
	 * {@link RakNetServer} directly.
	 * 
	 * @return the server.
	 * @see #removeListener(RakNetServerListener)
	 * @see com.whirvis.jraknet.server.RakNetServerListener RakNetServerListener
	 */
	public final RakNetServer removeSelfListener() {
		this.removeListener(this);
		return this;
	}

	/**
	 * Calls the event.
	 * 
	 * @param event
	 *            the event to call.
	 * @see com.whirvis.jraknet.server.RakNetServerListener RakNetServerListener
	 */
	protected final void callEvent(Consumer<? super RakNetServerListener> event) {
		listeners.forEach(listener -> Scheduler.scheduleSync(listener, event));
	}

	/**
	 * Returns the clients connected to the server.
	 * 
	 * @return the clients connected to the server.
	 */
	public final RakNetClientSession[] getClients() {
		return clients.values().toArray(new RakNetClientSession[clients.size()]);
	}

	/**
	 * Returns the amount of clients connected to the server.
	 * 
	 * @return the amount of clients connected to the server.
	 */
	public final int getClientCount() {
		return clients.size();
	}

	/**
	 * Returns whether or not the client with the address is currently connected
	 * to the server.
	 * 
	 * @param address
	 *            the address to check.
	 * @return <code>true</code> if a client with the address is connected to
	 *         the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(InetSocketAddress address) {
		return clients.containsKey(address);
	}

	// TODO: InetAddress address, int port
	// TODO: String address, int port

	/**
	 * Returns whether or not the client with the globally unique ID is
	 * currently connected to the server.
	 * 
	 * @param guid
	 *            the globally unique ID to check.
	 * @return <code>true</code> if a client with the globally unique ID is
	 *         connected to the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(long guid) {
		for (RakNetClientSession session : clients.values()) {
			if (session.getGloballyUniqueId() == guid) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the client with the address.
	 * 
	 * @param address
	 *            the address of the client.
	 * @return the client with the address, <code>null</code> if there is none.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final RakNetClientSession getClient(InetSocketAddress address) {
		return clients.get(address);
	}

	// TODO: InetAddress address, int port
	// TODO: String address, int port

	/**
	 * Returns the client with the globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the client.
	 * @return the client with the globally unique ID, <code>null</code> if
	 *         there is none.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final RakNetClientSession getClient(long guid) {
		for (RakNetClientSession session : clients.values()) {
			if (session.getGloballyUniqueId() == guid) {
				return session;
			}
		}
		return null;
	}

	@Override
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, Packet packet) {
		if (!this.hasClient(guid)) {
			throw new IllegalArgumentException("No client with the specified GUID exists");
		}
		return this.getClient(guid).sendMessage(reliability, channel, packet);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the address of the client.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @returns <code>true</code> if a client was disconnected,
	 *          <code>false</code> otherwise.
	 */
	public final boolean disconnectClient(InetSocketAddress address, String reason) {
		if (!clients.containsKey(address)) {
			return false; // No client to disconnect
		}

		RakNetClientSession session = clients.remove(address);
		session.sendMessage(Reliability.UNRELIABLE, ID_DISCONNECTION_NOTIFICATION);
		log.debug("Disconnected client with address " + address + " for \"" + (reason == null ? "Disconnected" : reason)
				+ "\"");
		if (session.getState() == RakNetState.CONNECTED) {
			this.callEvent(listener -> listener.onClientDisconnect(session, reason == null ? "Disconnected" : reason));
		} else {
			this.callEvent(
					listener -> listener.onClientPreDisconnect(address, reason == null ? "Disconnected" : reason));
		}
		return true;
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the address of the client.
	 * @returns <code>true</code> if a client was disconnected,
	 *          <code>false</code> otherwise.
	 */
	public final boolean disconnectClient(InetSocketAddress address) {
		return this.disconnectClient(address, null);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param session
	 *            the session of the client to disconnect.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code> value
	 *            will have <code>"Disconnected"</code> be used as the reason
	 *            instead.
	 * @return <code>true</code> if a client was disconnected,
	 *         <code>false</code> otherwise.
	 * @throws IllegalArgumentException
	 *             if the given session is fabricated, meaning that the session
	 *             is not one created by the server but rather one created
	 *             externally.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final void disconnectClient(RakNetClientSession session, String reason) {
		if (!clients.containsValue(session)) {
			throw new IllegalArgumentException("Session must be of the server"); // TODO
																					// Fix
																					// message
		}
		this.disconnectClient(session.getAddress(), reason);
	}

	// TODO: Add disconnectClients(InetAddress, String)
	// TODO: Add disconnectClients(InetAddress)

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param session
	 *            the session of the client to disconnect.
	 * @return <code>true</code> if a client was disconnected,
	 *         <code>false</code> otherwise.
	 * @throws IllegalArgumentException
	 *             if the given session is fabricated, meaning that the session
	 *             is not one created by the server but rather one created
	 *             externally.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final void disconnectClient(RakNetClientSession session) {
		this.disconnectClient(session, null);
	}

	/**
	 * Blocks the IP address. All currently connected clients with the IP
	 * address (regardless of port) will be disconnected with the same reason
	 * that the IP address was blocked.
	 * 
	 * @param address
	 *            the IP address to block.
	 * @param reason
	 *            the reason the address was blocked. A <code>null</code> reason
	 *            will have <code>"Address blocked"</code> be used as the reason
	 *            instead.
	 * @param time
	 *            how long the address will blocked in milliseconds.
	 * @throws NullPointerException
	 *             if <code>address</code> is <code>null</code>.
	 */
	public final void blockAddress(InetAddress address, String reason, long time) {
		handler.blockAddress(address, reason, time);
	}

	/**
	 * Blocks the IP address. All currently connected clients with the IP
	 * address (regardless of port) will be disconnected with the same reason
	 * that the IP address was blocked.
	 * 
	 * @param address
	 *            the IP address to block.
	 * @param time
	 *            how long the address will blocked in milliseconds.
	 * @throws NullPointerException
	 *             if <code>address</code> is <code>null</code>.
	 * @see #blockAddress(InetAddress, String, long)
	 */
	public final void blockAddress(InetAddress address, long time) {
		this.blockAddress(address, null, time);
	}

	/**
	 * Unblocks the IP address.
	 * 
	 * @param address
	 *            the IP address to unblock.
	 * @throws NullPointerException
	 *             if <code>address</code> is <code>null</code>.
	 */
	public final void unblockAddress(InetAddress address) {
		handler.unblockAddress(address);
	}

	/**
	 * Returns whether or not the IP address is blocked.
	 * 
	 * @param address
	 *            the IP address to check.
	 * @return <code>true</code> if the IP address is blocked,
	 *         <code>false</code> otherwise.
	 */
	public final boolean isAddressBlocked(InetAddress address) {
		return handler.isAddressBlocked(address);
	}

	/**
	 * Called by the {@link com.whirvis.jraknet.server.RakNetServerHandler
	 * RakNetServerHandler} when it catches a <code>Throwable</code> while
	 * handling a packet.
	 * 
	 * @param address
	 *            the address that caused the exception.
	 * @param cause
	 *            the <code>Throwable</code> caught by the handler.
	 */
	protected final void handleHandlerException(InetSocketAddress address, Throwable cause) {
		if (this.hasClient(address)) {
			// TODO: use printed stack trace
			this.disconnectClient(address, cause.getClass().getName());
		}
		log.warn("Handled exception " + cause.getClass().getName() + " caused by address " + address);
		this.callEvent(listener -> listener.onHandlerException(address, cause));
	}

	/**
	 * Handles a packet received by the
	 * {@link com.whirvis.jraknet.server.RakNetServerHandler
	 * RakNetServerHandler}.
	 * 
	 * @param packet
	 *            the packet to handle.
	 * @param sender
	 *            the address of the sender.
	 * @see com.whirvis.jraknet.RakNetPacket RakNetPacket
	 * @see com.whirvis.jrkanet.RakNetClientHandler RakNetClientHandler
	 */
	protected final void handleMessage(RakNetPacket packet, InetSocketAddress sender) {
		// TODO: Redocument this
		short packetId = packet.getId();
		if (packetId == ID_UNCONNECTED_PING || packetId == ID_UNCONNECTED_PING_OPEN_CONNECTIONS) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();
			if (ping.failed()) {
				return;
			}
			if ((packetId == ID_UNCONNECTED_PING || (clients.size() < this.maxConnections || this.maxConnections < 0))
					&& this.broadcastingEnabled == true) {
				ServerPing pingEvent = new ServerPing(sender, identifier, ping.connectionType);
				this.callEvent(listener -> listener.handlePing(pingEvent));
				if (ping.magic == true && pingEvent.getIdentifier() != null) {
					UnconnectedPong pong = new UnconnectedPong();
					pong.timestamp = ping.timestamp;
					pong.pongId = this.pongId;
					pong.identifier = pingEvent.getIdentifier();
					pong.encode();
					if (!pong.failed()) {
						this.sendNettyMessage(pong, sender);
					} else {
						log.error(pong.getClass().getSimpleName() + " packet failed to encode");
					}
				}
			}
		} else if (packetId == ID_OPEN_CONNECTION_REQUEST_1) {
			OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne(packet);
			connectionRequestOne.decode();
			if (clients.containsKey(sender)) {
				if (clients.get(sender).getState().equals(RakNetState.CONNECTED)) {
					this.disconnectClient(sender, "Client re-instantiated connection");
				}
			}
			if (connectionRequestOne.magic == true) {
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (connectionRequestOne.protocolVersion != this.getProtocolVersion()) {
						IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol();
						incompatibleProtocol.networkProtocol = this.getProtocolVersion();
						incompatibleProtocol.serverGuid = this.guid;
						incompatibleProtocol.encode();
						this.sendNettyMessage(incompatibleProtocol, sender);
					} else {
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
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (this.hasClient(connectionRequestTwo.clientGuid)) {
						this.sendNettyMessage(ID_ALREADY_CONNECTED, sender);
					} else {
						if (connectionRequestTwo.maximumTransferUnit <= this.maximumTransferUnit) {
							OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo();
							connectionResponseTwo.serverGuid = this.guid;
							connectionResponseTwo.clientAddress = sender;
							connectionResponseTwo.maximumTransferUnit = connectionRequestTwo.maximumTransferUnit;
							connectionResponseTwo.encryptionEnabled = false;
							connectionResponseTwo.encode();
							if (!connectionResponseTwo.failed()) {
								this.callEvent(listener -> listener.onClientPreConnect(sender));
								RakNetClientSession clientSession = new RakNetClientSession(this,
										System.currentTimeMillis(), connectionRequestTwo.connectionType,
										connectionRequestTwo.clientGuid, connectionRequestTwo.maximumTransferUnit,
										channel, sender);
								clients.put(sender, clientSession);
								this.sendNettyMessage(connectionResponseTwo, sender);
							}
						}
					}
				} else {
					this.sendNettyMessage(errorPacket, sender);
				}
			}
		} else if (packetId >= ID_CUSTOM_0 && packetId <= ID_CUSTOM_F) {
			if (clients.containsKey(sender)) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();
				RakNetClientSession session = clients.get(sender);
				session.handleCustom(custom);
			}
		} else if (packetId == Acknowledge.ACKNOWLEDGED || packetId == Acknowledge.NOT_ACKNOWLEDGED) {
			if (clients.containsKey(sender)) {
				Acknowledge acknowledge = new Acknowledge(packet);
				acknowledge.decode();
				RakNetClientSession session = clients.get(sender);
				session.handleAcknowledge(acknowledge);
			}
		}
		if (MessageIdentifier.hasPacket(packet.getId())) {
			log.debug("Handled internal packet with ID " + MessageIdentifier.getName(packet.getId()) + " ("
					+ packet.getId() + ")");
		} else {
			log.debug("Sent packet with ID " + packet.getId() + " to session handler");
		}
	}

	/**
	 * Validates the sender of a packet. This is called throughout initial
	 * client connection to make sure there are no issues.
	 * 
	 * @param sender
	 *            the address of the packet sender.
	 * @return the packet to respond with if there was an error.
	 * @see com.whirvis.jraknet.RakNetPacket RakNetPacket
	 */
	private final RakNetPacket validateSender(InetSocketAddress sender) {
		if (this.hasClient(sender)) {
			return new RakNetPacket(ID_ALREADY_CONNECTED);
		} else if (this.getClientCount() >= this.maxConnections && this.maxConnections >= 0) {
			return new RakNetPacket(ID_NO_FREE_INCOMING_CONNECTIONS);
		} else if (this.isAddressBlocked(sender.getAddress())) {
			ConnectionBanned connectionBanned = new ConnectionBanned();
			connectionBanned.serverGuid = this.guid;
			connectionBanned.encode();
			return connectionBanned;
		}
		return null; // No issues
	}

	/**
	 * Sends a raw message to the address. Be careful when using this method,
	 * because if it is used incorrectly it could break server sessions
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
		log.debug("Sent netty message with size of " + buf.capacity() + " bytes (" + (buf.capacity() * 8) + ") to "
				+ address);
	}

	/**
	 * Sends a raw message to the address. Be careful when using this method,
	 * because if it is used incorrectly it could break server sessions
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
	 * Sends a raw message to the address. Be careful when using this method,
	 * because if it is used incorrectly it could break server sessions
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
		if (listeners.size() <= 0) {
			log.warn("Server has no listeners");
		}

		try {
			// Create bootstrap and bind channel
			bootstrap.channel(NioDatagramChannel.class).group(group);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = bootstrap.bind(port).sync().channel();
			this.running = true;
			log.debug("Created and bound bootstrap");

			// Start server
			this.callEvent(listener -> listener.onServerStart());
			log.info("Started server");

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

				for (RakNetClientSession session : clients.values()) {
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
						this.disconnectClient(session, throwable.getMessage());
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
					server.callEvent(listener -> listener.onThreadException(throwable));
				}
			}
		};
		thread.setName("JRAKNET_SERVER_" + Long.toHexString(server.getGloballyUniqueId()).toUpperCase());
		thread.start();
		this.serverThread = thread;
		log.info("Started on thread with name " + thread.getName());

		// Return the thread so it can be modified
		return thread;
	}

	/**
	 * Stops the server. All currently connected clients will be disconnected
	 * with the same reason used for shutdown.
	 * 
	 * @param reason
	 *            the reason for shutdown. A <code>null</code> reason will have
	 *            </code>"Server shutdown"</code> be used as the reason instead.
	 */
	public final void shutdown(String reason) {
		// Disconnect clients
		clients.values().stream()
				.forEach(session -> this.disconnectClient(session, reason == null ? "Server shutdown" : reason));
		clients.clear();

		// Stop server
		this.running = false;
		if (this.serverThread != null) {
			serverThread.interrupt();
		}
		log.info("Shutdown server");
		this.callEvent(listener -> listener.onServerShutdown());
	}

	/**
	 * Stops the server. All cu
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
