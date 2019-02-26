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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.connection.ConnectionBanned;
import com.whirvis.jraknet.protocol.connection.IncompatibleProtocolVersion;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestTwo;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseTwo;
import com.whirvis.jraknet.protocol.message.CustomPacket;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.AcknowledgedPacket;
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

	// TODO: PREVENT MULTIPLE CLIENT WITH THE SAME GUID

	/**
	 * Used to convert a {@link java.util.stream.Stream Stream} to a
	 * <code>RakNetClientSession[]</code>.
	 */
	private static final IntFunction<RakNetClientSession[]> RAKNET_CLIENT_SESSION_FUNCTION = new IntFunction<RakNetClientSession[]>() {

		@Override
		public RakNetClientSession[] apply(int value) {
			return new RakNetClientSession[value];
		}

	};

	/**
	 * Allows for infinite connections to the server.
	 */
	public static final int INFINITE_CONNECTIONS = -1;

	private final long guid;
	private final Logger log;
	private final long pongId;
	private final long timestamp;
	private final InetSocketAddress bindAddress;
	private final int maximumTransferUnit;
	private int maxConnections;
	private boolean broadcastingEnabled;
	private Identifier identifier;
	private final ConcurrentLinkedQueue<RakNetServerListener> listeners;
	private final ConcurrentHashMap<InetSocketAddress, RakNetClientSession> clients;
	private Bootstrap bootstrap;
	private EventLoopGroup group;
	private RakNetServerHandler handler;
	private Channel channel;
	private Thread sessionThread;
	private volatile boolean running;

	/**
	 * Creates a RakNet server.
	 * 
	 * @param address
	 *            the address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @param identifier
	 *            the identifier that will be sent in response to server pings
	 *            if server broadcasting is enabled. A <code>null</code>
	 *            identifier means nothing will be sent in response to server
	 *            pings, even if server broadcasting is enabled.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than zero and not equal to
	 *             {@value #INFINITE_CONNECTIONS} or the maximum transfer unit
	 *             size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 */
	public RakNetServer(InetSocketAddress address, int maximumTransferUnit, int maxConnections, Identifier identifier)
			throws NullPointerException, IllegalArgumentException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
			throw new IllegalArgumentException(
					"Maximum transfer unit can be no smaller than " + RakNet.MINIMUM_MTU_SIZE);
		} else if (maxConnections < 0 && maxConnections != INFINITE_CONNECTIONS) {
			throw new IllegalArgumentException(
					"Maximum connections must be greater than or equal to zero or negative one (for infinite connections)");
		}
		UUID uuid = UUID.randomUUID();
		this.guid = uuid.getMostSignificantBits();
		this.log = LogManager.getLogger("jraknet-server-" + Long.toHexString(guid));
		this.pongId = uuid.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();
		this.bindAddress = address;
		this.maxConnections = maxConnections;
		this.maximumTransferUnit = maximumTransferUnit;
		this.broadcastingEnabled = true;
		this.identifier = identifier;
		this.listeners = new ConcurrentLinkedQueue<RakNetServerListener>();
		this.clients = new ConcurrentHashMap<InetSocketAddress, RakNetClientSession>();
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param address
	 *            the address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than zero and not equal to
	 *             {@value #INFINITE_CONNECTIONS} or the maximum transfer unit
	 *             size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 */
	public RakNetServer(InetSocketAddress address, int maximumTransferUnit, int maxConnections)
			throws NullPointerException, IllegalArgumentException {
		this(address, maximumTransferUnit, maxConnections, null);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param address
	 *            the IP address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param port
	 *            the port the server will bind to during startup.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @param identifier
	 *            the identifier that will be sent in response to server pings
	 *            if server broadcasting is enabled. A <code>null</code>
	 *            identifier means nothing will be sent in response to server
	 *            pings, even if server broadcasting is enabled.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than zero and not equal to
	 *             {@value #INFINITE_CONNECTIONS} or the maximum transfer unit
	 *             size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public RakNetServer(InetAddress address, int port, int maximumTransferUnit, int maxConnections,
			Identifier identifier) throws NullPointerException, IllegalArgumentException {
		this(new InetSocketAddress(address, port), maximumTransferUnit, maxConnections, identifier);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param address
	 *            the IP address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param port
	 *            the port the server will bind to during startup.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than zero and not equal to
	 *             {@value #INFINITE_CONNECTIONS} or the maximum transfer unit
	 *             size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public RakNetServer(InetAddress address, int port, int maximumTransferUnit, int maxConnections)
			throws NullPointerException, IllegalArgumentException {
		this(address, port, maximumTransferUnit, maxConnections, null);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param address
	 *            the IP address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param port
	 *            the port the server will bind to during startup.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @param identifier
	 *            the identifier that will be sent in response to server pings
	 *            if server broadcasting is enabled. A <code>null</code>
	 *            identifier means nothing will be sent in response to server
	 *            pings, even if server broadcasting is enabled.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>bindAddress</code> could be
	 *             found, or if a scope_id was specified for a global IPv6
	 *             address.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than zero and not equal to
	 *             {@value #INFINITE_CONNECTIONS} or the maximum transfer unit
	 *             size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public RakNetServer(String address, int port, int maximumTransferUnit, int maxConnections, Identifier identifier)
			throws UnknownHostException, NullPointerException, IllegalArgumentException {
		this(InetAddress.getByName(address), port, maximumTransferUnit, maxConnections, identifier);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param address
	 *            the IP address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param port
	 *            the port the server will bind to during startup.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>bindAddress</code> could be
	 *             found, or if a scope_id was specified for a global IPv6
	 *             address.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than zero and not equal to
	 *             {@value #INFINITE_CONNECTIONS} or the maximum transfer unit
	 *             size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public RakNetServer(String address, int port, int maximumTransferUnit, int maxConnections)
			throws UnknownHostException, NullPointerException, IllegalArgumentException {
		this(address, port, maximumTransferUnit, maxConnections, null);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param port
	 *            the port the server will bind to during startup.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @param identifier
	 *            the identifier that will be sent in response to server pings
	 *            if server broadcasting is enabled. A <code>null</code>
	 *            identifier means nothing will be sent in response to server
	 *            pings, even if server broadcasting is enabled.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than zero and not equal to
	 *             {@value #INFINITE_CONNECTIONS} or the maximum transfer unit
	 *             size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public RakNetServer(int port, int maximumTransferUnit, int maxConnections, Identifier identifier)
			throws NullPointerException, IllegalArgumentException {
		this(new InetSocketAddress((InetAddress) null, port), maximumTransferUnit, maxConnections, identifier);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param port
	 *            the port the server will bind to during startup.
	 * @param maximumTransferUnit
	 *            the highest maximum transfer unit a client can use. The
	 *            maximum transfer unit is the maximum number of bytes that can
	 *            be sent in one packet. If a packet exceeds this size, it is
	 *            automatically split up so that it can still be sent over the
	 *            connection (this is handled automatically by
	 *            {@link com.whirvis.jrkanet.session.RakNetSession
	 *            RakNetSession}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than zero and not equal to
	 *             {@value #INFINITE_CONNECTIONS} or the maximum transfer unit
	 *             size is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public RakNetServer(int port, int maximumTransferUnit, int maxConnections)
			throws NullPointerException, IllegalArgumentException {
		this(port, maximumTransferUnit, maxConnections, null);
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
	 * Returns the address the server is bound to.
	 * 
	 * @return the address the server is bound to.
	 */
	public final InetSocketAddress getAddress() {
		return this.bindAddress;
	}

	/**
	 * Returns the IP address the server is bound to.
	 * 
	 * @return the IP address the server is bound to.
	 */
	public final InetAddress getInetAddress() {
		return bindAddress.getAddress();
	}

	/**
	 * Returns the port the server is bound to.
	 * 
	 * @return the port the server is bound to.
	 */
	public final int getPort() {
		return bindAddress.getPort();
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
	 * Sets the maximum amount of connections allowed at once.
	 * 
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 */
	public final void setMaxConnections(int maxConnections) {
		if (maxConnections < 0 && maxConnections != INFINITE_CONNECTIONS) {
			throw new IllegalArgumentException(
					"Maximum connections must be greater than or equal to zero or negative one (for infinite connections)");
		}
		this.maxConnections = maxConnections;
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
		if (identifier != null) {
			log.info("Set identifier to \"" + identifier.build() + "\"");
		} else {
			log.info("Removed identifier");
		}
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
	 * @throws NullPointerException
	 *             if the event is <code>null</code>.
	 * @see com.whirvis.jraknet.server.RakNetServerListener RakNetServerListener
	 */
	protected final void callEvent(Consumer<? super RakNetServerListener> event) {
		if (event == null) {
			throw new NullPointerException("Event cannot be null");
		}
		for (RakNetServerListener listener : listeners) {
			Scheduler.scheduleSync(listener, event);
		}
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
	 * Returns whether or not a client with the address is currently connected
	 * to the server.
	 * 
	 * @param address
	 *            the address.
	 * @return <code>true</code> if a client with the address is connected to
	 *         the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(InetSocketAddress address) {
		if (address == null) {
			return false; // No address
		} else if (address.getAddress() == null) {
			return false; // No IP address
		}
		return clients.containsKey(address);
	}

	/**
	 * Returns whether or not a client with the address is currently connected
	 * to the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client with the address is connected to
	 *         the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(InetAddress address, int port) {
		if (port < 0x0000 || port > 0xFFFF) {
			return false; // Invalid port range
		}
		return this.hasClient(new InetSocketAddress(address, port));
	}

	/**
	 * Returns whether or not a client with the address is currently connected
	 * to the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client with the address is connected to
	 *         the server, <code>false</code> otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public final boolean hasClient(String address, int port) throws UnknownHostException {
		return this.hasClient(InetAddress.getByName(address), port);
	}

	/**
	 * Returns whether or not a client with the IP address is currently
	 * connected to the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @return <code>true</code> if a client with the IP address is connected to
	 *         the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(InetAddress address) {
		if (address == null) {
			return false; // No IP address
		}
		for (InetSocketAddress clientAddress : clients.keySet()) {
			if (clientAddress.equals(address)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether or not a client with the IP address is currently
	 * connected to the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @return <code>true</code> if a client with the IP address is connected to
	 *         the server, <code>false</code> otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public final boolean hasClient(String address) throws UnknownHostException {
		return this.hasClient(InetAddress.getByName(address));
	}

	/**
	 * Returns whether or not a client with the port is currently connected to
	 * the server.
	 * 
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client with the port is connected to the
	 *         server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(int port) {
		if (port < 0x0000 || port > 0xFFFF) {
			return false; // Invalid port range
		}
		for (InetSocketAddress clientAddress : clients.keySet()) {
			if (clientAddress.getPort() == port) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether or not the client with the globally unique ID is
	 * currently connected to the server.
	 * 
	 * @param guid
	 *            the globally unique ID.
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
	 *            the address.
	 * @return the client with the address, <code>null</code> if there is none.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final RakNetClientSession getClient(InetSocketAddress address) {
		return clients.get(address);
	}

	/**
	 * Returns the client with the address.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return the client with the address, <code>null</code> if there is none.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final RakNetClientSession getClient(InetAddress address, int port) {
		if (address == null) {
			return null; // No address
		} else if (port < 0x0000 || port > 0xFFFF) {
			return null; // Invalid port range
		}
		return this.getClient(new InetSocketAddress(address, port));
	}

	/**
	 * Returns the client with the address.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return the client with the address, <code>null</code> if there is none.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final RakNetClientSession getClient(String address, int port) throws UnknownHostException {
		if (address == null) {
			return null; // No address
		}
		return this.getClient(InetAddress.getByName(address), port);
	}

	/**
	 * Returns all clients with the IP address.
	 * 
	 * @param address
	 *            the IP address.
	 * @return the clients with the IP address.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final RakNetClientSession[] getClient(String address) throws UnknownHostException {
		if (address == null) {
			return new RakNetClientSession[0]; // No address
		}
		InetAddress inetAddress = InetAddress.getByName(address);
		return clients.values().stream().filter(session -> session.getAddress().equals(inetAddress))
				.toArray(RAKNET_CLIENT_SESSION_FUNCTION);
	}

	/**
	 * Returns all clients with the port.
	 * 
	 * @param address
	 *            the port.
	 * @return the clients with the port.
	 * @see com.whirvis.jraknet.session.RakNetClientSession RakNetClientSession
	 */
	public final RakNetClientSession[] getClient(int port) {
		if (port < 0x0000 || port > 0xFFFF) {
			return new RakNetClientSession[0]; // Invalid port range
		}
		return clients.values().stream().filter(session -> session.getPort() == port)
				.toArray(RAKNET_CLIENT_SESSION_FUNCTION);
	}

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
	 *            the address.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @returns <code>true</code> if a client was disconnected,
	 *          <code>false</code> otherwise.
	 */
	public final boolean disconnectClient(InetSocketAddress address, String reason) {
		RakNetClientSession session = clients.remove(address);
		if (session == null) {
			return false; // No client to disconnect
		}
		session.sendMessage(Reliability.UNRELIABLE, RakNetPacket.ID_DISCONNECTION_NOTIFICATION);
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
	 *            the address.
	 * @returns <code>true</code> if a client was disconnected,
	 *          <code>false</code> otherwise.
	 */
	public final boolean disconnectClient(InetSocketAddress address) {
		return this.disconnectClient(address, null);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnectClient(InetAddress address, int port, String reason) {
		if (port < 0x0000 || port > 0xFFFF) {
			return false; // Invalid port range
		}
		return this.disconnectClient(new InetSocketAddress(address, port), reason);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnectClient(InetAddress address, int port) {
		return this.disconnectClient(address, port, null);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnectClient(String address, int port, String reason) throws UnknownHostException {
		if (address == null) {
			return false; // No address
		}
		return this.disconnectClient(InetAddress.getByName(address), port, reason);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnectClient(String address, int port) throws UnknownHostException {
		return this.disconnectClient(address, port, null);
	}

	/**
	 * Disconnects all clients from the server with the address.
	 * 
	 * @param address
	 *            the IP address.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnectClients(InetAddress address, String reason) {
		AtomicBoolean disconnected = new AtomicBoolean(false);
		clients.keySet().stream().filter(sessionAddress -> sessionAddress.equals(address)).forEach(sessionAddress -> {
			this.disconnectClient(sessionAddress, reason);
			disconnected.set(true);
		});
		return disconnected.get();
	}

	/**
	 * Disconnects all clients from the server with the IP address.
	 * 
	 * @param address
	 *            the IP address.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnectClients(InetAddress address) {
		return this.disconnectClients(address, null);
	}

	/**
	 * Disconnects all clients from the server with the IP address.
	 * 
	 * @param address
	 *            the IP address.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnectClients(String address, String reason) throws UnknownHostException {
		return this.disconnectClients(InetAddress.getByName(address), reason);
	}

	/**
	 * Disconnects all clients from the server with the IP address.
	 * 
	 * @param address
	 *            the IP address.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnectClients(String address) throws UnknownHostException {
		return this.disconnectClients(address, null);
	}

	/**
	 * Disconnects all clients from the server with the port.
	 * 
	 * @param port
	 *            the port.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnectClients(int port, String reason) {
		AtomicBoolean disconnected = new AtomicBoolean(false);
		clients.keySet().stream().filter(sessionAddress -> sessionAddress.getPort() == port).forEach(sessionAddress -> {
			this.disconnectClient(sessionAddress, reason);
			disconnected.set(true);
		});
		return disconnected.get();
	}

	/**
	 * Disconnects all clients from the server with the port.
	 * 
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnectClients(int port) {
		return this.disconnectClients(port, null);
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
			throw new IllegalArgumentException("Session must belong to the server");
		}
		this.disconnectClient(session.getAddress(), reason);
	}

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
			this.disconnectClient(address, RakNet.getStackTrace(cause));
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
		short packetId = packet.getId();
		if (packetId == RakNetPacket.ID_UNCONNECTED_PING
				|| packetId == RakNetPacket.ID_UNCONNECTED_PING_OPEN_CONNECTIONS) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();
			if (ping.failed()) {
				return;
			}
			if ((packetId == RakNetPacket.ID_UNCONNECTED_PING
					|| (clients.size() < this.maxConnections || this.maxConnections < 0))
					&& this.broadcastingEnabled == true && ping.magic == true) {
				ServerPing pingEvent = new ServerPing(sender, ping.connectionType, identifier);
				this.callEvent(listener -> listener.handlePing(pingEvent));
				if (pingEvent.getIdentifier() != null) {
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
		} else if (packetId == RakNetPacket.ID_OPEN_CONNECTION_REQUEST_1) {
			OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne(packet);
			connectionRequestOne.decode();
			if (clients.containsKey(sender)) {
				if (clients.get(sender).getState().equals(RakNetState.CONNECTED)) {
					this.disconnectClient(sender, "Client reinstantiated connection");
				}
			}
			if (connectionRequestOne.magic == true) {
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (connectionRequestOne.networkProtocol != this.getProtocolVersion()) {
						IncompatibleProtocolVersion incompatibleProtocol = new IncompatibleProtocolVersion();
						incompatibleProtocol.networkProtocol = this.getProtocolVersion();
						incompatibleProtocol.serverGuid = guid;
						incompatibleProtocol.encode();
						this.sendNettyMessage(incompatibleProtocol, sender);
					} else {
						if (connectionRequestOne.maximumTransferUnit <= maximumTransferUnit) {
							OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne();
							connectionResponseOne.serverGuid = guid;
							connectionResponseOne.maximumTransferUnit = maximumTransferUnit;
							connectionResponseOne.encode();
							this.sendNettyMessage(connectionResponseOne, sender);
						}
					}
				} else {
					this.sendNettyMessage(errorPacket, sender);
				}
			}
		} else if (packetId == RakNetPacket.ID_OPEN_CONNECTION_REQUEST_2) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo(packet);
			connectionRequestTwo.decode();
			if (!connectionRequestTwo.failed() && connectionRequestTwo.magic == true) {
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (connectionRequestTwo.maximumTransferUnit <= this.maximumTransferUnit) {
						OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo();
						connectionResponseTwo.serverGuid = this.guid;
						connectionResponseTwo.clientAddress = sender;
						connectionResponseTwo.maximumTransferUnit = connectionRequestTwo.maximumTransferUnit;
						connectionResponseTwo.encode();
						if (!connectionResponseTwo.failed()) {
							this.callEvent(listener -> listener.onClientPreConnect(sender));
							RakNetClientSession clientSession = new RakNetClientSession(this,
									System.currentTimeMillis(), connectionRequestTwo.connectionType,
									connectionRequestTwo.clientGuid, connectionRequestTwo.maximumTransferUnit, channel,
									sender);
							clients.put(sender, clientSession);
							this.sendNettyMessage(connectionResponseTwo, sender);
						}
					}
				} else {
					this.sendNettyMessage(errorPacket, sender);
				}
			}
		} else if (packetId >= RakNetPacket.ID_CUSTOM_0 && packetId <= RakNetPacket.ID_CUSTOM_F) {
			if (clients.containsKey(sender)) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();
				RakNetClientSession session = clients.get(sender);
				session.handleCustom(custom);
			}
		} else if (packetId == RakNetPacket.ID_ACK || packetId == RakNetPacket.ID_NACK) {
			if (clients.containsKey(sender)) {
				AcknowledgedPacket acknowledge = new AcknowledgedPacket(packet);
				acknowledge.decode();
				RakNetClientSession session = clients.get(sender);
				session.handleAcknowledge(acknowledge);
			}
		}
		log.debug("Handled" + (RakNetPacket.hasPacket(packet.getId()) ? RakNetPacket.getName(packet.getId()) + " packet"
				: "packet with ID " + RakNet.toHexStringId(packet))); // TODO:
																		// Have
																		// get
																		// name
																		// returned
																		// hex
																		// string
																		// ID by
																		// default?
	}

	/**
	 * Validates the sender of a packet. This is called throughout initial
	 * client connection to make sure there are no issues.
	 * 
	 * @param sender
	 *            the address of the packet sender.
	 * @return the packet to respond with if there was an error,
	 *         <code>null</code> if there are no issues.
	 * @see com.whirvis.jraknet.RakNetPacket RakNetPacket
	 */
	private final RakNetPacket validateSender(InetSocketAddress sender) {
		if (this.hasClient(sender)) {
			return new RakNetPacket(RakNetPacket.ID_ALREADY_CONNECTED);
		} else if (this.getClientCount() >= this.maxConnections && this.maxConnections >= 0) {
			return new RakNetPacket(RakNetPacket.ID_NO_FREE_INCOMING_CONNECTIONS);
		} else if (this.isAddressBlocked(sender.getAddress())) {
			ConnectionBanned connectionBanned = new ConnectionBanned();
			connectionBanned.serverGuid = guid;
			connectionBanned.encode();
			return connectionBanned;
		}
		return null;
	}

	/**
	 * Sends a Netty message over the channel raw. This should be used
	 * sparingly, as if it is used incorrectly it could break client sessions
	 * entirely. In order to send a message to a session, use one of the
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(com.whirvis.jraknet.protocol.Reliability, io.netty.buffer.ByteBuf)
	 * sendMessage()} methods.
	 * 
	 * @param buf
	 *            the buffer to send.
	 * @param address
	 *            the address to send the buffer to.
	 * @see {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(Reliability, ByteBuf)
	 *      sendMessage(Reliability, ByteBuf)}
	 */
	public final void sendNettyMessage(ByteBuf buf, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(buf, address));
		log.debug("Sent netty message with size of " + buf.capacity() + " bytes (" + (buf.capacity() * 8) + ") to "
				+ address);
	}

	/**
	 * Sends a Netty message over the channel raw. This should be used
	 * sparingly, as if it is used incorrectly it could break client sessions
	 * entirely. In order to send a message to a session, use one of the
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(Reliability, Packet)
	 * sendMessage()} methods.
	 * 
	 * @param packet
	 *            the packet to send.
	 * @param address
	 *            the address to send the packet to.
	 * @see {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(Reliability, Packet)
	 *      sendMessage(Reliability, Packet)}
	 */
	public final void sendNettyMessage(Packet packet, InetSocketAddress address) {
		this.sendNettyMessage(packet.buffer(), address);
	}

	/**
	 * Sends a Netty message over the channel raw. This should be used
	 * sparingly, as if it is used incorrectly it could break client sessions
	 * entirely. In order to send a message to a session, use one of the
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(com.whirvis.jraknet.protocol.Reliability, int)
	 * sendMessage()} methods.
	 * 
	 * @param buffer
	 *            the packet ID to send.
	 * @param address
	 *            the address to send the packet to.
	 * @see {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(com.whirvis.jraknet.protocol.Reliability, int)
	 *      sendMessage(Reliability, int)}
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
		if (running == true) {
			throw new IllegalStateException("Server is already running");
		} else if (listeners.isEmpty()) {
			log.warn("Server has no listeners");
		}

		try {
			this.bootstrap = new Bootstrap();
			this.group = new NioEventLoopGroup();
			this.handler = new RakNetServerHandler(this);
			bootstrap.handler(handler);

			// Create bootstrap and bind channel
			bootstrap.channel(NioDatagramChannel.class).group(group);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = (bindAddress != null ? bootstrap.bind(bindAddress) : bootstrap.bind(0)).sync().channel();
			this.running = true;
			log.debug("Created and bound bootstrap");

			// Create and start session update thread
			RakNetServer server = this;
			this.sessionThread = new Thread("jraknet-session-thread-" + Long.toHexString(guid)) {

				@Override
				public void run() {
					try {
						while (server.running == true && !this.isInterrupted()) {
							Thread.sleep(0, 1); // Lower CPU usage
							for (RakNetClientSession session : clients.values()) {
								try {
									session.update();
									if (session.getPacketsReceivedThisSecond() >= RakNet.getMaxPacketsPerSecond()) {
										server.blockAddress(session.getInetAddress(), "Too many packets",
												RakNet.MAX_PACKETS_PER_SECOND_BLOCK);
									}
								} catch (Throwable throwable) {
									for (RakNetServerListener listener : listeners) {
										listener.onSessionException(session, throwable);
									}
									server.disconnectClient(session, throwable.getMessage());
								}
							}
						}
					} catch (InterruptedException e) {
						this.interrupt(); // Interrupted during sleep
					}
				}

			};
			sessionThread.start();
			log.debug("Created and started session update thread");
			this.callEvent(listener -> listener.onServerStart());
		} catch (InterruptedException e) {
			this.running = false;
			throw new RakNetException(e);
		}
		log.info("Started server");
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
		if (running == false) {
			throw new IllegalStateException("Server is not running");
		}

		// Disconnect clients
		clients.values().stream()
				.forEach(session -> this.disconnectClient(session, reason == null ? "Server shutdown" : reason));
		clients.clear();

		// Stop server
		this.running = false;
		sessionThread.interrupt();
		log.info("Shutdown server");

		// Shutdown networking
		channel.close();
		group.shutdownGracefully(0L, 1000L, TimeUnit.MILLISECONDS);
		this.channel = null;
		this.handler = null;
		this.group = null;
		this.bootstrap = null;
		log.debug("Shutdown networking");
		this.callEvent(listener -> listener.onServerShutdown());
	}

	/**
	 * Stops the server.
	 */
	public final void shutdown() {
		this.shutdown(null);
	}

	@Override
	public String toString() {
		return "RakNetServer [guid=" + guid + ", log=" + log + ", pongId=" + pongId + ", timestamp=" + timestamp
				+ ", bindAddress=" + bindAddress + ", maximumTransferUnit=" + maximumTransferUnit + ", maxConnections="
				+ maxConnections + ", broadcastingEnabled=" + broadcastingEnabled + ", identifier=" + identifier
				+ ", listeners=" + listeners + ", running=" + running + ", getClientCount()=" + getClientCount() + "]";
	}

}
