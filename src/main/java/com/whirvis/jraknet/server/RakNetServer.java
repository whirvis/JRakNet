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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.InvalidChannelException;
import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.ThreadedListener;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.connection.ConnectionBanned;
import com.whirvis.jraknet.protocol.connection.IncompatibleProtocolVersion;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestTwo;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseTwo;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.status.UnconnectedPing;
import com.whirvis.jraknet.protocol.status.UnconnectedPong;

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
 * @since JRakNet v1.0.0
 */
public class RakNetServer implements RakNetServerListener {

	/**
	 * No globally unique ID for the
	 * {@link #validateSender(InetSocketAddress, long)} method.
	 */
	private static final int NO_GUID = -1;

	/**
	 * Has the maximum transfer unit automatically determined during startup.
	 */
	public static final int AUTOMATIC_MTU = -1;

	/**
	 * Allows for infinite connections to the server.
	 */
	public static final int INFINITE_CONNECTIONS = -1;

	private final InetSocketAddress bindingAddress;
	private final long guid;
	private final Logger log;
	private final long pongId;
	private final long timestamp;
	private final int maximumTransferUnit;
	private int maxConnections;
	private boolean broadcastingEnabled;
	private Identifier identifier;
	private int eventThreadCount;
	private final ConcurrentLinkedQueue<RakNetServerListener> listeners;
	private final ConcurrentHashMap<InetSocketAddress, RakNetClientPeer> clients;
	private final ConcurrentLinkedQueue<InetAddress> banned;
	private Bootstrap bootstrap;
	private EventLoopGroup group;
	private RakNetServerHandler handler;
	private Channel channel;
	private InetSocketAddress bindAddress;
	private Thread peerThread;
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
	 *            {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer}). A
	 *            value of {@value #AUTOMATIC_MTU} will have the maximum
	 *            transfer unit be determined automatically via
	 *            {@link RakNet#getMaximumTransferUnit(InetAddress)} with the
	 *            parameter being the specified bind <code>address</code>.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
	 */
	public RakNetServer(InetSocketAddress address, int maximumTransferUnit, int maxConnections, Identifier identifier)
			throws NullPointerException, IllegalArgumentException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE && maximumTransferUnit != AUTOMATIC_MTU) {
			throw new IllegalArgumentException(
					"Maximum transfer unit must be no smaller than " + RakNet.MINIMUM_MTU_SIZE + " or equal to "
							+ AUTOMATIC_MTU + " for the maximum transfer unit to be determined automatically");
		} else if (maxConnections < 0 && maxConnections != INFINITE_CONNECTIONS) {
			throw new IllegalArgumentException("Maximum connections must be greater than or equal to 0 or "
					+ INFINITE_CONNECTIONS + " for infinite connections");
		}
		UUID uuid = UUID.randomUUID();
		this.bindingAddress = address;
		this.guid = uuid.getMostSignificantBits();
		this.log = LogManager
				.getLogger(RakNetServer.class.getSimpleName() + "-" + Long.toHexString(guid).toUpperCase());
		this.pongId = uuid.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();
		this.maxConnections = maxConnections;
		this.maximumTransferUnit = maximumTransferUnit == AUTOMATIC_MTU ? RakNet.getMaximumTransferUnit(address)
				: maximumTransferUnit;
		this.broadcastingEnabled = true;
		this.identifier = identifier;
		this.listeners = new ConcurrentLinkedQueue<RakNetServerListener>();
		this.clients = new ConcurrentHashMap<InetSocketAddress, RakNetClientPeer>();
		this.banned = new ConcurrentLinkedQueue<InetAddress>();
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param address
	 *            the address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS}.
	 */
	public RakNetServer(InetSocketAddress address, int maxConnections, Identifier identifier)
			throws NullPointerException, IllegalArgumentException {
		this(address, AUTOMATIC_MTU, maxConnections, identifier);
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
	 *            {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer}). A
	 *            value of {@value #AUTOMATIC_MTU} will have the maximum
	 *            transfer unit be determined automatically via
	 *            {@link RakNet#getMaximumTransferUnit(InetAddress)} with the
	 *            parameter being the specified bind <code>address</code>.
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
	 */
	public RakNetServer(InetSocketAddress address, int maximumTransferUnit, int maxConnections)
			throws NullPointerException, IllegalArgumentException {
		this(address, maximumTransferUnit, maxConnections, null);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param address
	 *            the address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS}.
	 */
	public RakNetServer(InetSocketAddress address, int maxConnections)
			throws NullPointerException, IllegalArgumentException {
		this(address, AUTOMATIC_MTU, maxConnections);
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
	 *            {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer}). A
	 *            value of {@value #AUTOMATIC_MTU} will have the maximum
	 *            transfer unit be determined automatically via
	 *            {@link RakNet#getMaximumTransferUnit(InetAddress)} with the
	 *            parameter being the specified bind <code>address</code>.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS}.
	 */
	public RakNetServer(InetAddress address, int port, int maxConnections, Identifier identifier)
			throws NullPointerException, IllegalArgumentException {
		this(address, port, AUTOMATIC_MTU, maxConnections, identifier);
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
	 *            {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer}). A
	 *            value of {@value #AUTOMATIC_MTU} will have the maximum
	 *            transfer unit be determined automatically via
	 *            {@link RakNet#getMaximumTransferUnit(InetAddress)} with the
	 *            parameter being the specified bind <code>address</code>.
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
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
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS}.
	 */
	public RakNetServer(InetAddress address, int port, int maxConnections)
			throws NullPointerException, IllegalArgumentException {
		this(address, port, AUTOMATIC_MTU, maxConnections);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param host
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
	 *            {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer}). A
	 *            value of {@value #AUTOMATIC_MTU} will have the maximum
	 *            transfer unit be determined automatically via
	 *            {@link RakNet#getMaximumTransferUnit(InetAddress)} with the
	 *            parameter being the specified bind <code>address</code>.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
	 */
	public RakNetServer(String host, int port, int maximumTransferUnit, int maxConnections, Identifier identifier)
			throws UnknownHostException, NullPointerException, IllegalArgumentException {
		this(InetAddress.getByName(host), port, maximumTransferUnit, maxConnections, identifier);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param host
	 *            the IP address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param port
	 *            the port the server will bind to during startup.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS}.
	 */
	public RakNetServer(String host, int port, int maxConnections, Identifier identifier)
			throws UnknownHostException, NullPointerException, IllegalArgumentException {
		this(host, port, AUTOMATIC_MTU, maxConnections, identifier);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param host
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
	 *            {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer}). A
	 *            value of {@value #AUTOMATIC_MTU} will have the maximum
	 *            transfer unit be determined automatically via
	 *            {@link RakNet#getMaximumTransferUnit(InetAddress)} with the
	 *            parameter being the specified bind <code>address</code>.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
	 */
	public RakNetServer(String host, int port, int maximumTransferUnit, int maxConnections)
			throws UnknownHostException, NullPointerException, IllegalArgumentException {
		this(host, port, maximumTransferUnit, maxConnections, null);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param host
	 *            the IP address the server will bind to during startup. A
	 *            <code>null</code> IP address will have the server bind to the
	 *            wildcard address.
	 * @param port
	 *            the port the server will bind to during startup.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS}.
	 */
	public RakNetServer(String host, int port, int maxConnections)
			throws UnknownHostException, NullPointerException, IllegalArgumentException {
		this(host, port, AUTOMATIC_MTU, maxConnections);
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
	 *            {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer}). A
	 *            value of {@value #AUTOMATIC_MTU} will have the maximum
	 *            transfer unit be determined automatically via
	 *            {@link RakNet#getMaximumTransferUnit(InetAddress)} with the
	 *            parameter being the specified bind <code>address</code>.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
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
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
	 */
	public RakNetServer(int port, int maxConnections, Identifier identifier)
			throws NullPointerException, IllegalArgumentException {
		this(port, AUTOMATIC_MTU, maxConnections, identifier);
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
	 *            {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer}).
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS} or the maximum
	 *             transfer unit size is less than
	 *             {@value RakNet#MINIMUM_MTU_SIZE} and is not equal to
	 *             {@value #AUTOMATIC_MTU}.
	 */
	public RakNetServer(int port, int maximumTransferUnit, int maxConnections)
			throws NullPointerException, IllegalArgumentException {
		this(port, maximumTransferUnit, maxConnections, null);
	}

	/**
	 * Creates a RakNet server.
	 * 
	 * @param port
	 *            the port the server will bind to during startup.
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws NullPointerException
	 *             if the address is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the maximum connections is less than <code>0</code> and
	 *             not equal to {@value #INFINITE_CONNECTIONS}.
	 */
	public RakNetServer(int port, int maxConnections) throws NullPointerException, IllegalArgumentException {
		this(port, AUTOMATIC_MTU, maxConnections);
	}

	/**
	 * Forwards the port that the server is running on.
	 * 
	 * @return the result of the port forward attempt.
	 * @see RakNet#forwardPort(int)
	 */
	public final RakNet.UPnPResult forwardPort() {
		return RakNet.forwardPort(this.getPort());
	}

	/**
	 * Closes the port that the server is running on.
	 * 
	 * @return the result of the port close attempt.
	 * @see RakNet#closePort(int)
	 */
	public final RakNet.UPnPResult closePort() {
		return RakNet.closePort(this.getPort());
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
		return System.currentTimeMillis() - timestamp;
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
	 * Returns the server's maximum transfer unit.
	 * 
	 * @return the server's maximum transfer unit.
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
	 *            the maximum number of connections. A value of
	 *            {@value #INFINITE_CONNECTIONS} will allow for an infinite
	 *            number of connections.
	 * @throws IllegalArgumentException
	 *             if the <code>maxConnections</code> is less than
	 *             <code>0</code> and is not equal to
	 *             {@value #INFINITE_CONNECTIONS}.
	 */
	public final void setMaxConnections(int maxConnections) throws IllegalArgumentException {
		if (maxConnections < 0 && maxConnections != INFINITE_CONNECTIONS) {
			throw new IllegalArgumentException("Maximum connections must be greater than or equal to 0 or "
					+ INFINITE_CONNECTIONS + " for infinite connections");
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
		if (identifier != null) {
			log.info("Set identifier to \"" + identifier.build() + "\"");
		} else {
			log.info("Removed identifier");
		}
	}

	/**
	 * Adds a listener to the server.
	 * <p>
	 * Listeners are used to listen for events that occur relating to the server
	 * such as clients connecting to the server, receiving messages, and more.
	 * 
	 * @param listener
	 *            the listener to add.
	 * @return the server.
	 * @throws NullPointerException
	 *             if the listener is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the listener is another server that is not the server
	 *             itself.
	 */
	public final RakNetServer addListener(RakNetServerListener listener)
			throws NullPointerException, IllegalArgumentException {
		if (listener == null) {
			throw new NullPointerException("Listener cannot be null");
		} else if (listener instanceof RakNetClient && !this.equals(listener)) {
			throw new IllegalArgumentException("A server cannot be used as a listener except for itself");
		} else if (!listeners.contains(listener)) {
			listeners.add(listener);
			if (listener != this) {
				log.info("Added listener of class " + listener.getClass().getName());
			} else {
				log.info("Added self listener");
			}
		}
		return this;
	}

	/**
	 * Adds the server to its own set of listeners, used when extending the
	 * {@link RakNetServer} directly.
	 * 
	 * @return the server.
	 * @see #addListener(RakNetServerListener)
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
	 */
	public final void callEvent(Consumer<? super RakNetServerListener> event) throws NullPointerException {
		if (event == null) {
			throw new NullPointerException("Event cannot be null");
		}
		for (RakNetServerListener listener : listeners) {
			if (listener.getClass().isAnnotationPresent(ThreadedListener.class)) {
				ThreadedListener threadedListener = listener.getClass().getAnnotation(ThreadedListener.class);
				new Thread(RakNetServer.class.getSimpleName() + (threadedListener.name().length() > 0 ? "-" : "")
						+ threadedListener.name() + "-Thread-" + ++eventThreadCount) {

					@Override
					public void run() {
						event.accept(listener);
					}

				}.start();
			} else {
				event.accept(listener);
			}
		}
	}

	/**
	 * Returns the clients connected to the server.
	 * 
	 * @return the clients connected to the server.
	 */
	public final RakNetClientPeer[] getClients() {
		return clients.values().toArray(new RakNetClientPeer[clients.size()]);
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
	 * Returns whether or not a client with the specified address is currently
	 * connected to the server.
	 * 
	 * @param address
	 *            the address.
	 * @return <code>true</code> if a client with the address is connected to
	 *         the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(InetSocketAddress address) {
		if (address != null) {
			return clients.containsKey(address);
		}
		return false;
	}

	/**
	 * Returns whether or not a client with the specified address is currently
	 * connected to the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client with the address is connected to
	 *         the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(InetAddress address, int port) {
		if (port >= 0x0000 && port <= 0xFFFF) {
			return this.hasClient(new InetSocketAddress(address, port));
		}
		return false;
	}

	/**
	 * Returns whether or not a client with the specified address is currently
	 * connected to the server.
	 * 
	 * @param host
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client with the address is connected to
	 *         the server, <code>false</code> otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final boolean hasClient(String host, int port) throws UnknownHostException {
		return this.hasClient(InetAddress.getByName(host), port);
	}

	/**
	 * Returns whether or not a client with the specified IP address is
	 * currently connected to the server.
	 * 
	 * @param address
	 *            the IP address.
	 * @return <code>true</code> if a client with the IP address is connected to
	 *         the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(InetAddress address) {
		if (address != null) {
			for (InetSocketAddress clientAddress : clients.keySet()) {
				if (clientAddress.equals(address)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns whether or not a client with the specified IP address is
	 * currently connected to the server.
	 * 
	 * @param host
	 *            the IP address.
	 * @return <code>true</code> if a client with the IP address is connected to
	 *         the server, <code>false</code> otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final boolean hasClient(String host) throws UnknownHostException {
		return this.hasClient(InetAddress.getByName(host));
	}

	/**
	 * Returns whether or not a client with the specified port is currently
	 * connected to the server.
	 * 
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client with the port is connected to the
	 *         server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(int port) {
		if (port >= 0x0000 || port <= 0xFFFF) {
			for (InetSocketAddress clientAddress : clients.keySet()) {
				if (clientAddress.getPort() == port) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns whether or not the client with the specified globally unique ID
	 * is currently connected to the server.
	 * 
	 * @param guid
	 *            the globally unique ID.
	 * @return <code>true</code> if a client with the globally unique ID is
	 *         connected to the server, <code>false</code> otherwise.
	 */
	public final boolean hasClient(long guid) {
		for (RakNetClientPeer peer : clients.values()) {
			if (peer.getGloballyUniqueId() == guid) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the client with the specified address.
	 * 
	 * @param address
	 *            the address.
	 * @return the client with the address, <code>null</code> if there is none.
	 */
	public final RakNetClientPeer getClient(InetSocketAddress address) {
		return clients.get(address);
	}

	/**
	 * Returns the client with the specified address.
	 * 
	 * @param address
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return the client with the address, <code>null</code> if there is none.
	 */
	public final RakNetClientPeer getClient(InetAddress address, int port) {
		if (address != null && port > 0x0000 && port < 0xFFFF) {
			return this.getClient(new InetSocketAddress(address, port));
		}
		return null;
	}

	/**
	 * Returns the client with the specified address.
	 * 
	 * @param host
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return the client with the address, <code>null</code> if there is none.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final RakNetClientPeer getClient(String host, int port) throws UnknownHostException {
		if (host != null) {
			return this.getClient(InetAddress.getByName(host), port);
		}
		return null;
	}

	/**
	 * Returns all clients with the specified IP address.
	 * 
	 * @param host
	 *            the IP address.
	 * @return the clients with the IP address.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final RakNetClientPeer[] getClient(String host) throws UnknownHostException {
		ArrayList<RakNetClientPeer> peers = new ArrayList<RakNetClientPeer>();
		if (host != null) {
			InetAddress inetAddress = InetAddress.getByName(host);
			for (RakNetClientPeer peer : clients.values()) {
				if (peer.getInetAddress().equals(inetAddress)) {
					peers.add(peer);
				}
			}
		}
		return peers.toArray(new RakNetClientPeer[peers.size()]);
	}

	/**
	 * Returns all clients with the specified port.
	 * 
	 * @param port
	 *            the port.
	 * @return the clients with the port.
	 */
	public final RakNetClientPeer[] getClient(int port) {
		if (port < 0x0000 || port > 0xFFFF) {
			return new RakNetClientPeer[0]; // Invalid port range
		}
		ArrayList<RakNetClientPeer> peers = new ArrayList<RakNetClientPeer>();
		for (RakNetClientPeer peer : clients.values()) {
			if (peer.getPort() == port) {
				peers.add(peer);
			}
		}
		return peers.toArray(new RakNetClientPeer[peers.size()]);
	}

	/**
	 * Returns the client with the specified globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the client.
	 * @return the client with the globally unique ID, <code>null</code> if
	 *         there is none.
	 */
	public final RakNetClientPeer getClient(long guid) {
		for (RakNetClientPeer peer : clients.values()) {
			if (peer.getGloballyUniqueId() == guid) {
				return peer;
			}
		}
		return null;
	}

	/**
	 * Returns the globally unique ID of the specified peer.
	 * 
	 * @param peer
	 *            the peer.
	 * @return the globally unique ID of the specified peer, <code>-1</code> if
	 *         it does not exist.
	 * @throws NullPointerException
	 *             if the <code>peer</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 */
	public final long getGuid(RakNetClientPeer peer) throws NullPointerException, IllegalArgumentException {
		if (peer == null) {
			throw new NullPointerException("Peer cannot be null");
		}
		RakNetClientPeer clientPeer = clients.get(peer.getAddress());
		if (clientPeer != null) {
			if (clientPeer != peer) {
				throw new NullPointerException("Peer must be of the server");
			}
			return clientPeer.getGloballyUniqueId();
		}
		return -1L;
	}

	/**
	 * Sends a message to the specified peer.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packet
	 *            the packet to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>packet</code> are
	 *             <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, Packet packet)
			throws NullPointerException, IllegalArgumentException {
		if (reliability == null) {
			throw new NullPointerException("Reliability cannot be null");
		} else if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		} else if (!this.hasClient(guid)) {
			throw new IllegalArgumentException("No client with the specified GUID exists");
		}
		return this.getClient(guid).sendMessage(reliability, channel, packet);
	}

	/**
	 * Sends a message to the specified peer.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packet
	 *            the packet to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>packet</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			Packet packet) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, packet);
	}

	/**
	 * Sends messages to the specified peer.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packets.
	 * @param channel
	 *            the channel to send the packets on.
	 * @param packets
	 *            the packets to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>packets</code> are
	 *             <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, int channel, Packet... packets)
			throws NullPointerException, InvalidChannelException {
		if (packets == null) {
			throw new NullPointerException("Packets cannot be null");
		}
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[packets.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(guid, reliability, channel, packets[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends messages to the specified peer.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packets.
	 * @param channel
	 *            the channel to send the packets on.
	 * @param packets
	 *            the packets to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>packets</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			Packet... packets) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, packets);
	}

	/**
	 * Sends a message to the specified peer on the default channel.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packet
	 *            the packet to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>packet</code> are
	 *             <code>null</code>.
	 */
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, Packet packet)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, packet);
	}

	/**
	 * Sends a message to the specified peer on the default channel.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packet
	 *            the packet to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>packet</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 */
	public final EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, Packet packet)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, packet);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packets.
	 * @param packets
	 *            the packets to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>packets</code> are
	 *             <code>null</code>.
	 */
	public final EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, Packet... packets)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, packets);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packets.
	 * @param packets
	 *            the packets to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>packets</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 */
	public final EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, Packet... packets)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, packets);
	}

	/**
	 * Sends a message to the specified peer.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param buf
	 *            the buffer to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>buf</code> are
	 *             <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, ByteBuf buf)
			throws NullPointerException, InvalidChannelException {
		return this.sendMessage(guid, reliability, channel, new Packet(buf));
	}

	/**
	 * Sends a message to the specified peer.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param buf
	 *            the buffer to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>buf</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			ByteBuf buf) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, buf);
	}

	/**
	 * Sends messages to the specified peer.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packets.
	 * @param channel
	 *            the channel to send the packets on.
	 * @param bufs
	 *            the buffers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>bufs</code> are
	 *             <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, int channel, ByteBuf... bufs)
			throws NullPointerException, InvalidChannelException {
		if (bufs == null) {
			throw new NullPointerException("Buffers cannot be null");
		}
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[bufs.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(guid, reliability, channel, bufs[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends messages to the specified peer.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packets.
	 * @param channel
	 *            the channel to send the packets on.
	 * @param bufs
	 *            the buffers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>bufs</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			ByteBuf... bufs) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, bufs);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param buf
	 *            the buffer to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the inexistence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>buf</code> are
	 *             <code>null</code>.
	 */
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, ByteBuf buf)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, buf);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param buf
	 *            the buffer to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>buf</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 */
	public final EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, ByteBuf buf)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, buf);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param bufs
	 *            the buffers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>bufs</code> are
	 *             <code>null</code>.
	 */
	public final EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, ByteBuf... bufs)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, bufs);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param bufs
	 *            the buffers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>bufs</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 */
	public final EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, ByteBuf... bufs)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, bufs);
	}

	/**
	 * Sends a message identifier to the specified peer.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the message identifier.
	 * @param channel
	 *            the channel to send the message identifier on.
	 * @param packetId
	 *            the message identifier to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> is <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, int packetId)
			throws NullPointerException, InvalidChannelException {
		return this.sendMessage(guid, reliability, channel, new RakNetPacket(packetId));
	}

	/**
	 * Sends a message identifier to the specified peer.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the message identifier.
	 * @param channel
	 *            the channel to send the message identifier on.
	 * @param packetId
	 *            the message identifier to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the<code>peer</code> or <code>reliability</code> are
	 *             <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			int packetId) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, packetId);
	}

	/**
	 * Sends message identifiers to the specified peer.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the message identifiers.
	 * @param channel
	 *            the channel to send the message identifiers on.
	 * @param packetIds
	 *            the message identifiers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>packetIds</code> are
	 *             <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, int channel, int... packetIds)
			throws NullPointerException, InvalidChannelException {
		if (packetIds == null) {
			throw new NullPointerException("Packet IDs cannot be null");
		}
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[packetIds.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(guid, reliability, channel, packetIds[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends message identifiers to the specified peer.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the message identifiers.
	 * @param channel
	 *            the channel to send the message identifiers on.
	 * @param packetIds
	 *            the message identifiers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>packetIds</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 * @throws InvalidChannelException
	 *             if the channel is higher than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public final EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			int... packetIds) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, packetIds);
	}

	/**
	 * Sends a message identifier to the specified peer on the default channel.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the message identifier.
	 * @param packetId
	 *            the message identifier to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> is <code>null</code>.
	 */
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, int packetId)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, new RakNetPacket(packetId));
	}

	/**
	 * Sends a message identifier to the specified peer on the default channel.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the message identifier.
	 * @param packetId
	 *            the message identifier to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> is
	 *             <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 */
	public final EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, int packetId)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, packetId);
	}

	/**
	 * Sends message identifiers to the specified peer on the default channel.
	 * 
	 * @param guid
	 *            the globally unique ID of the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the message identifiers.
	 * @param packetIds
	 *            the message identifiers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>reliability</code> or <code>packetIds</code> are
	 *             <code>null</code>.
	 */
	public final EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, int... packetIds)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, packetIds);
	}

	/**
	 * Sends message identifiers to the specified peer on the default channel.
	 * 
	 * @param peer
	 *            the peer to send the packet to.
	 * @param reliability
	 *            the reliability of the message identifiers.
	 * @param packetIds
	 *            the message identifiers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet
	 *         was sent due to the non existence of the peer with the
	 *         <code>guid</code>. This is normally not important, however it can
	 *         be used for packet acknowledged and not acknowledged events if
	 *         the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException
	 *             if the <code>peer</code>, <code>reliability</code> or
	 *             <code>packetIds</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>peer</code> is not of the server.
	 */
	public final EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, int... packetIds)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, packetIds);
	}

	/**
	 * Returns whether or not the specified client IP address is banned.
	 * 
	 * @param address
	 *            the IP address.
	 * @return <code>true</code> if the client address is banned,
	 *         <code>false</code> otherwise.
	 */
	public final boolean isClientBanned(InetAddress address) {
		return banned.contains(address);
	}

	/**
	 * Returns whether or not the specified client IP address is banned.
	 * 
	 * @param host
	 *            the IP address.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 * @return <code>true</code> if the client address is banned,
	 *         <code>false</code> otherwise.
	 */
	public final boolean isClientBanned(String host) throws UnknownHostException {
		return banned.contains(InetAddress.getByName(host));
	}

	/**
	 * Bans the specified client IP address.
	 * 
	 * @param address
	 *            the IP address to ban.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public final void ban(InetAddress address) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		} else if (!banned.contains(address)) {
			banned.add(address);
		}
	}

	/**
	 * Bans the specified client IP address.
	 * 
	 * @param host
	 *            the IP address to ban.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final void ban(String host) throws NullPointerException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		this.ban(InetAddress.getByName(host));
	}

	/**
	 * Unbans the specified client IP address.
	 * 
	 * @param address
	 *            the IP address to unban.
	 */
	public final void unban(InetAddress address) {
		banned.remove(address);
	}

	/**
	 * Unbans the specified client IP address.
	 * 
	 * @param host
	 *            the IP address to unban.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final void unban(String host) throws UnknownHostException {
		if (host != null) {
			this.unban(InetAddress.getByName(host));
		}
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
	 * @return <code>true</code> if a client was disconnected,
	 *         <code>false</code> otherwise.
	 */
	public final boolean disconnect(InetSocketAddress address, String reason) {
		RakNetClientPeer peer = clients.remove(address);
		if (peer == null) {
			return false; // No client to disconnect
		}
		peer.disconnect();
		log.debug("Disconnected client with address " + address + " for \"" + (reason == null ? "Disconnected" : reason)
				+ "\"");
		this.callEvent(
				listener -> listener.onDisconnect(this, address, peer, reason == null ? "Disconnected" : reason));
		return true;
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
	 * @return <code>true</code> if a client was disconnected,
	 *         <code>false</code> otherwise.
	 */
	public final boolean disconnect(InetSocketAddress address, Throwable reason) {
		return this.disconnect(address, reason == null ? null : RakNet.getStackTrace(reason));
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the address of the client.
	 * @return <code>true</code> if a client was disconnected,
	 *         <code>false</code> otherwise.
	 */
	public final boolean disconnect(InetSocketAddress address) {
		return this.disconnect(address, (String) /* Solves ambiguity */ null);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the IP address of the client.
	 * @param port
	 *            the port.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnect(InetAddress address, int port, String reason) {
		if (port < 0x0000 || port > 0xFFFF) {
			return false; // Invalid port range
		}
		return this.disconnect(new InetSocketAddress(address, port), reason);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the IP address of the client.
	 * @param port
	 *            the port.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnect(InetAddress address, int port, Throwable reason) {
		if (port < 0x0000 || port > 0xFFFF) {
			return false; // Invalid port range
		}
		return this.disconnect(new InetSocketAddress(address, port), reason);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param address
	 *            the IP address of the client.
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnect(InetAddress address, int port) {
		return this.disconnect(address, port, (String) /* Solves ambiguity */ null);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param host
	 *            the IP address of the client.
	 * @param port
	 *            the port.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnect(String host, int port, String reason) throws UnknownHostException {
		if (host == null) {
			return false;
		}
		return this.disconnect(InetAddress.getByName(host), port, reason);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param host
	 *            the IP address of the client.
	 * @param port
	 *            the port.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnect(String host, int port, Throwable reason) throws UnknownHostException {
		if (host == null) {
			return false;
		}
		return this.disconnect(InetAddress.getByName(host), port, reason);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param host
	 *            the IP address of the client.
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnect(String host, int port) throws UnknownHostException {
		return this.disconnect(host, port, (String) /* Solves ambiguity */ null);
	}

	/**
	 * Disconnects all clients from the server with the address.
	 * 
	 * @param address
	 *            the IP address of the client.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnect(InetAddress address, String reason) {
		if (address == null) {
			return false;
		}
		boolean disconnected = false;
		for (InetSocketAddress peerAddress : clients.keySet()) {
			if (address.equals(peerAddress)) {
				this.disconnect(peerAddress, reason);
				disconnected = true;
			}
		}
		return disconnected;
	}

	/**
	 * Disconnects all clients from the server with the IP address.
	 * 
	 * @param address
	 *            the IP address of the client.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnect(InetAddress address) {
		return this.disconnect(address, null);
	}

	/**
	 * Disconnects all clients from the server with the IP address.
	 * 
	 * @param host
	 *            the IP address of the client.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnect(String host, String reason) throws UnknownHostException {
		if (host == null) {
			return false;
		}
		return this.disconnect(InetAddress.getByName(host), reason);
	}

	/**
	 * Disconnects all clients from the server with the IP address.
	 * 
	 * @param host
	 *            the IP address of the client.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code>
	 *            reason will have <code>"Disconnected"</code> be used as the
	 *            reason instead.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnect(String host, Throwable reason) throws UnknownHostException {
		return this.disconnect(host, reason == null ? null : RakNet.getStackTrace(reason));
	}

	/**
	 * Disconnects all clients from the server with the IP address.
	 * 
	 * @param host
	 *            the IP address of the client.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public final boolean disconnect(String host) throws UnknownHostException {
		return this.disconnect(host, (String) /* Solves ambiguity */ null);
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
	public final boolean disconnect(int port, String reason) {
		if (port < 0x0000 || port > 0xFFFF) {
			return false; // Invalid port range
		}
		boolean disconnected = false;
		for (InetSocketAddress address : clients.keySet()) {
			if (address.getPort() == port) {
				this.disconnect(address, reason == null ? "Disconnected" : reason);
				disconnected = true;
			}
		}
		return disconnected;
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
	public final boolean disconnect(int port, Throwable reason) {
		return this.disconnect(port, reason == null ? null : RakNet.getStackTrace(reason));
	}

	/**
	 * Disconnects all clients from the server with the port.
	 * 
	 * @param port
	 *            the port.
	 * @return <code>true</code> if a client was disconnect, <code>false</code>
	 *         otherwise.
	 */
	public final boolean disconnect(int port) {
		return this.disconnect(port, (String) /* Solves ambiguity */ null);
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param peer
	 *            the peer of the client to disconnect.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code> value
	 *            will have <code>"Disconnected"</code> be used as the reason
	 *            instead.
	 * @return <code>true</code> if a client was disconnected,
	 *         <code>false</code> otherwise.
	 * @throws IllegalArgumentException
	 *             if the given peer is fabricated, meaning that the peer is not
	 *             one created by the server but rather one created externally.
	 */
	public final boolean disconnect(RakNetClientPeer peer, String reason) throws IllegalArgumentException {
		if (peer != null) {
			if (peer.getServer() != this) {
				throw new IllegalArgumentException("Peer must belong to the server");
			}
			return this.disconnect(peer.getAddress(), reason);
		}
		return false;
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param peer
	 *            the peer of the client to disconnect.
	 * @param reason
	 *            the reason for client disconnection. A <code>null</code> value
	 *            will have <code>"Disconnected"</code> be used as the reason
	 *            instead.
	 * @return <code>true</code> if a client was disconnected,
	 *         <code>false</code> otherwise.
	 * @throws IllegalArgumentException
	 *             if the given peer is fabricated, meaning that the peer is not
	 *             one created by the server but rather one created externally.
	 */
	public final boolean disconnect(RakNetClientPeer peer, Throwable reason) throws IllegalArgumentException {
		return this.disconnect(peer, reason == null ? null : RakNet.getStackTrace(reason));
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param peer
	 *            the peer of the client to disconnect.
	 * @return <code>true</code> if a client was disconnected,
	 *         <code>false</code> otherwise.
	 * @throws IllegalArgumentException
	 *             if the given peer is fabricated, meaning that the peer is not
	 *             one created by the server but rather one created externally.
	 */
	public final boolean disconnect(RakNetClientPeer peer) {
		return this.disconnect(peer, (String) /* Solves ambiguity */ null);
	}

	/**
	 * Returns whether or not the specified IP address is blocked.
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
	 * Blocks the specified IP address.
	 * <p>
	 * All currently connected clients with the IP address (regardless of port)
	 * will be disconnected with the same reason that the IP address was
	 * blocked.
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
	public final void blockAddress(InetAddress address, String reason, long time) throws NullPointerException {
		handler.blockAddress(address, reason, time);
	}

	/**
	 * Blocks the specified IP address.
	 * <p>
	 * All currently connected clients with the IP address (regardless of port)
	 * will be disconnected with the same reason that the IP address was
	 * blocked.
	 * 
	 * @param address
	 *            the IP address to block.
	 * @param time
	 *            how long the address will blocked in milliseconds.
	 * @throws NullPointerException
	 *             if <code>address</code> is <code>null</code>.
	 */
	public final void blockAddress(InetAddress address, long time) throws NullPointerException {
		this.blockAddress(address, null, time);
	}

	/**
	 * Unblocks the specified IP address.
	 * 
	 * @param address
	 *            the IP address to unblock.
	 */
	public final void unblockAddress(InetAddress address) {
		handler.unblockAddress(address);
	}

	/**
	 * Called by the {@link RakNetServerHandler} when it catches a
	 * <code>Throwable</code> while handling a packet.
	 * 
	 * @param address
	 *            the address that caused the exception.
	 * @param cause
	 *            the <code>Throwable</code> caught by the handler.
	 * @throws NullPointerException
	 *             if the cause <code>address</code> or <code>cause</code> are
	 *             <code>null</code>.
	 */
	protected final void handleHandlerException(InetSocketAddress address, Throwable cause)
			throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (cause == null) {
			throw new NullPointerException("Cause cannot be null");
		} else if (this.hasClient(address)) {
			this.disconnect(address, RakNet.getStackTrace(cause));
		}
		log.warn("Handled exception " + cause.getClass().getName() + " caused by address " + address);
		this.callEvent(listener -> listener.onHandlerException(this, address, cause));
	}

	/**
	 * Handles a packet received by the {@link RakNetServerHandler}.
	 * 
	 * @param sender
	 *            the address of the sender.
	 * @param packet
	 *            the packet to handle.
	 * @throws NullPointerException
	 *             if the <code>sender</code> or <code>packet</code> are
	 *             <code>null</code>.
	 */
	protected final void handleMessage(InetSocketAddress sender, RakNetPacket packet) throws NullPointerException {
		if (sender == null) {
			throw new NullPointerException("Sender cannot be null");
		} else if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		} else if (clients.containsKey(sender)) {
			clients.get(sender).handleInternal(packet);
		} else if (packet.getId() == RakNetPacket.ID_UNCONNECTED_PING
				|| packet.getId() == RakNetPacket.ID_UNCONNECTED_PING_OPEN_CONNECTIONS) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();
			if (!ping.failed()
					&& (packet.getId() == RakNetPacket.ID_UNCONNECTED_PING
							|| (clients.size() < maxConnections || maxConnections < 0))
					&& broadcastingEnabled == true && ping.magic == true) {
				ServerPing pingEvent = new ServerPing(sender, ping.connectionType, identifier);
				this.callEvent(listener -> listener.onPing(this, pingEvent));
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
		} else if (packet.getId() == RakNetPacket.ID_OPEN_CONNECTION_REQUEST_1) {
			OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne(packet);
			connectionRequestOne.decode();
			if (clients.containsKey(sender)) {
				if (clients.get(sender).isLoggedIn()) {
					this.disconnect(sender, "Client reinstantiated connection");
				}
			}
			if (connectionRequestOne.magic == true) {
				RakNetPacket errorPacket = this.validateSender(sender, NO_GUID);
				if (errorPacket == null) {
					if (connectionRequestOne.networkProtocol != this.getProtocolVersion()) {
						IncompatibleProtocolVersion incompatibleProtocol = new IncompatibleProtocolVersion();
						incompatibleProtocol.networkProtocol = this.getProtocolVersion();
						incompatibleProtocol.serverGuid = this.guid;
						incompatibleProtocol.encode();
						this.sendNettyMessage(incompatibleProtocol, sender);
					} else {
						if (connectionRequestOne.maximumTransferUnit <= maximumTransferUnit) {
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
		} else if (packet.getId() == RakNetPacket.ID_OPEN_CONNECTION_REQUEST_2) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo(packet);
			connectionRequestTwo.decode();
			if (!connectionRequestTwo.failed() && connectionRequestTwo.magic == true) {
				RakNetPacket errorPacket = this.validateSender(sender, connectionRequestTwo.clientGuid);
				if (errorPacket == null) {
					if (connectionRequestTwo.maximumTransferUnit <= maximumTransferUnit) {
						OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo();
						connectionResponseTwo.serverGuid = this.guid;
						connectionResponseTwo.maximumTransferUnit = connectionRequestTwo.maximumTransferUnit;
						connectionResponseTwo.encode();
						if (!connectionResponseTwo.failed()) {
							this.callEvent(
									listener -> listener.onConnect(this, sender, connectionRequestTwo.connectionType));
							clients.put(sender,
									new RakNetClientPeer(this, connectionRequestTwo.connectionType,
											connectionRequestTwo.clientGuid, connectionRequestTwo.maximumTransferUnit,
											channel, sender));
							this.sendNettyMessage(connectionResponseTwo, sender);
						}
					}
				} else {
					this.sendNettyMessage(errorPacket, sender);
				}
			}
		}
		log.debug("Handled " + RakNetPacket.getName(packet.getId()) + " packet");
	}

	/**
	 * Validates the sender of a packet.
	 * <p>
	 * This is called throughout initial client connection to make sure there
	 * are no issues.
	 * 
	 * @param sender
	 *            the address of the packet sender.
	 * @param guid
	 *            the globally unique ID of the sender, {@value #NO_GUID} if
	 *            there is none.
	 * @return the packet to respond with if there was an error,
	 *         <code>null</code> if there are no issues.
	 * @throws NullPointerException
	 *             if the <code>sender</code> is <code>null</code>.
	 */
	private final RakNetPacket validateSender(InetSocketAddress sender, long guid) throws NullPointerException {
		if (sender == null) {
			throw new NullPointerException("Sender cannot be null");
		} else if (this.hasClient(sender) || (this.hasClient(guid) && guid != NO_GUID)) {
			return new RakNetPacket(RakNetPacket.ID_ALREADY_CONNECTED);
		} else if (this.getClientCount() >= maxConnections && maxConnections >= 0) {
			return new RakNetPacket(RakNetPacket.ID_NO_FREE_INCOMING_CONNECTIONS);
		} else if (this.isClientBanned(sender.getAddress())) {
			ConnectionBanned connectionBanned = new ConnectionBanned();
			connectionBanned.serverGuid = guid;
			connectionBanned.encode();
			return connectionBanned;
		}
		return null;
	}

	/**
	 * Sends a Netty message over the channel raw.
	 * <p>
	 * This should be used sparingly, as if it is used incorrectly it could
	 * break client peers entirely. In order to send a message to a peer, use
	 * one of the
	 * {@link com.whirvis.jraknet.peer.RakNetPeer#sendMessage(com.whirvis.jraknet.protocol.Reliability, io.netty.buffer.ByteBuf)
	 * sendMessage()} methods.
	 * 
	 * @param buf
	 *            the buffer to send.
	 * @param address
	 *            the address to send the buffer to.
	 * @throws NullPointerException
	 *             if the <code>buf</code>, <code>address</code>, or IP address
	 *             of the <code>address</code> are <code>null</code>.
	 */
	public final void sendNettyMessage(ByteBuf buf, InetSocketAddress address) throws NullPointerException {
		if (buf == null) {
			throw new NullPointerException("Buffer cannot be null");
		} else if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		channel.writeAndFlush(new DatagramPacket(buf, address));
		log.debug("Sent netty message with size of " + buf.capacity() + " bytes (" + (buf.capacity() * 8) + " bits) to "
				+ address);
	}

	/**
	 * Sends a Netty message over the channel raw.
	 * <p>
	 * This should be used sparingly, as if it is used incorrectly it could
	 * break client peers entirely. In order to send a message to a peer, use
	 * one of the
	 * {@link com.whirvis.jraknet.peer.RakNetPeer#sendMessage(Reliability, Packet)
	 * sendMessage()} methods.
	 * 
	 * @param packet
	 *            the packet to send.
	 * @param address
	 *            the address to send the packet to.
	 * @throws NullPointerException
	 *             if the <code>packet</code>, <code>address</code>, or IP
	 *             address of the <code>address</code> are <code>null</code>.
	 */
	public final void sendNettyMessage(Packet packet, InetSocketAddress address) throws NullPointerException {
		if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		}
		this.sendNettyMessage(packet.buffer(), address);
	}

	/**
	 * Sends a Netty message over the channel raw.
	 * <p>
	 * This should be used sparingly, as if it is used incorrectly it could
	 * break client peers entirely. In order to send a message to a peer, use
	 * one of the
	 * {@link com.whirvis.jraknet.peer.RakNetPeer#sendMessage(com.whirvis.jraknet.protocol.Reliability, int)
	 * sendMessage()} methods.
	 * 
	 * @param packetId
	 *            the packet ID to send.
	 * @param address
	 *            the address to send the packet to.
	 * @throws NullPointerException
	 *             if the <code>address</code> or IP address of the
	 *             <code>address</code> are <code>null</code>.
	 */
	public final void sendNettyMessage(int packetId, InetSocketAddress address) throws NullPointerException {
		this.sendNettyMessage(new RakNetPacket(packetId), address);
	}

	/**
	 * Returns whether or not the server is running.
	 * 
	 * @return <code>true</code> if the server is running, <code>false</code>
	 *         otherwise.
	 */
	public final boolean isRunning() {
		return this.running;
	}

	/**
	 * Starts the server.
	 * 
	 * @throws IllegalStateException
	 *             if the server is already running.
	 * @throws RakNetException
	 *             if an error occurs during startup.
	 */
	public void start() throws IllegalStateException, RakNetException {
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
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false)
					.option(ChannelOption.SO_SNDBUF, maximumTransferUnit)
					.option(ChannelOption.SO_RCVBUF, maximumTransferUnit);
			this.channel = (bindingAddress != null ? bootstrap.bind(bindingAddress) : bootstrap.bind(0)).sync()
					.channel();
			this.bindAddress = (InetSocketAddress) channel.localAddress();
			this.running = true;
			log.debug("Created and bound bootstrap");

			// Create and start peer update thread
			RakNetServer server = this;
			this.peerThread = new Thread(
					RakNetServer.class.getSimpleName() + "-Peer-Thread-" + Long.toHexString(guid).toUpperCase()) {

				@Override
				public void run() {
					HashMap<RakNetClientPeer, Throwable> disconnected = new HashMap<RakNetClientPeer, Throwable>();
					while (server.running == true && !this.isInterrupted()) {
						try {
							Thread.sleep(0, 1); // Lower CPU usage
						} catch (InterruptedException e) {
							this.interrupt(); // Interrupted during sleep
							continue;
						}
						for (RakNetClientPeer peer : clients.values()) {
							if (!peer.isDisconnected()) {
								try {
									peer.update();
									if (peer.getPacketsReceivedThisSecond() >= RakNet.getMaxPacketsPerSecond()) {
										server.blockAddress(peer.getInetAddress(), "Too many packets",
												RakNet.MAX_PACKETS_PER_SECOND_BLOCK);
									}
								} catch (Throwable throwable) {
									server.callEvent(listener -> listener.onPeerException(server, peer, throwable));
									disconnected.put(peer, throwable);
								}
							}
						}

						/*
						 * Disconnect peers.
						 * 
						 * This must be done here as simply removing a client
						 * from the clients map would be an incorrect way of
						 * disconnecting a client. This means that calling the
						 * disconnect() method is required. However, calling it
						 * while in the loop would cause a
						 * ConcurrentModifactionException. To get around this,
						 * the clients that need to be disconnected are properly
						 * disconnected after the loop is finished. This is done
						 * simply by having them and their disconnect reason be
						 * put in a disconnection map.
						 */
						if (disconnected.size() > 0) {
							for (RakNetClientPeer peer : disconnected.keySet()) {
								server.disconnect(peer, disconnected.get(peer));
							}
							disconnected.clear();
						}
					}
				}

			};
			peerThread.start();
			log.debug("Created and started peer update thread");
			this.callEvent(listener -> listener.onStart(this));
		} catch (InterruptedException e) {
			this.running = false;
			throw new RakNetException(e);
		}
		log.info("Started server");
	}

	/**
	 * Stops the server.
	 * <p>
	 * All currently connected clients will be disconnected with the same reason
	 * used for shutdown.
	 * 
	 * @param reason
	 *            the reason for shutdown. A <code>null</code> reason will have
	 *            <code>"Server shutdown"</code> be used as the reason instead.
	 * @throws IllegalStateException
	 *             if the server is not running.
	 */
	public void shutdown(String reason) throws IllegalStateException {
		if (running == false) {
			throw new IllegalStateException("Server is not running");
		}

		// Disconnect clients
		for (RakNetClientPeer client : clients.values()) {
			this.disconnect(client, reason == null ? "Server shutdown" : reason);
		}
		clients.clear();

		// Stop server
		this.running = false;
		peerThread.interrupt();
		log.info("Shutdown server");

		// Shutdown networking
		channel.close();
		group.shutdownGracefully(0L, 1000L, TimeUnit.MILLISECONDS);
		this.channel = null;
		this.handler = null;
		this.group = null;
		this.bootstrap = null;
		log.debug("Shutdown networking");
		this.callEvent(listener -> listener.onShutdown(this));
	}

	/**
	 * Stops the server.
	 * <p>
	 * All currently connected clients will be disconnected with the same reason
	 * used for shutdown.
	 * 
	 * @param reason
	 *            the reason for shutdown. A <code>null</code> reason will have
	 *            <code>"Server shutdown"</code> be used as the reason instead.
	 * @throws IllegalStateException
	 *             if the server is not running.
	 */
	public final void shutdown(Throwable reason) throws IllegalStateException {
		this.shutdown(reason != null ? RakNet.getStackTrace(reason) : null);
	}

	/**
	 * Stops the server.
	 * <p>
	 * All currently connected clients will be disconnected with the same reason
	 * used for shutdown.
	 * 
	 * @throws IllegalStateException
	 *             if the server is not running.
	 */
	public final void shutdown() throws IllegalStateException {
		this.shutdown((String) /* Solves ambiguity */ null);
	}

	@Override
	public String toString() {
		return "RakNetServer [bindingAddress=" + bindingAddress + ", guid=" + guid + ", log=" + log + ", pongId="
				+ pongId + ", timestamp=" + timestamp + ", maximumTransferUnit=" + maximumTransferUnit
				+ ", maxConnections=" + maxConnections + ", broadcastingEnabled=" + broadcastingEnabled
				+ ", identifier=" + identifier + ", bindAddress=" + bindAddress + ", running=" + running
				+ ", getProtocolVersion()=" + getProtocolVersion() + ", getTimestamp()=" + getTimestamp()
				+ ", getAddress()=" + getAddress() + ", getClientCount()=" + getClientCount() + "]";
	}

}
