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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.discovery.DiscoveredServer;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestTwo;
import com.whirvis.jraknet.protocol.login.ConnectionRequest;
import com.whirvis.jraknet.protocol.message.CustomPacket;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.AcknowledgedPacket;
import com.whirvis.jraknet.scheduler.Scheduler;
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
 * @see RakNetClientListener
 * @see #addListener(RakNetClientListener)
 * @see #connect(InetSocketAddress)
 */
public class RakNetClient implements UnumRakNetPeer, RakNetClientListener {

	/**
	 * The default maximum transfer unit sizes used by the client. These were
	 * chosen due to the maximum transfer unit sizes used by the Minecraft
	 * client during connection.
	 */
	public static final int[] DEFAULT_TRANSFER_UNIT_SIZES = new int[] { 1492, 1200, 576, RakNet.MINIMUM_MTU_SIZE };

	/**
	 * The amount of time to wait before the client broadcasts another ping to
	 * the local network and all added external servers. This was also
	 * determined based on Minecraft's frequency of broadcasting pings to
	 * servers.
	 */
	public static final long PING_BROADCAST_WAIT_MILLIS = 1000L;

	private final long guid;
	private final Logger log;
	private final long timestamp;
	private final ConcurrentLinkedQueue<RakNetClientListener> listeners;
	private Bootstrap bootstrap;
	private RakNetClientHandler handler;
	private EventLoopGroup group;
	private Channel channel;
	private InetSocketAddress bindAddress;
	private MaximumTransferUnit[] maximumTransferUnits;
	private int highestMaximumTransferUnitSize;
	private SessionPlanner preparation;
	private volatile RakNetServerSession session;
	private Thread sessionThread;

	/**
	 * Creates a RakNet client.
	 * 
	 * @param address
	 *            the address the client will bind to during connection. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address along with the client giving Netty the
	 *            responsibility of choosing which port to bind to.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public RakNetClient(InetSocketAddress address) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		}
		UUID uuid = UUID.randomUUID();
		this.guid = uuid.getMostSignificantBits();
		this.log = LogManager.getLogger("jraknet-client-" + Long.toHexString(guid));
		this.timestamp = System.currentTimeMillis();
		this.listeners = new ConcurrentLinkedQueue<RakNetClientListener>();
		this.bindAddress = address;
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param address
	 *            the IP address the client will bind to during connection. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param port
	 *            the port the client will bind to during connection. A port of
	 *            <code>0</code> will have the client give Netty the
	 *            respsonsibility of choosing the port to bind to.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public RakNetClient(InetAddress address, int port) throws NullPointerException {
		this(new InetSocketAddress(address, port));
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param address
	 *            the IP address the client will bind to during connection. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public RakNetClient(InetAddress address) {
		this(new InetSocketAddress(address, 0));
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param host
	 *            the IP address the client will bind to during connection. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @param port
	 *            the port the client will bind to during connection. A port of
	 *            <code>0</code> will have the client give Netty the
	 *            respsonsibility of choosing the port to bind to.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>. if no IP
	 *             address for the host could be found, or if a scope_id was
	 *             specified for a global IPv6 address.
	 */
	public RakNetClient(String host, int port) throws UnknownHostException {
		this(InetAddress.getByName(host), port);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param host
	 *            the IP address the client will bind to during connection. A
	 *            <code>null</code> address will have the client bind to the
	 *            wildcard address.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 */
	public RakNetClient(String host) throws NullPointerException, UnknownHostException {
		this(host, 0);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param port
	 *            the port the client will bind to during creation. A port of
	 *            <code>zero</code> will have the client give Netty the
	 *            respsonsibility of choosing the port to bind to.
	 */
	public RakNetClient(int port) {
		this(new InetSocketAddress(port));
	}

	/**
	 * Creates a RakNet client.
	 */
	public RakNetClient() {
		this((InetSocketAddress) /* Solves ambiguity */ null);
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
	 * Returns the client's timestamp.
	 * 
	 * @return the client's timestamp.
	 */
	public final long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	/**
	 * Adds a {@link RakNetClientListener} to the client. Listeners are used to
	 * listen for events that occur relating to the client such as connecting to
	 * discovers, discovering local servers, and more.
	 * 
	 * @param listener
	 *            the listener to add.
	 * @return the client.
	 * @throws NullPointerException
	 *             if the <code>listener</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>listener</code> is another client that is not
	 *             the client itself.
	 */
	public final RakNetClient addListener(RakNetClientListener listener)
			throws NullPointerException, IllegalArgumentException {
		if (listener == null) {
			throw new NullPointerException("Listener cannot be null");
		} else if (listener instanceof RakNetClient && !this.equals(listener)) {
			throw new IllegalArgumentException("A client cannot be used as a listener except for itself");
		} else if (listeners.contains(listener)) {
			return this; // Prevent duplicates
		}
		listeners.add(listener);
		if (listener != this) {
			log.debug("Added listener of class " + listener.getClass().getName());
		} else {
			log.debug("Added self listener");
		}
		return this;
	}

	/**
	 * Adds the client to its own set of listeners, used when extending the
	 * {@link RakNetClient} directly.
	 * 
	 * @return the client.
	 * @see RakNetClientListener
	 * @see #addListener(RakNetClientListener)
	 */
	public final RakNetClient addSelfListener() {
		return this.addListener(this);
	}

	/**
	 * Removes a {@link RakNetClientListener} from the client.
	 * 
	 * @param listener
	 *            the listener to remove.
	 * @return the client.
	 */
	public final RakNetClient removeListener(RakNetClientListener listener) {
		if (listeners.remove(listener)) {
			if (listener != this) {
				log.debug("Removed listener of class " + listener.getClass().getName());
			} else {
				log.debug("Removed self listener");
			}
		}
		return this;
	}

	/**
	 * Removes the client from its own set of listeners, used when extending the
	 * {@link RakNetClient} directly.
	 * 
	 * @return the client.
	 * @see RakNetClientListener
	 * @see #removeListener(RakNetClientListener)
	 */
	public final RakNetClient removeSelfListener() {
		return this.removeListener(this);
	}

	/**
	 * Calls an event.
	 * 
	 * @param event
	 *            the event to call.
	 * @throws NullPointerException
	 *             if the <code>event</code> is <code>null</code>.
	 * @see RakNetClientListener
	 */
	public final void callEvent(Consumer<? super RakNetClientListener> event) throws NullPointerException {
		if (event == null) {
			throw new NullPointerException("Event cannot be null");
		}
		listeners.forEach(listener -> Scheduler.scheduleSync(listener, event));
	}

	/**
	 * Returns whether or not the client is currently running. If it is running,
	 * this means that it is currently connecting to or is connected to a
	 * server.
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
	 * Returns the address the client is bound to. This will be the value
	 * supplied during client creation until the client has connected to a
	 * server using the {@link #connect(InetSocketAddress) connect()} method.
	 * Once the client has connected a server, the bind address will be changed
	 * to the address returned from the channel's
	 * {@link io.netty.channel.Channel#localAddress() localAddress()} method.
	 * 
	 * @return the address the client is bound to.
	 */
	public InetSocketAddress getAddress() {
		return this.bindAddress;
	}

	/**
	 * Returns the IP address the client is bound to based on the address
	 * returned from {@link #getAddress()}.
	 * 
	 * @return the IP address the client is bound to.
	 */
	public InetAddress getInetAddress() {
		return bindAddress.getAddress();
	}

	/**
	 * Returns the port the client is bound to based on the address returned
	 * from {@link #getAddress()}.
	 * 
	 * @return the port the client is bound to.
	 */
	public int getPort() {
		return bindAddress.getPort();
	}

	/**
	 * Returns the maximum transfer unit sizes the client will use during
	 * connection.
	 * 
	 * @return the maximum transfer unit sizes the client will use during
	 *         connection.
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
	 * @throws NullPointerException
	 *             if the <code>maximumTransferUnitSizes</code> is
	 *             <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>maximumTransferUnitSizes</code> is empty or one
	 *             of its values is less than
	 *             {@value com.whirvis.jraknet.RakNet#MINIMUM_MTU_SIZE}.
	 * @throws RuntimeException
	 *             if determining the maximum transfer unit for the network card
	 *             with the client's bind address was a failure or no valid
	 *             maximum transfer unit could be located for the network card
	 *             that the client's binding address is bound to.
	 */
	public final void setMaximumTransferUnitSizes(int... maximumTransferUnitSizes)
			throws NullPointerException, IllegalArgumentException, RuntimeException {
		if (maximumTransferUnitSizes == null) {
			throw new NullPointerException("Maximum transfer unit sizes cannot be null");
		} else if (maximumTransferUnitSizes.length <= 0) {
			throw new IllegalArgumentException("At least one maximum transfer unit size must be specified");
		}

		// Determine valid maximum transfer units
		boolean foundTransferUnit = false;
		int networkCardMaximumTransferUnit = RakNet.getMaximumTransferUnit(bindAddress.getAddress());
		if (networkCardMaximumTransferUnit < 0) {
			throw new RuntimeException("Failed to determine maximum transfer unit" + (bindAddress.getAddress() != null
					? " for network card with address " + bindAddress.getAddress() : ""));
		}
		ArrayList<MaximumTransferUnit> maximumTransferUnits = new ArrayList<MaximumTransferUnit>();
		for (int i = 0; i < maximumTransferUnitSizes.length; i++) {
			int maximumTransferUnitSize = maximumTransferUnitSizes[i];
			if (maximumTransferUnitSize < RakNet.MINIMUM_MTU_SIZE) {
				throw new IllegalArgumentException(
						"Maximum transfer unit size must be higher than " + RakNet.MINIMUM_MTU_SIZE);
			}
			if (networkCardMaximumTransferUnit >= maximumTransferUnitSize) {
				maximumTransferUnits.add(new MaximumTransferUnit(maximumTransferUnitSize,
						(i * 2) + (i + 1 < maximumTransferUnitSizes.length ? 2 : 1)));
				foundTransferUnit = true;
			} else {
				log.warn("Valid maximum transfer unit " + maximumTransferUnitSize
						+ " failed to register due to network card limitations");
			}
		}
		this.maximumTransferUnits = maximumTransferUnits.toArray(new MaximumTransferUnit[maximumTransferUnits.size()]);

		// Determine the highest maximum transfer unit
		int highestMaximumTransferUnit = Integer.MIN_VALUE;
		for (MaximumTransferUnit maximumTransferUnit : maximumTransferUnits) {
			if (maximumTransferUnit.getSize() > highestMaximumTransferUnit) {
				highestMaximumTransferUnit = maximumTransferUnit.getSize();
			}
		}
		this.highestMaximumTransferUnitSize = highestMaximumTransferUnit;
		if (foundTransferUnit == false) {
			throw new RuntimeException("No compatible maximum transfer unit found for machine network cards");
		}
		int[] registeredMaximumTransferUnitSizes = new int[maximumTransferUnits.size()];
		for (int i = 0; i < registeredMaximumTransferUnitSizes.length; i++) {
			registeredMaximumTransferUnitSizes[i] = this.maximumTransferUnits[i].getSize();
		}
		String registeredMaximumTransferUnitSizesStr = Arrays.toString(registeredMaximumTransferUnitSizes);
		log.debug("Set maximum transfer unit sizes to " + registeredMaximumTransferUnitSizesStr.substring(1,
				registeredMaximumTransferUnitSizesStr.length() - 1));
	}

	/**
	 * Returns the session of the server the client is currently connected to.
	 * 
	 * @return the session of the server the client is currently connected to,
	 *         <code>null</code> if it is not connected to a server.
	 */
	public final RakNetServerSession getServer() {
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
			return false; // No session
		}
		return session.getState() == RakNetState.CONNECTED;
	}

	/**
	 * Sends a Netty message over the channel raw. This should be used
	 * sparingly, as if it is used incorrectly it could break server sessions
	 * entirely. In order to send a message to a session, use one of the
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(Reliability, ByteBuf)
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
		log.debug("Sent netty message with size of " + buf.capacity() + " bytes (" + (buf.capacity() * 8) + " bits) to "
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
	 * {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(Reliability, int)
	 * sendMessage()} methods.
	 * 
	 * @param buffer
	 *            the packet ID to send.
	 * @param address
	 *            the address to send the packet to.
	 * @see {@link com.whirvis.jraknet.session.RakNetSession#sendMessage(Reliability, int)
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
	 * @param sender
	 *            the address of the sender.
	 * @param packet
	 *            the packet to handle.
	 */
	protected final void handleMessage(InetSocketAddress sender, RakNetPacket packet) {
		short packetId = packet.getId();
		if (preparation != null) {
			if (sender.equals(preparation.address)) {
				preparation.handleMessage(packet);
			}
		} else if (session != null) {
			if (sender.equals(session.getAddress())) {
				if (packetId >= RakNetPacket.ID_CUSTOM_0 && packetId <= RakNetPacket.ID_CUSTOM_F) {
					CustomPacket custom = new CustomPacket(packet);
					custom.decode();
					session.handleCustom(custom);
				} else if (packetId == RakNetPacket.ID_ACK || packetId == RakNetPacket.ID_NACK) {
					AcknowledgedPacket acknowledge = new AcknowledgedPacket(packet);
					acknowledge.decode();
					session.handleAcknowledge(acknowledge);
				}
			}
		}
		log.debug("Handled " + (RakNetPacket.hasPacket(packet.getId())
				? RakNetPacket.getName(packet.getId()) + " packet" : "packet with ID " + RakNet.toHexStringId(packet)));
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
				preparation.cancelReason = new NettyHandlerException(this, handler, address, cause);
			} else if (session != null) {
				this.disconnect(cause);
			}
		}
		log.debug("Handled exception " + cause.getClass().getName() + " caused by address " + address);
		this.callEvent(listener -> listener.onHandlerException(this, address, cause));
	}

	/**
	 * Connects the client to a server.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @throws NullPointerException
	 *             if the <code>address</code> or the IP address of the
	 *             <code>address</code> is <code>null</code>.
	 * @throws IllegalStateException
	 *             if the client is currently connected to a server.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(InetSocketAddress address)
			throws NullPointerException, IllegalStateException, RakNetException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		} else if (this.isConnected()) {
			throw new IllegalStateException("Client is currently connected to a server");
		} else if (listeners.isEmpty()) {
			log.warn("Client has no listeners");
		}

		// Initiate networking
		try {
			this.bootstrap = new Bootstrap();
			this.group = new NioEventLoopGroup();
			this.handler = new RakNetClientHandler(this);
			bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = (bindAddress != null ? bootstrap.bind(bindAddress) : bootstrap.bind(0)).sync().channel();
			this.bindAddress = (InetSocketAddress) channel.localAddress();
			this.setMaximumTransferUnitSizes(DEFAULT_TRANSFER_UNIT_SIZES);
			log.debug("Initialized networking");
		} catch (InterruptedException e) {
			throw new RakNetException(e);
		}

		// Prepare connection
		MaximumTransferUnit[] units = MaximumTransferUnit.sort(maximumTransferUnits);
		for (MaximumTransferUnit unit : maximumTransferUnits) {
			unit.reset();
		}
		this.preparation = new SessionPlanner(this, units[0].getSize(), highestMaximumTransferUnitSize);
		preparation.address = address;
		log.debug("Reset maximum transfer units and created session preparation");

		// Send open connection request one with a decreasing MTU
		int retriesLeft = 0;
		try {
			for (MaximumTransferUnit unit : units) {
				retriesLeft += unit.getRetries();
				while (unit.retry() > 0 && preparation.loginPackets[0] == false && preparation.cancelReason == null) {
					OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
					connectionRequestOne.maximumTransferUnit = unit.getSize();
					connectionRequestOne.networkProtocol = this.getProtocolVersion();
					connectionRequestOne.encode();
					this.sendNettyMessage(connectionRequestOne, address);
					log.debug("Attemped connection request one with maximum transfer unit size " + unit.getSize());
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
					log.debug("Attempted connection request two");
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

		// Finish connection
		if (preparation.readyForSession()) {
			this.session = preparation.createSession(channel);
			this.preparation = null;
			log.debug("Created session from session preparataion");

			// Send connection packet
			ConnectionRequest connectionRequest = new ConnectionRequest();
			connectionRequest.clientGuid = this.guid;
			connectionRequest.timestamp = (System.currentTimeMillis() - this.timestamp);
			connectionRequest.encode();
			session.sendMessage(Reliability.RELIABLE_ORDERED, connectionRequest);
			log.debug("Sent connection request to server");

			// Create and start session update thread
			RakNetClient client = this;
			this.sessionThread = new Thread("jraknet-session-thread-" + Long.toHexString(guid).toLowerCase()) {

				@Override
				public void run() {
					try {
						while (session != null && !this.isInterrupted()) {
							Thread.sleep(0, 1); // Lower CPU usage
							session.update();
						}
					} catch (InterruptedException e) {
						this.interrupt(); // Interrupted during sleep
					} catch (Throwable throwable) {
						client.callEvent(listener -> listener.onSessionException(client, throwable));
						client.disconnect(throwable);
					}
				}

			};
			sessionThread.start();
			log.debug("Created and started session update thread");
		} else {
			RakNetException cancelReason = preparation.cancelReason;
			this.preparation = null;
			this.session = null;
			throw cancelReason;
		}
		log.info("Connected to server with address " + address);
	}

	/**
	 * Connects the client to a servers.
	 * 
	 * @param address
	 *            the IP address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not within the range of
	 *             <code>0-65535</code>.
	 * @throws IllegalStateException
	 *             if the client is currently connected to a server.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(InetAddress address, int port)
			throws NullPointerException, IllegalArgumentException, IllegalStateException, RakNetException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		} else if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Invalid port range");
		}
		this.connect(new InetSocketAddress(address, port));
	}

	/**
	 * Connects the client to a server.
	 * 
	 * @param host
	 *            the IP address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not within the range of
	 *             <code>0-65535</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 * @throws IllegalStateException
	 *             if the client is currently connected to a server.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(String host, int port) throws NullPointerException, IllegalArgumentException,
			UnknownHostException, IllegalStateException, RakNetException {
		if (host == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		this.connect(InetAddress.getByName(host), port);
	}

	/**
	 * Connects the the client to the discovered server.
	 * 
	 * @param server
	 *            the discovered server to connect to.
	 * @throws NullPointerException
	 *             if the discovered server is <code>null</code>.
	 * @throws IllegalStateException
	 *             if the client is currently connected to a server.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(DiscoveredServer server)
			throws NullPointerException, IllegalStateException, RakNetException {
		if (server == null) {
			throw new NullPointerException("Discovered server cannot be null");
		}
		this.connect(server.getAddress());
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

		// Close session and interrupt thread
		session.closeConnection();
		sessionThread.interrupt();

		// Destroy session
		log.info("Disconnected from server with address " + session.getAddress() + " with reason \""
				+ (reason == null ? "Disconnected" : reason) + "\"");
		this.callEvent(listener -> listener.onDisconnect(this, session, reason == null ? "Disconnected" : reason));
		this.session = null;
		this.sessionThread = null;

		// Shutdown networking
		channel.close();
		group.shutdownGracefully(0L, 1000L, TimeUnit.MILLISECONDS);
		this.channel = null;
		this.handler = null;
		this.group = null;
		this.bootstrap = null;
		log.debug("Shutdown networking");
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
	public final void disconnect(Throwable reason) throws IllegalStateException {
		this.disconnect(reason != null ? RakNet.getStackTrace(reason) : null);
	}

	/**
	 * Disconnects the client from the server.
	 * 
	 * @throws IllegalStateException
	 *             if the client is not connected to a server.
	 */
	public final void disconnect() throws IllegalStateException {
		this.disconnect((String) /* Solves ambiguity */ null);
	}

	@Override
	public String toString() {
		return "RakNetClient [guid=" + guid + ", timestamp=" + timestamp + ", bindAddress=" + bindAddress
				+ ", maximumMaximumTransferUnitSize=" + highestMaximumTransferUnitSize + "]";
	}

}
