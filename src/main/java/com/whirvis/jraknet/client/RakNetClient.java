/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
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
import com.whirvis.jraknet.ThreadedListener;
import com.whirvis.jraknet.client.peer.PeerFactory;
import com.whirvis.jraknet.discovery.DiscoveredServer;
import com.whirvis.jraknet.peer.RakNetPeerMessenger;
import com.whirvis.jraknet.peer.RakNetServerPeer;
import com.whirvis.jraknet.peer.RakNetState;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.login.ConnectionRequest;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;

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
 * @since JRakNet v1.0.0
 * @see RakNetClientListener
 * @see #addListener(RakNetClientListener)
 * @see #connect(InetSocketAddress)
 */
public class RakNetClient implements RakNetPeerMessenger, RakNetClientListener {

	/**
	 * The default maximum transfer unit sizes used by the client.
	 * <p>
	 * These were chosen due to the maximum transfer unit sizes used by the
	 * Minecraft client during connection.
	 */
	public static final int[] DEFAULT_TRANSFER_UNIT_SIZES = new int[] { 1492, 1200, 576, RakNet.MINIMUM_MTU_SIZE };

	/**
	 * The amount of time to wait before the client broadcasts another ping to the
	 * local network and all added external servers.
	 * <p>
	 * This was also determined based on Minecraft's frequency of broadcasting pings
	 * to servers.
	 */
	public static final long PING_BROADCAST_WAIT_MILLIS = 1000L;

	private final InetSocketAddress bindingAddress;
	private final long guid;
	private final Logger log;
	private final long timestamp;
	private final ConcurrentLinkedQueue<RakNetClientListener> listeners;
	private int eventThreadCount;
	private InetSocketAddress serverAddress;
	private Bootstrap bootstrap;
	private RakNetClientHandler handler;
	private EventLoopGroup group;
	private Channel channel;
	private InetSocketAddress bindAddress;
	private MaximumTransferUnit[] maximumTransferUnits;
	private int highestMaximumTransferUnitSize;
	private PeerFactory peerFactory;
	private volatile RakNetServerPeer peer;
	private Thread peerThread;

	/**
	 * Creates a RakNet client.
	 * 
	 * @param address the address the client will bind to during connection. A
	 *                <code>null</code> address will have the client bind to the
	 *                wildcard address along with the client giving Netty the
	 *                responsibility of choosing which port to bind to.
	 */
	public RakNetClient(InetSocketAddress address) {
		this.bindingAddress = address;
		this.guid = UUID.randomUUID().getMostSignificantBits();
		this.log = LogManager
				.getLogger(RakNetClient.class.getSimpleName() + "-" + Long.toHexString(guid).toUpperCase());
		this.timestamp = System.currentTimeMillis();
		this.listeners = new ConcurrentLinkedQueue<RakNetClientListener>();
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param address the IP address the client will bind to during connection. A
	 *                <code>null</code> address will have the client bind to the
	 *                wildcard address.
	 * @param port    the port the client will bind to during connection. A port of
	 *                <code>0</code> will have the client give Netty the
	 *                respsonsibility of choosing the port to bind to.
	 */
	public RakNetClient(InetAddress address, int port) {
		this(new InetSocketAddress(address, port));
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param address the IP address the client will bind to during connection. A
	 *                <code>null</code> address will have the client bind to the
	 *                wildcard address.
	 */
	public RakNetClient(InetAddress address) {
		this(new InetSocketAddress(address, 0));
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param host the IP address the client will bind to during connection. A
	 *             <code>null</code> address will have the client bind to the
	 *             wildcard address.
	 * @param port the port the client will bind to during connection. A port of
	 *             <code>0</code> will have the client give Netty the
	 *             respsonsibility of choosing the port to bind to.
	 * @throws UnknownHostException if no IP address for the <code>host</code> could
	 *                              be found, or if a scope_id was specified for a
	 *                              global IPv6 address.
	 */
	public RakNetClient(String host, int port) throws UnknownHostException {
		this(InetAddress.getByName(host), port);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param host the IP address the client will bind to during connection. A
	 *             <code>null</code> address will have the client bind to the
	 *             wildcard address.
	 * @throws UnknownHostException if no IP address for the <code>host</code> could
	 *                              be found, or if a scope_id was specified for a
	 *                              global IPv6 address.
	 */
	public RakNetClient(String host) throws UnknownHostException {
		this(host, 0);
	}

	/**
	 * Creates a RakNet client.
	 * 
	 * @param port the port the client will bind to during creation. A port of
	 *             <code>0</code> will have the client give Netty the
	 *             respsonsibility of choosing the port to bind to.
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
		return System.currentTimeMillis() - timestamp;
	}

	/**
	 * Adds a {@link RakNetClientListener} to the client.
	 * <p>
	 * Listeners are used to listen for events that occur relating to the client
	 * such as connecting to discovers, discovering local servers, and more.
	 * 
	 * @param listener the listener to add.
	 * @return the client.
	 * @throws NullPointerException     if the <code>listener</code> is
	 *                                  <code>null</code>.
	 * @throws IllegalArgumentException if the <code>listener</code> is another
	 *                                  client that is not the client itself.
	 */
	public final RakNetClient addListener(RakNetClientListener listener)
			throws NullPointerException, IllegalArgumentException {
		if (listener == null) {
			throw new NullPointerException("Listener cannot be null");
		} else if (listener instanceof RakNetClient && !this.equals(listener)) {
			throw new IllegalArgumentException("A client cannot be used as a listener except for itself");
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
	 * @param listener the listener to remove.
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
	 * @param event the event to call.
	 * @throws NullPointerException if the <code>event</code> is <code>null</code>.
	 * @see RakNetClientListener
	 */
	public final void callEvent(Consumer<? super RakNetClientListener> event) throws NullPointerException {
		if (event == null) {
			throw new NullPointerException("Event cannot be null");
		}
		for (RakNetClientListener listener : listeners) {
			if (listener.getClass().isAnnotationPresent(ThreadedListener.class)) {
				ThreadedListener threadedListener = listener.getClass().getAnnotation(ThreadedListener.class);
				new Thread(RakNetClient.class.getSimpleName() + (threadedListener.name().length() > 0 ? "-" : "")
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
	 * Returns whether or not the client is currently running.
	 * <p>
	 * If it is running, this means that it is currently connecting to or is
	 * connected to a server.
	 * 
	 * @return <code>true</code> if the client is running, <code>false</code>
	 *         otherwise.
	 */
	public final boolean isRunning() {
		if (channel == null) {
			return false; // No channel to check
		}
		return channel.isOpen();
	}

	/**
	 * Returns the address of the server the client is connecting to or is connected
	 * to.
	 * 
	 * @return the address of the server the client is connecting to or is connected
	 *         to, <code>null</code> if the client is disconnected.
	 */
	public InetSocketAddress getServerAddress() {
		return this.serverAddress;
	}

	/**
	 * Returns the address the client is bound to.
	 * <p>
	 * This will be the value supplied during client creation until the client has
	 * connected to a server using the {@link #connect(InetSocketAddress)} method.
	 * Once the client has connected a server, the bind address will be changed to
	 * the address returned from the channel's {@link Channel#localAddress()}
	 * method.
	 * 
	 * @return the address the client is bound to.
	 */
	public InetSocketAddress getAddress() {
		return this.bindAddress;
	}

	/**
	 * Returns the IP address the client is bound to based on the address returned
	 * from {@link #getAddress()}.
	 * 
	 * @return the IP address the client is bound to.
	 */
	public InetAddress getInetAddress() {
		return bindAddress.getAddress();
	}

	/**
	 * Returns the port the client is bound to based on the address returned from
	 * {@link #getAddress()}.
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
	 * Sets the maximum transfer unit sizes that will be used by the client during
	 * connection.
	 * 
	 * @param maximumTransferUnitSizes the maximum transfer unit sizes.
	 * @throws NullPointerException     if the <code>maximumTransferUnitSizes</code>
	 *                                  is <code>null</code>.
	 * @throws IllegalArgumentException if the <code>maximumTransferUnitSizes</code>
	 *                                  is empty or one of its values is less than
	 *                                  {@value RakNet#MINIMUM_MTU_SIZE}.
	 * @throws RuntimeException         if determining the maximum transfer unit for
	 *                                  the network card with the client's bind
	 *                                  address was a failure or no valid maximum
	 *                                  transfer unit could be located for the
	 *                                  network card that the client's binding
	 *                                  address is bound to.
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
			throw new RuntimeException("Failed to determine maximum transfer unit"
					+ (bindAddress.getAddress() != null ? " for network card with address " + bindAddress.getAddress()
							: ""));
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
	 * Returns the peer of the server the client is currently connected to.
	 * 
	 * @return the peer of the server the client is currently connected to,
	 *         <code>null</code> if it is not connected to a server.
	 */
	public final RakNetServerPeer getServer() {
		return this.peer;
	}

	/**
	 * Returns whether or not the client is connected.
	 * <p>
	 * The client is considered connected if the current state is
	 * {@link RakNetState#CONNECTED} or has a higher order. This does not apply to
	 * the {@link #isHandshaking()}, {@link #isLoggedIn()}, or
	 * {@link #isDisconnected()} methods.
	 * 
	 * @return <code>true</code> if the client is connected, <code>false</code>
	 *         otherwise.
	 */
	public boolean isConnected() {
		if (peer == null) {
			return false; // No peer
		}
		return peer.isConnected();
	}

	/**
	 * Returns whether or not the client is handshaking.
	 * 
	 * @return <code>true</code> if the client is handshaking, <code>false</code>
	 *         otherwise.
	 */
	public boolean isHandshaking() {
		if (peer == null) {
			return false; // No peer
		}
		return peer.isHandshaking();
	}

	/**
	 * Returns whether or not the client is logged in.
	 * 
	 * @return <code>true</code> if the client is logged in, <code>false</code>
	 *         otherwise.
	 */
	public boolean isLoggedIn() {
		if (peer == null) {
			return false; // No peer
		}
		return peer.isLoggedIn();
	}

	/**
	 * Returns whether or not the client is disconnected.
	 * 
	 * @return <code>true</code> if the client is disconnected, <code>false</code>
	 *         otherwise.
	 */
	public boolean isDisconnected() {
		if (peer == null) {
			return true; // No peer
		}
		return peer.isDisconnected();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalStateException if the client is not connected to a server.
	 */
	@Override
	public final EncapsulatedPacket sendMessage(Reliability reliability, int channel, Packet packet)
			throws IllegalStateException {
		if (!this.isConnected()) {
			throw new IllegalStateException("Cannot send messages while not connected to a server");
		}
		return peer.sendMessage(reliability, channel, packet);
	}

	/**
	 * Sends a Netty message over the channel raw.
	 * <p>
	 * This should be used sparingly, as if it is used incorrectly it could break
	 * server peers entirely. In order to send a message to a peer, use one of the
	 * {@link com.whirvis.jraknet.peer.RakNetPeer#sendMessage(Reliability, ByteBuf)
	 * sendMessage()} methods.
	 * 
	 * @param buf     the buffer to send.
	 * @param address the address to send the buffer to.
	 * @throws NullPointerException if the <code>buf</code>, <code>address</code>,
	 *                              or IP address of <code>address</code> are
	 *                              <code>null</code>.
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
	 * This should be used sparingly, as if it is used incorrectly it could break
	 * server peers entirely. In order to send a message to a peer, use one of the
	 * {@link com.whirvis.jraknet.peer.RakNetPeer#sendMessage(Reliability, Packet)
	 * sendMessage()} methods.
	 * 
	 * @param packet  the packet to send.
	 * @param address the address to send the packet to.
	 * @throws NullPointerException if the <code>packet</code>,
	 *                              <code>address</code>, or IP address of
	 *                              <code>address</code> are <code>null</code>.
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
	 * This should be used sparingly, as if it is used incorrectly it could break
	 * server peers entirely. In order to send a message to a peer, use one of the
	 * {@link com.whirvis.jraknet.peer.RakNetPeer#sendMessage(Reliability, int)
	 * sendMessage()} methods.
	 * 
	 * @param packetId the packet ID to send.
	 * @param address  the address to send the packet to.
	 * @throws NullPointerException if the <code>address</code> or IP address of
	 *                              <code>address</code> are <code>null</code>.
	 */
	public final void sendNettyMessage(int packetId, InetSocketAddress address) throws NullPointerException {
		this.sendNettyMessage(new RakNetPacket(packetId), address);
	}

	/**
	 * Handles a packet received by the {@link RakNetClientHandler}.
	 * 
	 * @param sender the address of the sender.
	 * @param packet the packet to handle.
	 * @throws NullPointerException if the <code>sender</code> or
	 *                              <code>packet</code> are <code>null</code>.
	 */
	protected final void handleMessage(InetSocketAddress sender, RakNetPacket packet) throws NullPointerException {
		if (sender == null) {
			throw new NullPointerException("Sender cannot be null");
		} else if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		} else if (peerFactory != null) {
			if (sender.equals(peerFactory.getAddress())) {
				RakNetServerPeer peer = peerFactory.assemble(packet);
				if (peer != null) {
					this.peer = peer;
					this.peerFactory = null;
				}
			}
		} else if (peer != null) {
			peer.handleInternal(packet);
		}
		log.debug("Handled " + RakNetPacket.getName(packet.getId()) + " packet");
	}

	/**
	 * Called by the {@link com.whirvis.jraknet.client.RakNetClientHandler
	 * RakNetClientHander} when it catches a <code>Throwable</code> while handling a
	 * packet.
	 * 
	 * @param address the address that caused the exception.
	 * @param cause   the <code>Throwable</code> caught by the handler.
	 * @throws NullPointerException if the cause <code>address</code> or
	 *                              <code>cause</code> are <code>null</code>.
	 */
	protected final void handleHandlerException(InetSocketAddress address, Throwable cause)
			throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (cause == null) {
			throw new NullPointerException("Cause cannot be null");
		} else if (peerFactory != null) {
			if (address.equals(peerFactory.getAddress())) {
				peerFactory.exceptionCaught(new NettyHandlerException(this, handler, address, cause));
			}
		} else if (peer != null) {
			if (address.equals(peer.getAddress())) {
				this.disconnect(cause);
			}
		}
		log.debug("Handled exception " + cause.getClass().getName() + " caused by address " + address);
		this.callEvent(listener -> listener.onHandlerException(this, address, cause));
	}

	/**
	 * Connects the client to a server.
	 * 
	 * @param address the address of the server to connect to.
	 * @throws NullPointerException  if the <code>address</code> or the IP address
	 *                               of the <code>address</code> is
	 *                               <code>null</code>.
	 * @throws IllegalStateException if the client is currently connected to a
	 *                               server.
	 * @throws RakNetException       if an error occurs during connection or login.
	 */
	public void connect(InetSocketAddress address) throws NullPointerException, IllegalStateException, RakNetException {
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
		this.serverAddress = address;
		try {
			this.bootstrap = new Bootstrap();
			this.group = new NioEventLoopGroup();
			this.handler = new RakNetClientHandler(this);
			bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = (bindingAddress != null ? bootstrap.bind(bindingAddress) : bootstrap.bind(0)).sync()
					.channel();
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
			log.debug("Reset maximum transfer unit with size of " + unit.getSize() + " bytes (" + (unit.getSize() * 8)
					+ " bits)");
		}
		this.peerFactory = new PeerFactory(this, address, bootstrap, channel, units[0].getSize(),
				highestMaximumTransferUnitSize);
		log.debug("Reset maximum transfer units and created peer peerFactory");
		peerFactory.startAssembly(units);

		// Send connection packet
		ConnectionRequest connectionRequest = new ConnectionRequest();
		connectionRequest.clientGuid = this.guid;
		connectionRequest.timestamp = System.currentTimeMillis() - timestamp;
		connectionRequest.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, connectionRequest);
		log.debug("Sent connection request to server");

		// Create and start peer update thread
		RakNetClient client = this;
		this.peerThread = new Thread(
				RakNetClient.class.getSimpleName() + "-Peer-Thread-" + Long.toHexString(guid).toUpperCase()) {

			@Override
			public void run() {
				while (peer != null && !this.isInterrupted()) {
					try {
						Thread.sleep(0, 1); // Lower CPU usage
					} catch (InterruptedException e) {
						this.interrupt(); // Interrupted during sleep
						continue;
					}
					if (peer != null) {
						if (!peer.isDisconnected()) {
							try {
								peer.update();
							} catch (Throwable throwable) {
								client.callEvent(listener -> listener.onPeerException(client, peer, throwable));
								if (!peer.isDisconnected()) {
									client.disconnect(throwable);
								}
							}
						}
					}
				}
			}

		};
		peerThread.start();
		log.debug("Created and started peer update thread");
		log.info("Connected to server with address " + address);
	}

	/**
	 * Connects the client to a server.
	 * 
	 * @param address the IP address of the server to connect to.
	 * @param port    the port of the server to connect to.
	 * @throws NullPointerException     if the <code>address</code> is
	 *                                  <code>null</code>.
	 * @throws IllegalArgumentException if the <code>port</code> is not in between
	 *                                  <code>0-65535</code>.
	 * @throws IllegalStateException    if the client is currently connected to a
	 *                                  server.
	 * @throws RakNetException          if an error occurs during connection or
	 *                                  login.
	 */
	public final void connect(InetAddress address, int port)
			throws NullPointerException, IllegalArgumentException, IllegalStateException, RakNetException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		} else if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Port must be in between 0-65535");
		}
		this.connect(new InetSocketAddress(address, port));
	}

	/**
	 * Connects the client to a server.
	 * 
	 * @param host the IP address of the server to connect to.
	 * @param port the port of the server to connect to.
	 * @throws NullPointerException     if the <code>host</code> is
	 *                                  <code>null</code>.
	 * @throws IllegalArgumentException if the <code>port</code> is not within the
	 *                                  range of <code>0-65535</code>.
	 * @throws UnknownHostException     if no IP address for the <code>host</code>
	 *                                  could be found, or if a scope_id was
	 *                                  specified for a global IPv6 address.
	 * @throws IllegalStateException    if the client is currently connected to a
	 *                                  server.
	 * @throws RakNetException          if an error occurs during connection or
	 *                                  login.
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
	 * @param server the discovered server to connect to.
	 * @throws NullPointerException  if the discovered <code>server</code> is
	 *                               <code>null</code>.
	 * @throws IllegalStateException if the client is currently connected to a
	 *                               server.
	 * @throws RakNetException       if an error occurs during connection or login.
	 */
	public final void connect(DiscoveredServer server)
			throws NullPointerException, IllegalStateException, RakNetException {
		if (server == null) {
			throw new NullPointerException("Discovered server cannot be null");
		}
		this.connect(server.getAddress());
	}

	/**
	 * Disconnects the client from the server.
	 * 
	 * @param reason the reason for disconnection. A <code>null</code> reason will
	 *               have <code>"Disconnected"</code> be used as the reason instead.
	 * @throws IllegalStateException if the client is not connected to a server.
	 */
	public void disconnect(String reason) throws IllegalStateException {
		if (peer == null) {
			throw new IllegalStateException("Client is not connected to a server");
		}

		// Disconnect peer and interrupt thread
		peerThread.interrupt();
		this.peerThread = null;
		RakNetServerPeer peer = this.peer;
		if (!peer.isDisconnected()) {
			peer.disconnect();
			this.peer = null;
		}
		log.info("Disconnected from server with address " + peer.getAddress() + " with reason \""
				+ (reason == null ? "Disconnected" : reason) + "\"");
		this.callEvent(
				listener -> listener.onDisconnect(this, serverAddress, peer, reason == null ? "Disconnected" : reason));

		// Shutdown networking
		channel.close();
		group.shutdownGracefully(0L, 1000L, TimeUnit.MILLISECONDS);
		this.serverAddress = null;
		this.channel = null;
		this.handler = null;
		this.group = null;
		this.bootstrap = null;
		log.debug("Shutdown networking");
	}

	/**
	 * Disconnects the client from the server.
	 * 
	 * @param reason the reason for disconnection. A <code>null</code> reason will
	 *               have <code>"Disconnected"</code> be used as the reason instead.
	 * @throws IllegalStateException if the client is not connected to a server.
	 */
	public final void disconnect(Throwable reason) throws IllegalStateException {
		this.disconnect(reason != null ? RakNet.getStackTrace(reason) : null);
	}

	/**
	 * Disconnects the client from the server.
	 * 
	 * @throws IllegalStateException if the client is not connected to a server.
	 */
	public final void disconnect() throws IllegalStateException {
		this.disconnect((String) /* Solves ambiguity */ null);
	}

	@Override
	public String toString() {
		return "RakNetClient [bindingAddress=" + bindingAddress + ", guid=" + guid + ", timestamp=" + timestamp
				+ ", bindAddress=" + bindAddress + ", maximumTransferUnits=" + Arrays.toString(maximumTransferUnits)
				+ ", highestMaximumTransferUnitSize=" + highestMaximumTransferUnitSize + ", getProtocolVersion()="
				+ getProtocolVersion() + ", getTimestamp()=" + getTimestamp() + ", getAddress()=" + getAddress()
				+ ", isConnected()=" + isConnected() + "]";
	}

}
