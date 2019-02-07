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
 * Copyright (c) 2016-2019 Trent Summerlin
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
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
import com.whirvis.jraknet.client.discovery.DiscoveredServer;
import com.whirvis.jraknet.client.discovery.DiscoveryMode;
import com.whirvis.jraknet.client.discovery.DiscoveryThread;
import com.whirvis.jraknet.protocol.MessageIdentifier;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.login.ConnectionRequest;
import com.whirvis.jraknet.protocol.login.OpenConnectionRequestOne;
import com.whirvis.jraknet.protocol.login.OpenConnectionRequestTwo;
import com.whirvis.jraknet.protocol.message.CustomPacket;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Acknowledge;
import com.whirvis.jraknet.protocol.status.UnconnectedPing;
import com.whirvis.jraknet.protocol.status.UnconnectedPingOpenConnections;
import com.whirvis.jraknet.protocol.status.UnconnectedPong;
import com.whirvis.jraknet.session.RakNetServerSession;
import com.whirvis.jraknet.session.RakNetState;
import com.whirvis.jraknet.session.UnumRakNetPeer;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Used to connect to servers using the RakNet protocol.
 *
 * @author Trent Summerlin
 * @since JRakNet v1.0
 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
 */
public class RakNetClient implements UnumRakNetPeer, RakNetClientListener {

	private static final Logger LOG = LogManager.getLogger(RakNetClient.class);
	private static final int[] DEFAULT_TRANSFER_UNITS = new int[] { 1492, 1200, 576, RakNet.MINIMUM_MTU_SIZE };
	private static final long PING_BROADCAST_WAIT_MILLIS = 1000L;
	private static DiscoveryThread discoverySystem = new DiscoveryThread();

	private final long guid;
	private final long pingId;
	private final long timestamp;
	private final ConcurrentLinkedQueue<RakNetClientListener> listeners;
	private HashSet<Integer> discoveryPorts;
	private DiscoveryMode discoveryMode;
	/** Synchronize this first! (<code>externalServers</code> goes second!) */
	private final ConcurrentHashMap<InetSocketAddress, DiscoveredServer> discovered;
	/** Synchronize this second! (<code>discovered</code> goes first!) */
	private final ConcurrentHashMap<InetSocketAddress, DiscoveredServer> externalServers;
	private long lastPingBroadcast;
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetClientHandler handler;
	private final Channel channel;
	private final InetSocketAddress bindAddress;
	private MaximumTransferUnit[] maximumTransferUnits;
	private int maximumMaximumTransferUnitSize;
	private SessionPreparation preparation;
	private volatile RakNetServerSession session;
	private Thread clientThread;

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address along with the client giving Netty the
	 *            responsibility of choosing which port to bind to.
	 * @param discoveryMode
	 *            the discovery mode which will determine how server discovery
	 *            is handled.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. A <code>null</code>
	 *            discovery mode means the discovery ports will determine the
	 *            discovery mode. If the amount of discovery ports is greater
	 *            than zero, then the discovery mode will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(InetSocketAddress bindAddress, DiscoveryMode discoveryMode, int... discoveryPorts)
			throws RakNetException {
		// Generate client information
		UUID uuid = UUID.randomUUID();
		this.guid = uuid.getMostSignificantBits();
		this.pingId = uuid.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();
		this.listeners = new ConcurrentLinkedQueue<RakNetClientListener>();

		// Prepare discovery system
		this.discoveryPorts = new HashSet<Integer>();
		this.discoveryMode = discoveryMode != null ? discoveryMode
				: (discoveryPorts.length > 0 ? DiscoveryMode.ALL_CONNECTIONS : DiscoveryMode.DISABLED);
		this.discovered = new ConcurrentHashMap<InetSocketAddress, DiscoveredServer>();
		this.externalServers = new ConcurrentHashMap<InetSocketAddress, DiscoveredServer>();
		this.setDiscoveryPorts(discoveryPorts);

		// Initiate networking
		try {
			this.bootstrap = new Bootstrap();
			this.group = new NioEventLoopGroup();
			this.handler = new RakNetClientHandler(this);
			bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = (bindAddress != null ? bootstrap.bind(bindAddress) : bootstrap.bind(0)).sync().channel();
			this.bindAddress = (InetSocketAddress) channel.localAddress();
			this.setMaximumTransferUnitSizes(DEFAULT_TRANSFER_UNITS);
			LOG.debug("Created and bound bootstrap");
		} catch (InterruptedException e) {
			throw new RakNetException(e);
		}
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address along with the client giving Netty the
	 *            responsibility of choosing which port to bind to.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. If the amount of
	 *            discovery ports is greater than zero, then the discovery mode
	 *            will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(InetSocketAddress bindAddress, int... discoveryPorts) throws RakNetException {
		this(bindAddress, null, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the IP address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param bindPort
	 *            the port the client will bind to during creation. A port of
	 *            <code>zero</code> will have the client give Netty the
	 *            respsonsibility of choosing the port to bind to.
	 * @param discoveryMode
	 *            the discovery mode which will determine how server discovery
	 *            is handled.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. A <code>null</code>
	 *            discovery mode means the discovery ports will determine the
	 *            discovery mode. If the amount of discovery ports is greater
	 *            than zero, then the discovery mode will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(InetAddress bindAddress, int bindPort, DiscoveryMode discoveryMode, int... discoveryPorts)
			throws RakNetException {
		this(new InetSocketAddress(bindAddress, bindPort), discoveryMode, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the IP address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param discoveryMode
	 *            the discovery mode which will determine how server discovery
	 *            is handled.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. A <code>null</code>
	 *            discovery mode means the discovery ports will determine the
	 *            discovery mode. If the amount of discovery ports is greater
	 *            than zero, then the discovery mode will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(InetAddress bindAddress, DiscoveryMode discoveryMode, int... discoveryPorts)
			throws RakNetException {
		this(new InetSocketAddress(bindAddress, 0), discoveryMode, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the IP address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param bindPort
	 *            the port the client will bind to during creation. A port of
	 *            <code>zero</code> will have the client give Netty the
	 *            respsonsibility of choosing the port to bind to.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. If the amount of
	 *            discovery ports is greater than zero, then the discovery mode
	 *            will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(InetAddress bindAddress, int bindPort, int... discoveryPorts) throws RakNetException {
		this(bindAddress, bindPort, null, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the IP address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. If the amount of
	 *            discovery ports is greater than zero, then the discovery mode
	 *            will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(InetAddress bindAddress, int... discoveryPorts) throws RakNetException {
		this(bindAddress, 0, null, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the IP address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param discoveryMode
	 *            the discovery mode which will determine how server discovery
	 *            is handled.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. A <code>null</code>
	 *            discovery mode means the discovery ports will determine the
	 *            discovery mode. If the amount of discovery ports is greater
	 *            than zero, then the discovery mode will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>bindAddress</code> could be
	 *             found, or if a scope_id was specified for a global IPv6
	 *             address.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(String bindAddress, int bindPort, DiscoveryMode discoveryMode, int... discoveryPorts)
			throws UnknownHostException, RakNetException {
		this(InetAddress.getByName(bindAddress), bindPort, discoveryMode, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the IP address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param discoveryMode
	 *            the discovery mode which will determine how server discovery
	 *            is handled.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. A <code>null</code>
	 *            discovery mode means the discovery ports will determine the
	 *            discovery mode. If the amount of discovery ports is greater
	 *            than zero, then the discovery mode will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>bindAddress</code> could be
	 *             found, or if a scope_id was specified for a global IPv6
	 *             address.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(String bindAddress, DiscoveryMode discoveryMode, int... discoveryPorts)
			throws UnknownHostException, RakNetException {
		this(bindAddress, 0, discoveryMode, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the IP address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param bindPort
	 *            the port the client will bind to during creation. A port of
	 *            <code>zero</code> will have the client give Netty the
	 *            respsonsibility of choosing the port to bind to.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. If the amount of
	 *            discovery ports is greater than zero, then the discovery mode
	 *            will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>bindAddress</code> could be
	 *             found, or if a scope_id was specified for a global IPv6
	 *             address.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(String bindAddress, int bindPort, int... discoveryPorts)
			throws UnknownHostException, RakNetException {
		this(bindAddress, bindPort, null, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindAddress
	 *            the IP address the client will bind to during creation. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. If the amount of
	 *            discovery ports is greater than zero, then the discovery mode
	 *            will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>bindAddress</code> could be
	 *             found, or if a scope_id was specified for a global IPv6
	 *             address.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(String bindAddress, int... discoveryPorts) throws UnknownHostException, RakNetException {
		this(bindAddress, 0, null, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindPort
	 *            the port the client will bind to during creation. A port of
	 *            <code>zero</code> will have the client give Netty the
	 *            respsonsibility of choosing the port to bind to.
	 * @param discoveryMode
	 *            the discovery mode which will determine how server discovery
	 *            is handled.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. A <code>null</code>
	 *            discovery mode means the discovery ports will determine the
	 *            discovery mode. If the amount of discovery ports is greater
	 *            than zero, then the discovery mode will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(int bindPort, DiscoveryMode discoveryMode, int... discoveryPorts) throws RakNetException {
		this(new InetSocketAddress(bindPort), discoveryMode, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param bindPort
	 *            the port the client will bind to during creation. A port of
	 *            <code>zero</code> will have the client give Netty the
	 *            respsonsibility of choosing the port to bind to.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. A <code>null</code>
	 *            discovery mode means the discovery ports will determine the
	 *            discovery mode. If the amount of discovery ports is greater
	 *            than zero, then the discovery mode will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(int bindPort, int... discoveryPorts) throws RakNetException {
		this(bindPort, null, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param discoveryMode
	 *            the discovery mode which will determine how server discovery
	 *            is handled.
	 * @param discoveryPorts
	 *            the ports on which to discover servers. A <code>null</code>
	 *            discovery mode means the discovery ports will determine the
	 *            discovery mode. If the amount of discovery ports is greater
	 *            than zero, then the discovery mode will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(DiscoveryMode discoveryMode, int... discoveryPorts) throws RakNetException {
		this((InetSocketAddress) null, discoveryMode, discoveryPorts);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param discoveryPorts
	 *            the ports on which to discover servers. If the amount of
	 *            discovery ports is greater than zero, then the discovery mode
	 *            will be set to
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#ALL_CONNECTIONS
	 *            ALL_CONNECTIONS},
	 *            {@link com.whirvis.jraknet.client.discovery.DiscoveryMode#DISABLED
	 *            DISABLED} otherwise.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public RakNetClient(int... discoveryPorts) throws RakNetException {
		this((DiscoveryMode) null, discoveryPorts);
	}

	/**
	 * Returns the client's networking protocol version.
	 * 
	 * @return the client's networking protocol version.
	 */
	public final int getProtocolVersion() {
		return RakNet.CLIENT_NETWORK_PROTOCOL;
	}

	/**
	 * Returns the client's globally unique ID.
	 * 
	 * @return the client's globally unique ID.
	 */
	public final long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * Returns the client's ping ID.
	 * 
	 * @return the client's ping ID.
	 */
	public final long getPingId() {
		return this.pingId;
	}

	/**
	 * Returns the client's timestamp.
	 * 
	 * @return the client's timestamp.
	 */
	public final long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	/**
	 * Returns the client's listeners.
	 * 
	 * @return the client's listeners.
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	public final RakNetClientListener[] getListeners() {
		return listeners.toArray(new RakNetClientListener[listeners.size()]);
	}

	/**
	 * Adds a listener to the client. Listeners are used to listen for events
	 * that occur relating to the client such as connecting to discovers,
	 * discovering local servers, and more.
	 * 
	 * @param listener
	 *            the listener to add.
	 * @return the client.
	 * @throws NullPointerException
	 *             if the listener is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the listener is another client that is not the client
	 *             itself.
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	public final RakNetClient addListener(RakNetClientListener listener)
			throws NullPointerException, IllegalArgumentException {
		// Validate listener
		if (listener == null) {
			throw new NullPointerException("Listener must not be null");
		} else if (listeners.contains(listener)) {
			return this; // Prevent duplicates
		} else if (listener instanceof RakNetClient && !listener.equals(this)) {
			throw new IllegalArgumentException("A client cannot be used as a listener except for itself");
		}

		// Add listener
		listeners.add(listener);
		LOG.info("Added listener " + listener.getClass().getName());

		// Initiate discovery system if it is not yet started
		if (discoverySystem.isRunning() == false) {
			discoverySystem.start();
		}
		discoverySystem.addClient(this);
		return this;
	}

	/**
	 * Adds the client to its own set of listeners, used when extending the
	 * <code>RakNetClient</code> directly.
	 * 
	 * @return the client.
	 * @see #addListener(RakNetClientListener)
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	public final RakNetClient addSelfListener() {
		this.addListener(this);
		return this;
	}

	/**
	 * Removes a listener from the client.
	 * 
	 * @param listener
	 *            the listener to remove.
	 * @return the client.
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	public final RakNetClient removeListener(RakNetClientListener listener) {
		if (listeners.remove(listener)) {
			LOG.info("Removed listener " + listener.getClass().getName());
		} else {
			LOG.warn("Attempted to removed unregistered listener " + listener.getClass().getName());
		}
		return this;
	}

	/**
	 * Removes the client from its own set of listeners, used when extending the
	 * <code>RakNetClient</code> directly.
	 * 
	 * @return the client.
	 * @see #removeListener(RakNetClientListener)
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	public final RakNetClient removeSelfListener() {
		this.removeListener(this);
		return this;
	}

	/**
	 * Calls the event.
	 * 
	 * @param event
	 *            the event to call.
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	private final void callEvent(Consumer<? super RakNetClientListener> event) {
		listeners.forEach(event);
	}

	/**
	 * Returns the client's discovery ports.
	 * 
	 * @return the client's discovery ports.
	 */
	public final int[] getDiscoveryPorts() {
		return discoveryPorts.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Sets the client's discovery ports.
	 * 
	 * @param discoveryPorts
	 *            the new discovery ports.
	 * @throws IllegalArgumentException
	 *             if one of the discovery ports is not within the range of
	 *             <code>0-65535</code>.
	 */
	public final void setDiscoveryPorts(int... discoveryPorts) throws IllegalArgumentException {
		// Make a new set to prevent duplicates
		HashSet<Integer> discoverySet = new HashSet<Integer>();
		for (int discoveryPort : discoveryPorts) {
			if (discoveryPort < 0 || discoveryPort > 65535) {
				throw new IllegalArgumentException("Invalid port range for discovery port");
			}
			discoverySet.add(discoveryPort);
		}

		// Set discovery ports
		this.discoveryPorts = discoverySet;
		String discoveryString = Arrays.toString(discoveryPorts);
		LOG.debug("Set discovery ports to "
				+ (discoverySet.size() > 0 ? discoveryString.substring(1, discoveryString.length() - 1) : "nothing"));
	}

	/**
	 * Adds a discovery port to start broadcasting to.
	 * 
	 * @param discoveryPort
	 *            the discovery port to start broadcasting to.
	 */
	public final void addDiscoveryPort(int discoveryPort) {
		discoveryPorts.add(discoveryPort);
		LOG.debug("Added discovery port " + discoveryPort);
	}

	/**
	 * Removes a discovery port to stop broadcasting to.
	 * 
	 * @param discoveryPort
	 *            the discovery part to stop broadcasting to.
	 */
	public final void removeDiscoveryPort(int discoveryPort) {
		discoveryPorts.remove(discoveryPort);
		LOG.debug("Removed discovery port " + discoveryPort);
	}

	/**
	 * Returns the client's discovery mode.
	 * 
	 * @return the client's discovery mode.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public final DiscoveryMode getDiscoveryMode() {
		return this.discoveryMode;
	}

	/**
	 * Sets the client's discovery mode.
	 * 
	 * @param mode
	 *            the new discovery mode.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public final void setDiscoveryMode(DiscoveryMode mode) {
		if (listeners.size() <= 0) {
			LOG.warn("Client has no listeners");
		}
		this.discoveryMode = (mode != null ? mode : DiscoveryMode.DISABLED);
		if (this.discoveryMode == DiscoveryMode.DISABLED) {
			for (InetSocketAddress address : discovered.keySet()) {
				this.callEvent(listener -> listener.onServerForgotten(address));
			}
			discovered.clear(); // We are not discovering servers anymore!
			LOG.debug("Cleared discovered servers due to discovery mode being set to " + DiscoveryMode.DISABLED);
		}
		LOG.debug("Set discovery mode to " + mode);
	}

	/**
	 * Returns the locally discovered servers.
	 * 
	 * @return the locally discovered servers.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public final DiscoveredServer[] getLocalServers() {
		return discovered.values().toArray(new DiscoveredServer[discovered.size()]);
	}

	/**
	 * Returns the externally discovered servers.
	 * 
	 * @return the externally discovered servers.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public final DiscoveredServer[] getExternalServers() {
		return externalServers.values().toArray(new DiscoveredServer[externalServers.size()]);
	}

	/**
	 * Adds a server to the client's external server discovery list. This allows
	 * for the discovery of servers on external networks.
	 * 
	 * @param address
	 *            the server address.
	 */
	public final void addExternalServer(InetSocketAddress address) {
		if (!externalServers.containsKey(address)) {
			externalServers.put(address, new DiscoveredServer(address, -1L, null));
			LOG.debug("Added external server with address " + address);
			this.callEvent(listener -> listener.onExternalServerAdded(address));
		}
	}

	/**
	 * Adds a server to the client's external server discovery list. This allows
	 * for the discovery of servers on external networks.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 */
	public final void addExternalServer(InetAddress address, int port) {
		this.addExternalServer(new InetSocketAddress(address, port));
	}

	/**
	 * Adds a server to the client's external server discovery list. This allows
	 * for the discovery of servers on external networks.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public final void addExternalServer(String address, int port) throws UnknownHostException {
		this.addExternalServer(InetAddress.getByName(address), port);
	}

	/**
	 * Removes an external server from the client's external server discovery
	 * list.
	 * 
	 * @param address
	 *            the server address.
	 */
	public final void removeExternalServer(InetSocketAddress address) {
		if (externalServers.containsKey(address)) {
			externalServers.remove(address);
			LOG.debug("Removed external server with address " + address);
			this.callEvent(listener -> listener.onExternalServerRemoved(address));
		}
	}

	/**
	 * Removes an external server from the client's external server discovery
	 * list.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 */
	public final void removeExternalServer(InetAddress address, int port) {
		this.removeExternalServer(new InetSocketAddress(address, port));
	}

	/**
	 * Removes an external server from the client's external server discovery
	 * list.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public final void removeExternalServer(String address, int port) throws UnknownHostException {
		this.removeExternalServer(InetAddress.getByName(address), port);
	}

	/**
	 * Removes an external server from the client's external server discovery
	 * list.
	 * 
	 * @param server
	 *            the discovered server.
	 * @throws IllegalArgumentException
	 *             if the discovered server is was not discovered by the client.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryMode DiscoveryMode
	 */
	public final void removeExternalServer(DiscoveredServer server) throws IllegalArgumentException {
		if (!externalServers.contains(server)) {
			throw new IllegalArgumentException("Externally discovered server does not belong to client");
		}
		this.removeExternalServer(server.getAddress());
	}

	/**
	 * Removes all external servers from the client's external server discovery
	 * list.
	 * 
	 * @see com.whirvis.jraknet.client.discovery.DiscoveredServer
	 *      DiscoveredServer
	 */
	public final void clearExternalServers() {
		externalServers.clear();
	}

	/**
	 * Returns the discovered servers, both local and external.
	 * 
	 * @return the discovered servers, both local and external.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveredServer
	 *      DiscoveredServer
	 */
	public final DiscoveredServer[] getServers() {
		ArrayList<DiscoveredServer> servers = new ArrayList<DiscoveredServer>();
		servers.addAll(discovered.values());
		servers.addAll(externalServers.values());
		return servers.toArray(new DiscoveredServer[servers.size()]);
	}

	/**
	 * Returns whether or not the client is currently running. If it is running,
	 * this means that it can still be used to connect to servers. If it has
	 * been shutdown, this will not be the case.
	 * 
	 * @return <code>true</code> if the client is running, <code>false</code>
	 *         otherwise.
	 * @see #shutdown()
	 */
	public final boolean isRunning() {
		if (channel == null) {
			return false; // No channel to check
		}
		return channel.isOpen();
	}

	/**
	 * Returns the address the client is bound to.
	 * 
	 * @return the address the client is bound to.
	 */
	public InetSocketAddress getAddress() {
		return this.bindAddress;
	}

	/**
	 * Returns the IP address the client is bound to.
	 * 
	 * @return the IP address the client is bound to.
	 */
	public InetAddress getInetAddress() {
		return bindAddress.getAddress();
	}

	/**
	 * Returns the port the client is bound to.
	 * 
	 * @return the port the client is bound to.
	 */
	public int getPort() {
		return bindAddress.getPort();
	}

	/**
	 * Returns the maximum transfer unit sizes the client will use during login.
	 * 
	 * @return the maximum transfer unit sizes the client will use during login.
	 */
	public final int[] getMaximumTransferUnitSizes() {
		int[] maximumTransferUnitSizes = new int[maximumTransferUnits.length];
		for (int i = 0; i < maximumTransferUnitSizes.length; i++) {
			maximumTransferUnitSizes[i] = maximumTransferUnits[i].getSize();
		}
		return maximumTransferUnitSizes;
	}

	/**
	 * Sets the maximum transfer unit sizes that will be used by the client
	 * during connection.
	 * 
	 * @param maximumTransferUnitSizes
	 *            the maximum transfer unit sizes.
	 * @throws IllegalArgumentException
	 *             if the maximum transfer unit is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}.
	 * @throws RuntimeException
	 *             if no valid maximum transfer unit could be located for the
	 *             network card that the client's binding address is bound to.
	 */
	public final void setMaximumTransferUnitSizes(int... maximumTransferUnitSizes)
			throws IllegalArgumentException, RuntimeException {
		// Determine valid maximum transfer units
		boolean foundTransferUnit = false;
		ArrayList<MaximumTransferUnit> maximumTransferUnits = new ArrayList<MaximumTransferUnit>();
		for (int i = 0; i < maximumTransferUnitSizes.length; i++) {
			int maximumTransferUnitSize = maximumTransferUnitSizes[i];
			if (maximumTransferUnitSize < RakNet.MINIMUM_MTU_SIZE) {
				throw new IllegalArgumentException(
						"Maximum transfer unit size must be higher than " + RakNet.MINIMUM_MTU_SIZE);
			}
			if (RakNet.getMaximumTransferUnit() >= maximumTransferUnitSize) {
				maximumTransferUnits.add(new MaximumTransferUnit(maximumTransferUnitSize,
						(i * 2) + (i + 1 < maximumTransferUnitSizes.length ? 2 : 1)));
				foundTransferUnit = true;
			} else {
				LOG.warn("Valid maximum transfer unit " + maximumTransferUnitSize
						+ " failed to register due to network card limitations");
			}
		}
		this.maximumTransferUnits = maximumTransferUnits.toArray(new MaximumTransferUnit[maximumTransferUnits.size()]);

		// Determine the highest maximum transfer unit
		int maximumMaximumTransferUnit = Integer.MIN_VALUE;
		for (MaximumTransferUnit maximumTransferUnit : maximumTransferUnits) {
			if (maximumTransferUnit.getSize() > maximumMaximumTransferUnit) {
				maximumMaximumTransferUnit = maximumTransferUnit.getSize();
			}
		}
		this.maximumMaximumTransferUnitSize = maximumMaximumTransferUnit;
		if (foundTransferUnit == false) {
			throw new RuntimeException("No compatible maximum transfer unit found for machine network cards");
		}
	}

	/**
	 * Returns the session the client is currently connected to.
	 * 
	 * @return the session the client is currently connected to,
	 *         <code>null</code> if it is not connected to a server.
	 * @see com.whirvis.jraknet.session.RakNetServerSession RakNetServerSession
	 */
	public final RakNetServerSession getSession() {
		return this.session;
	}

	/**
	 * Returns whether or not the client is currently connected to a server.
	 * 
	 * @return <code>true</code> if the client is currently connected to a
	 *         server, <code>false</code> otherwise.
	 */
	public final boolean isConnected() {
		if (session == null) {
			return false; // No session state to check
		}
		return session.getState().equals(RakNetState.CONNECTED);
	}

	/**
	 * Returns the thread the client is running on.
	 * 
	 * @return the thread the server is running on, <code>null</code> if it was
	 *         not started using one of the
	 *         {@link #connectThreaded(InetSocketAddress) connectThreaded()}
	 *         methods.
	 */
	public final Thread getThread() {
		return this.clientThread;
	}

	/**
	 * Called by the {@link com.whirvis.jraknet.client.discovery.DiscoveryThread
	 * DiscoveryThread} to update client discovery data. Calling this is
	 * unnecessary and unrecommended except by the discovery system.
	 * 
	 * @see com.whirvis.jraknet.client.discovery.DiscoveryThread DicoveryThread
	 */
	public final void updateDiscoveryData() {
		// Remove all servers that have timed out
		ArrayList<InetSocketAddress> forgottenServers = new ArrayList<InetSocketAddress>();
		for (InetSocketAddress discoveredServerAddress : discovered.keySet()) {
			DiscoveredServer discoveredServer = discovered.get(discoveredServerAddress);
			if (System.currentTimeMillis()
					- discoveredServer.getDiscoveryTimestamp() >= DiscoveredServer.SERVER_TIMEOUT_MILLIS) {
				forgottenServers.add(discoveredServerAddress);
				this.callEvent(listener -> listener.onServerForgotten(discoveredServerAddress));
			}
		}
		discovered.keySet().removeAll(forgottenServers);
		if (forgottenServers.size() > 0) {
			LOG.debug("Forgot " + forgottenServers.size() + " server" + (forgottenServers.size() == 1 ? "" : "s"));
		}

		long currentTime = System.currentTimeMillis();
		if (currentTime - lastPingBroadcast >= PING_BROADCAST_WAIT_MILLIS) {
			// Broadcast ping to local network
			if (discoveryMode != DiscoveryMode.DISABLED) {
				Iterator<Integer> discoveryIterator = discoveryPorts.iterator();
				while (discoveryIterator.hasNext()) {
					int discoveryPort = discoveryIterator.next().intValue();
					UnconnectedPing ping = new UnconnectedPing();
					if (discoveryMode == DiscoveryMode.OPEN_CONNECTIONS) {
						ping = new UnconnectedPingOpenConnections();
					}
					ping.timestamp = this.getTimestamp();
					ping.pingId = this.pingId;
					ping.encode();

					if (!ping.failed()) {
						this.sendNettyMessage(ping, new InetSocketAddress("255.255.255.255", discoveryPort));
						LOG.debug("Broadcasted unconnected ping to port " + discoveryPort);
					} else {
						LOG.error(UnconnectedPing.class.getSimpleName()
								+ " failed to encode, unable to broadcast ping to local servers");
					}
				}
			}

			// Send ping to external servers
			if (!externalServers.isEmpty()) {
				UnconnectedPing ping = new UnconnectedPing();
				ping.timestamp = this.getTimestamp();
				ping.pingId = this.pingId;
				ping.encode();
				if (!ping.failed()) {
					for (InetSocketAddress externalAddress : externalServers.keySet()) {
						this.sendNettyMessage(ping, externalAddress);
						LOG.debug("Broadcasting ping to server with address " + externalAddress);
					}
				} else {
					LOG.error(UnconnectedPing.class.getSimpleName()
							+ " failed to encode, unable to broadcast ping to external servers");
				}
			}
			this.lastPingBroadcast = currentTime;
		}
	}

	/**
	 * Handles the unconnected pong and updates discovery data accordingly.
	 * 
	 * @param sender
	 *            the sender of the unconnected pong.
	 * @param pong
	 *            the unconnected pong.
	 * @see com.whirvis.jraknet.protocol.status.UnconnectedPong UnconnectedPong
	 */
	private final void updateDiscoveryData(InetSocketAddress sender, UnconnectedPong pong) {
		if (RakNet.isLocalAddress(sender) && !externalServers.containsKey(sender)) {
			if (!discovered.containsKey(sender)) { // Newly discovered server
				discovered.put(sender, new DiscoveredServer(sender, System.currentTimeMillis(), pong.identifier));
				LOG.info("Discovered local server with address " + sender);
				this.callEvent(listener -> listener.onServerDiscovered(sender, pong.identifier));
			} else { // Server data was changed
				DiscoveredServer server = discovered.get(sender);
				server.setDiscoveryTimestamp(System.currentTimeMillis());
				if (!pong.identifier.equals(server.getIdentifier())) {
					server.setIdentifier(pong.identifier);
					LOG.debug("Updated local server with address " + sender + " identifier to \"" + pong.identifier
							+ "\"");
					this.callEvent(listener -> listener.onServerIdentifierUpdate(sender, pong.identifier));
				}
			}
		} else if (externalServers.containsKey(sender)) {
			DiscoveredServer server = externalServers.get(sender);
			server.setDiscoveryTimestamp(System.currentTimeMillis());
			if (!pong.identifier.equals(server.getIdentifier())) {
				server.setIdentifier(pong.identifier);
				LOG.debug("Updated local server with address " + sender + " identifier to \"" + pong.identifier + "\"");
				this.callEvent(listener -> listener.onExternalServerIdentifierUpdate(sender, pong.identifier));
			}
		}
	}

	/**
	 * Sends a Netty message over the channel raw. This should be used
	 * sparingly, as if it is used incorrectly it could break server sessions
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
		LOG.debug("Sent netty message with size of " + buf.capacity() + " bytes (" + (buf.capacity() * 8) + " bits) to "
				+ address);
	}

	/**
	 * Sends a Netty message over the channel raw. This should be used
	 * sparingly, as if it is used incorrectly it could break server sessions
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
	 * sparingly, as if it is used incorrectly it could break server sessions
	 * entirely. In order to send a message to a session, use one of the
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(com.whirvis.jraknet.protocol.Reliability, int)
	 * sendMessage()} methods.
	 * 
	 * @param packet
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
	 * Handles a packet received by the
	 * {@link com.whirvis.jraknet.client.RakNetClientHandler
	 * RakNetClientHandler}.
	 * 
	 * @param packet
	 *            the packet to handle.
	 * @param sender
	 *            the address of the sender.
	 * @see com.whirvis.jraknet.RakNetPacket RakNetPacket
	 * @see com.whirvis.jraknet.client.RakNetClientHandler RakNetClientHandler
	 */
	protected final void handleMessage(RakNetPacket packet, InetSocketAddress sender) {
		short packetId = packet.getId();
		if (packetId == ID_UNCONNECTED_PONG) {
			UnconnectedPong pong = new UnconnectedPong(packet);
			pong.decode();
			if (pong.identifier != null) {
				this.updateDiscoveryData(sender, pong);
			}
		} else if (preparation != null) {
			if (sender.equals(preparation.address)) {
				preparation.handleMessage(packet);
			}
		} else if (session != null) {
			if (sender.equals(session.getAddress())) {
				if (packetId >= ID_CUSTOM_0 && packetId <= ID_CUSTOM_F) {
					CustomPacket custom = new CustomPacket(packet);
					custom.decode();

					session.handleCustom(custom);
				} else if (packetId == Acknowledge.ACKNOWLEDGED || packetId == Acknowledge.NOT_ACKNOWLEDGED) {
					Acknowledge acknowledge = new Acknowledge(packet);
					acknowledge.decode();

					session.handleAcknowledge(acknowledge);
				}
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
	 * Called by the {@link com.whirvis.jraknet.client.RakNetClientHandler
	 * RakNetClientHander} when it catches a <code>Throwable</code> while
	 * handling a packet.
	 * 
	 * @param address
	 *            the address that caused the exception.
	 * @param cause
	 *            the <code>Throwable</code> caught by the handler.
	 */
	protected final void handleHandlerException(InetSocketAddress address, Throwable cause) {
		if (address.equals(preparation.address)) {
			if (preparation != null) {
				preparation.cancelReason = new NettyHandlerException(this, handler, cause);
			} else if (session != null) {
				this.disconnect(cause.getClass().getName() + ": " + cause.getLocalizedMessage());
			}
		}
		LOG.warn("Handled exception " + cause.getClass().getName() + " caused by address " + address);
		this.callEvent(listener -> listener.onHandlerException(address, cause));
	}

	/**
	 * Connects the client to a server.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @throws IllegalStateException
	 *             if the client has been shutdown or is currently connected to
	 *             a server.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(InetSocketAddress address) throws IllegalStateException, RakNetException {
		if (listeners.size() <= 0) {
			LOG.warn("Client has no listeners");
		} else if (!this.isRunning()) {
			throw new IllegalStateException("Client is not running");
		} else if (this.isConnected()) {
			throw new IllegalStateException("Client is currently connected to a server");
		}

		// Prepare connection
		MaximumTransferUnit[] units = MaximumTransferUnit.sort(maximumTransferUnits);
		for (MaximumTransferUnit unit : maximumTransferUnits) {
			unit.reset();
		}
		this.preparation = new SessionPreparation(this, units[0].getSize(), maximumMaximumTransferUnitSize);
		preparation.address = address;

		// Send open connection request one with a decreasing MTU
		int retriesLeft = 0;
		try {
			for (MaximumTransferUnit unit : units) {
				retriesLeft += unit.getRetries();
				while (unit.retry() > 0 && preparation.loginPackets[0] == false && preparation.cancelReason == null) {
					OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
					connectionRequestOne.maximumTransferUnit = unit.getSize();
					connectionRequestOne.protocolVersion = this.getProtocolVersion();
					connectionRequestOne.encode();
					this.sendNettyMessage(connectionRequestOne, address);
					Thread.sleep(500);
				}
			}
		} catch (InterruptedException e) {
			throw new RakNetException(e);
		}

		// If the server did not respond then it is offline
		if (preparation.loginPackets[0] == false && preparation.cancelReason == null) {
			preparation.cancelReason = new ServerOfflineException(this, preparation.address);
		}

		// Send open connection request two until a response is received
		try {
			while (retriesLeft > 0 && preparation.loginPackets[1] == false && preparation.cancelReason == null) {
				OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo();
				connectionRequestTwo.clientGuid = this.guid;
				connectionRequestTwo.address = preparation.address;
				connectionRequestTwo.maximumTransferUnit = preparation.maximumTransferUnit;
				connectionRequestTwo.encode();

				if (!connectionRequestTwo.failed()) {
					this.sendNettyMessage(connectionRequestTwo, address);
					Thread.sleep(500);
				} else {
					preparation.cancelReason = new PacketBufferException(this, connectionRequestTwo);
				}
			}
		} catch (InterruptedException e) {
			throw new RakNetException(e);
		}

		// If the server did not respond then it is offline
		if (preparation.loginPackets[1] == false && preparation.cancelReason == null) {
			preparation.cancelReason = new ServerOfflineException(this, preparation.address);
		}

		// If the session was set we are connected
		if (preparation.readyForSession()) {
			// Set session and delete preparation data
			this.session = preparation.createSession(channel);
			this.preparation = null;

			// Send connection packet
			ConnectionRequest connectionRequest = new ConnectionRequest();
			connectionRequest.clientGuid = this.guid;
			connectionRequest.timestamp = (System.currentTimeMillis() - this.timestamp);
			connectionRequest.encode();
			session.sendMessage(Reliability.RELIABLE_ORDERED, connectionRequest);
			LOG.debug("Sent connection packet to server");

			// Initiate connection loop required for the session to function
			this.initConnection();
		} else {
			// Reset the connection data, it failed
			RakNetException cancelReason = preparation.cancelReason;
			this.preparation = null;
			this.session = null;
			throw cancelReason;
		}
	}

	/**
	 * Connects the client to a servers.
	 * 
	 * @param address
	 *            the IP address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @throws IllegalStateException
	 *             if the client has been shutdown or is currently connected to
	 *             a server.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(InetAddress address, int port) throws IllegalStateException, RakNetException {
		this.connect(new InetSocketAddress(address, port));
	}

	/**
	 * Connects the client to a server.
	 * 
	 * @param address
	 *            the IP address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>bindAddress</code> could be
	 *             found, or if a scope_id was specified for a global IPv6
	 *             address.
	 * @throws IllegalStateException
	 *             if the client has been shutdown or is currently connected to
	 *             a server.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(String address, int port)
			throws UnknownHostException, IllegalStateException, RakNetException {
		this.connect(InetAddress.getByName(address), port);
	}

	/**
	 * Connects the the client to the discovered server.
	 * 
	 * @param server
	 *            the discovered server to connect to.
	 * @throws IllegalStateException
	 *             if the client has been shutdown or is currently connected to
	 *             a server.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 * @see com.whirvis.jraknet.client.discovery.DiscoveredServer
	 *      DiscoveredServer
	 */
	public final void connect(DiscoveredServer server) throws IllegalStateException, RakNetException {
		this.connect(server.getAddress());
	}

	/**
	 * Connects the client to a server with the address on its own
	 * <code>Thread</code>. Since the client is running on another thread, all
	 * throwables will be sent there. In order to catch these throwables, use
	 * the
	 * {@link com.whirvis.jraknet.client.RakNetClientListener#onThreadException(Throwable)
	 * onThreadException(Throwable)} method in
	 * {@link com.whirvis.jraknet.client.RakNetClientListener
	 * RakNetClientListener}.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @return the <code>Thread</code> the client is running on.
	 * @see com.whirvis.jraknet.client.RakNetClientListener#onThreadException(Throwable)
	 *      onThreadException(Throwable)
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	public final Thread connectThreaded(InetSocketAddress address) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					connect(address);
				} catch (Throwable throwable) {
					callEvent(listener -> listener.onThreadException(throwable));
					if (getListeners().length < 0) {
						throwable.printStackTrace();
					}
				}
			}
		};
		thread.setName("jraknet-client-" + Long.toHexString(this.getGloballyUniqueId()).toLowerCase());
		thread.start();
		this.clientThread = thread;
		LOG.info("Started on thread with name \"" + thread.getName() + "\"");
		return thread;
	}

	/**
	 * Connects the client to a server with the address on its own
	 * <code>Thread</code>. Since the client is running on another thread, all
	 * throwables will be sent there. In order to catch these throwables, use
	 * the
	 * {@link com.whirvis.jraknet.client.RakNetClientListener#onThreadException(Throwable)
	 * onThreadException(Throwable)} in the
	 * {@link com.whirvis.jraknet.client.RakNetClientListener
	 * RakNetClientListener}.
	 * 
	 * @param address
	 *            the IP address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @return the <code>Thread</code> the client is running on.
	 * @see com.whirvis.jraknet.client.RakNetClientListener#onThreadException(Throwable)
	 *      onThreadException(Throwable)
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	public final Thread connectThreaded(InetAddress address, int port) {
		return this.connectThreaded(new InetSocketAddress(address, port));
	}

	/**
	 * Connects the client to a server with the address on its own
	 * <code>Thread</code>. Since the client is running on another thread, all
	 * throwables will be sent there. In order to catch these throwables, use
	 * the
	 * {@link com.whirvis.jraknet.client.RakNetClientListener#onThreadException(Throwable)
	 * onThreadException(Throwable)} in the
	 * {@link com.whirvis.jraknet.client.RakNetClientListener
	 * RakNetClientListener}.
	 * 
	 * @param address
	 *            the IP address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 * @return the <code>Thread</code> the client is running on.
	 * @see com.whirvis.jraknet.client.RakNetClientListener#onThreadException(Throwable)
	 *      onThreadException(Throwable)
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	public final Thread connectThreaded(String address, int port) throws UnknownHostException {
		return this.connectThreaded(InetAddress.getByName(address), port);
	}

	/**
	 * Connects the client to a server with the address on its own
	 * <code>Thread</code>. Since the client is running on another thread, all
	 * throwables will be sent there. In order to catch these throwables, use
	 * the
	 * {@link com.whirvis.jraknet.client.RakNetClientListener#onThreadException(Throwable)
	 * onThreadException(Throwable)} in the
	 * {@link com.whirvis.jraknet.client.RakNetClientListener
	 * RakNetClientListener}.
	 * 
	 * @param server
	 *            the discovered server to connect to.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 * @return the <code>Thread</code> the client is running on.
	 * @see com.whirvis.jraknet.client.RakNetClientListener#onThreadException(Throwable)
	 *      onThreadException(Throwable)
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 * @see com.whirvis.jraknet.client.discovery.DiscoveredServer
	 *      DiscoveredServer
	 */
	public final Thread connectThreaded(DiscoveredServer server) {
		return this.connectThreaded(server.getAddress());
	}

	/**
	 * Starts the loop needed for the client to stay connected to the server.
	 * 
	 * @throws IllegalStateException
	 *             if the client is not running or is not connected to a server.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 */
	private final void initConnection() throws IllegalStateException, RakNetException {
		if (!this.isRunning()) {
			throw new IllegalStateException("Client is not running");
		} else if (session == null) {
			throw new RakNetClientException(this, "Attempted to initiate connection without session");
		}
		LOG.debug("Initiated connected with server");
		try {
			while (session != null) {
				session.update();
				Thread.sleep(0, 1); // Lower CPU usage
			}
		} catch (InterruptedException e) {
			throw new RakNetException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalStateException
	 *             if the client is not connected to a server.
	 */
	@Override
	public final EncapsulatedPacket sendMessage(Reliability reliability, int channel, Packet packet)
			throws IllegalStateException {
		if (!this.isConnected()) {
			throw new IllegalStateException("Cannot send messages while not connected to a server");
		}
		return session.sendMessage(reliability, channel, packet);
	}

	/**
	 * Disconnects the client from the server.
	 * 
	 * @param reason
	 *            the reason for disconnection. A <code>null</code> reason will
	 *            have <code>"Disconnected"</code> be used as the reason
	 *            instead.
	 * @throws IllegalStateException
	 *             if the client is not connected to a server.
	 */
	public final void disconnect(String reason) throws IllegalStateException {
		if (session == null) {
			throw new IllegalStateException("Client is not connected to a server");
		}

		// Close session and interrupt client thread
		session.closeConnection();
		if (this.clientThread != null) {
			clientThread.interrupt();
		}

		// Destroy session
		LOG.info("Disconnected from server with address " + session.getAddress() + " with reason \""
				+ (reason == null ? "Disconnected" : reason) + "\"");
		this.callEvent(listener -> listener.onDisconnect(session, reason == null ? "Disconnected" : reason));
		this.session = null;
	}

	/**
	 * Disconnects the client from the server.
	 * 
	 * @throws IllegalStateException
	 *             if the client is not connected to a server.
	 */
	public final void disconnect() throws IllegalStateException {
		this.disconnect(null);
	}

	/**
	 * Shuts down the client. Once this is called, the client will no longer be
	 * able to connect to servers. If the client is connected to a server when
	 * this is called, it will also disconnect from the server using the reason
	 * here.
	 * 
	 * @param reason
	 *            the reason for shutdown. A <code>null</code> reason will have
	 *            </code>"Shutdown"</code> be used as the reason instead.
	 * @throws IllegalStateException
	 *             if the client is not running.
	 */
	public final void shutdown(String reason) throws IllegalStateException {
		if (!this.isRunning()) {
			throw new IllegalStateException("Client is not running");
		}

		// Disconnect from server and shutdown Netty
		if (this.isConnected()) {
			this.disconnect(reason == null ? "Shutdown" : reason);
		}
		channel.close();
		group.shutdownGracefully();

		// Shutdown discovery system if needed
		discoverySystem.removeClient(this);
		if (discoverySystem.getClients().length <= 0) {
			discoverySystem.shutdown();
			discoverySystem = new DiscoveryThread();
		}

		LOG.info("Shutdown client");
		this.callEvent(listener -> listener.onClientShutdown(reason == null ? "Shutdown" : reason));
	}

	/**
	 * Shuts down the client. Once this is called, the client will no longer be
	 * able to connect to servers.
	 * 
	 * @throws IllegalStateException
	 *             if the client is not running.
	 */
	public final void shutdown() throws IllegalStateException {
		this.shutdown(null);
	}

	@Override
	public String toString() {
		return "RakNetClient [guid=" + guid + ", pingId=" + pingId + ", timestamp=" + timestamp + ", discoveryMode="
				+ discoveryMode + "]";
	}

}
