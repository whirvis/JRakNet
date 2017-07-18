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
 * Copyright (c) 2016, 2017 Trent "MarfGamer" Summerlin
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
package net.marfgamer.jraknet.client;

import static net.marfgamer.jraknet.protocol.MessageIdentifier.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.jraknet.NoListenerException;
import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetException;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.client.discovery.DiscoveredServer;
import net.marfgamer.jraknet.client.discovery.DiscoveryMode;
import net.marfgamer.jraknet.client.discovery.DiscoveryThread;
import net.marfgamer.jraknet.protocol.Reliability;
import net.marfgamer.jraknet.protocol.login.ConnectionRequest;
import net.marfgamer.jraknet.protocol.login.OpenConnectionRequestOne;
import net.marfgamer.jraknet.protocol.login.OpenConnectionRequestTwo;
import net.marfgamer.jraknet.protocol.message.CustomPacket;
import net.marfgamer.jraknet.protocol.message.acknowledge.Acknowledge;
import net.marfgamer.jraknet.protocol.status.UnconnectedPing;
import net.marfgamer.jraknet.protocol.status.UnconnectedPingOpenConnections;
import net.marfgamer.jraknet.protocol.status.UnconnectedPong;
import net.marfgamer.jraknet.session.RakNetServerSession;
import net.marfgamer.jraknet.session.RakNetState;
import net.marfgamer.jraknet.session.UnumRakNetPeer;
import net.marfgamer.jraknet.util.RakNetUtils;
import net.marfgamer.jraknet.util.map.IntMap;

/**
 * Used to connect to servers using the RakNet protocol.
 *
 * @author Trent "MarfGamer" Summerlin
 */
public class RakNetClient implements UnumRakNetPeer, RakNetClientListener {

	// Used to discover systems without relying on the main thread
	private static DiscoveryThread discoverySystem = new DiscoveryThread();

	// Client data
	private final long guid;
	private final long timestamp;
	private int discoveryPort;
	private DiscoveryMode discoveryMode;
	/** synchronize this first! (<code>externalServers</code> goes second!) */
	private final ConcurrentHashMap<InetSocketAddress, DiscoveredServer> discovered;
	/** synchronize this second! (<code>discovered</code> goes first!) */
	private final ConcurrentHashMap<InetSocketAddress, DiscoveredServer> externalServers;

	// Networking data
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetClientHandler handler;
	private final IntMap<MaximumTransferUnit> maximumTransferUnits;

	// Session management
	private Channel channel;
	private SessionPreparation preparation;
	private volatile RakNetServerSession session;
	private volatile RakNetClientListener listener;

	/**
	 * Constructs a <code>RakNetClient</code> with the specified
	 * <code>DiscoveryMode</code> and server discovery port.
	 * 
	 * @param discoveryMode
	 *            how the client will discover servers. If this is set to
	 *            <code>null</code>, the client will enable set it to
	 *            <code>DiscoveryMode.ALL_CONNECTIONS</code> as long as the port
	 *            is greater than -1.
	 * @param discoveryPort
	 *            the port the client will attempt to discover servers on.
	 */
	public RakNetClient(DiscoveryMode discoveryMode, int discoveryPort) {
		// Set client data
		this.guid = new Random().nextLong();
		this.timestamp = System.currentTimeMillis();

		// Set discovery data
		this.discoveryPort = discoveryPort;
		this.discoveryMode = discoveryMode;
		if (discoveryMode == null) {
			this.discoveryMode = (discoveryPort > -1 ? DiscoveryMode.ALL_CONNECTIONS : DiscoveryMode.NONE);
		}
		this.discovered = new ConcurrentHashMap<InetSocketAddress, DiscoveredServer>();
		this.externalServers = new ConcurrentHashMap<InetSocketAddress, DiscoveredServer>();

		// Set networking data
		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetClientHandler(this);

		// Add maximum transfer units
		this.maximumTransferUnits = new IntMap<MaximumTransferUnit>();
		this.addMaximumTransferUnit(new MaximumTransferUnit(RakNetUtils.getMaximumTransferUnit(), 2));
		this.addMaximumTransferUnit(new MaximumTransferUnit(1464, 3));
		this.addMaximumTransferUnit(new MaximumTransferUnit(1172, 4));
		this.addMaximumTransferUnit(new MaximumTransferUnit(RakNet.MINIMUM_TRANSFER_UNIT, 5));

		// Initiate bootstrap data
		try {
			bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = bootstrap.bind(0).sync().channel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructs a <code>RakNetClient</code> with the specified server
	 * discovery port with the <code>DiscoveryMode</code> set to
	 * <code>DiscoveryMode.ALL_CONNECTIONS</code>.
	 * 
	 * @param discoveryPort
	 *            the port the client will attempt to discover servers on.
	 */
	public RakNetClient(int discoveryPort) {
		this(null, discoveryPort);
	}

	/**
	 * Constructs a <code>RakNetClient</code> with the
	 * <code>DiscoveryMode</code> set to <code>DiscoveryMode.NONE</code> and the
	 * discovery port set to -1.
	 */
	public RakNetClient() {
		this(-1);
	}

	/**
	 * @return the client's networking protocol version.
	 */
	public final int getProtocolVersion() {
		return RakNet.CLIENT_NETWORK_PROTOCOL;
	}

	/**
	 * @return the client's globally unique ID.
	 */
	public final long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * @return the client's timestamp.
	 */
	public final long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	/**
	 * @return the client's discovery port.
	 */
	public final int getDiscoveryPort() {
		return this.discoveryPort;
	}

	/**
	 * Sets the client's discovery port.
	 * 
	 * @param discoveryPort
	 *            the new discovery port.
	 */
	public final void setDiscoveryPort(int discoveryPort) {
		this.discoveryPort = discoveryPort;
	}

	/**
	 * @return the client's discovery mode.
	 */
	public final DiscoveryMode getDiscoveryMode() {
		return this.discoveryMode;
	}

	/**
	 * Sets the client's discovery mode.
	 * 
	 * @param mode
	 *            how the client will discover servers on the local network.
	 */
	public final void setDiscoveryMode(DiscoveryMode mode) {
		if (listener == null) {
			throw new NoListenerException();
		}
		this.discoveryMode = (mode != null ? mode : DiscoveryMode.NONE);
		synchronized (discovered) {
			if (this.discoveryMode == DiscoveryMode.NONE) {
				for (InetSocketAddress address : discovered.keySet()) {
					listener.onServerForgotten(address);
				}
				discovered.clear(); // We are not discovering servers anymore!
			}
		}
	}

	/**
	 * Adds a server to the client's external server discovery list. This
	 * functions like the normal discovery system but is not affected by the
	 * <code>DiscoveryMode</code> or discovery port set for the client.
	 * 
	 * @param address
	 *            the server address.
	 */
	public final void addExternalServer(InetSocketAddress address) {
		synchronized (externalServers) {
			if (!externalServers.contains(address)) {
				externalServers.put(address, new DiscoveredServer(address, -1, null));
				listener.onExternalServerAdded(address);
			}
		}
	}

	/**
	 * Adds a server to the client's external server discovery list. This
	 * functions like the normal discovery system but is not affected by the
	 * <code>DiscoveryMode</code> or discovery port set for the client.
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
	 * Adds a server to the client's external server discovery list. This
	 * functions like the normal discovery system but is not affected by the
	 * <code>DiscoveryMode</code> or discovery port set for the client.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 * @throws UnknownHostException
	 *             if the specified address is an unknown host.
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
		synchronized (externalServers) {
			if (externalServers.contains(address)) {
				externalServers.remove(address);
				listener.onExternalServerRemoved(address);
			}
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
	 *             if the specified address is an unknown host.
	 */
	public final void removeExternalServer(String address, int port) throws UnknownHostException {
		this.removeExternalServer(InetAddress.getByName(address), port);
	}

	/**
	 * @return the external servers as an array.
	 */
	public final DiscoveredServer[] getExternalServers() {
		synchronized (externalServers) {
			return externalServers.values().toArray(new DiscoveredServer[externalServers.size()]);
		}
	}

	/**
	 * Adds a <code>MaximumTransferUnit</code> that can be used by the client
	 * during connection.
	 * 
	 * @param maximumTransferUnit
	 *            the maximum transfer unit.
	 */
	public final void addMaximumTransferUnit(MaximumTransferUnit maximumTransferUnit) {
		maximumTransferUnits.put(maximumTransferUnit.getMaximumTransferUnit(), maximumTransferUnit);
	}

	/**
	 * Adds a <code>MaximumTransferUnit</code> that can be used by the client
	 * during connection.
	 * 
	 * @param maximumTransferUnit
	 *            the maximum transfer unit.
	 * @param retries
	 *            the amount of retries before the client moves on to the lower
	 *            maximum transfer unit.
	 */
	public final void addMaximumTransferUnit(int maximumTransferUnit, int retries) {
		this.addMaximumTransferUnit(new MaximumTransferUnit(maximumTransferUnit, retries));
	}

	/**
	 * Removes a <code>MaximumTransferUnit</code> that was being used by the
	 * client based on it's maximum transfer unit.
	 * 
	 * @param maximumTransferUnit
	 *            the maximum transfer unit to remove.
	 */
	public final void removeMaximumTransferUnit(int maximumTransferUnit) {
		maximumTransferUnits.remove(maximumTransferUnit);
	}

	/**
	 * Removes a <code>MaximumTransferUnit</code> that was being used by the
	 * client.
	 * 
	 * @param maximumTransferUnit
	 *            the maximum transfer unit to remove.
	 */
	public final void removeMaximumTransferUnit(MaximumTransferUnit maximumTransferUnit) {
		this.removeMaximumTransferUnit(maximumTransferUnit);
	}

	/**
	 * @return the <code>MaximumTransferUnit</code>'s the client uses during
	 *         login.
	 */
	public final MaximumTransferUnit[] getMaximumTransferUnits() {
		return maximumTransferUnits.values().toArray(new MaximumTransferUnit[maximumTransferUnits.size()]);
	}

	/**
	 * @return the session the client is connected to.
	 */
	public final RakNetServerSession getSession() {
		return this.session;
	}

	/**
	 * @return the client's listener.
	 */
	public final RakNetClientListener getListener() {
		return this.listener;
	}

	/**
	 * Sets the client's listener.
	 * 
	 * @param listener
	 *            the client's new listener.
	 * @return the client.
	 */
	public final RakNetClient setListener(RakNetClientListener listener) {
		// Set listener
		if (listener == null) {
			throw new NullPointerException("Listener must not be null!");
		}
		this.listener = listener;

		// Initiate discovery system if it is not yet started
		if (discoverySystem.isRunning() == false) {
			discoverySystem.start();
		}
		discoverySystem.addClient(this);

		return this;
	}

	/**
	 * Sets the client's listener to itself, normally used for when a client is
	 * an all-in-one class
	 * 
	 * @return the client.
	 */
	public final RakNetClient setListenerSelf() {
		return this.setListener(this);
	}

	/**
	 * @return true if the client is connected.
	 */
	public final boolean isConnected() {
		if (session != null) {
			return (session.getState() == RakNetState.CONNECTED);
		}
		return false;
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
		listener.onHandlerException(address, cause);
		if (preparation != null) {
			if (address.equals(preparation.address)) {
				preparation.cancelReason = new NettyHandlerException(this, handler, cause);
			}
		} else {
			if (session != null) {
				if (address.equals(preparation.address)) {
					this.disconnect(cause.getClass().getName() + ": " + cause.getLocalizedMessage());
				}
			}
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
	public final void handleMessage(RakNetPacket packet, InetSocketAddress sender) {
		short packetId = packet.getId();

		// This packet has to do with server discovery so it isn't handled here
		if (packetId == ID_UNCONNECTED_PONG) {
			UnconnectedPong pong = new UnconnectedPong(packet);
			pong.decode();
			if (pong.identifier != null) {
				this.updateDiscoveryData(sender, pong);
			}
		}

		// Are we still logging in?
		if (preparation != null) {
			if (sender.equals(preparation.address)) {
				preparation.handleMessage(packet);
				return;
			}
		}

		// Only handle these from the server we're connected to!
		if (session != null) {
			if (sender.equals(session.getAddress())) {
				if (packetId >= ID_RESERVED_3 && packetId <= ID_RESERVED_9) {
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
	}

	/**
	 * Sends a raw message to the specified address. Be careful when using this
	 * method, because if it is used incorrectly it could break server sessions
	 * entirely! If you are wanting to send a message to a session, you are
	 * probably looking for the
	 * {@link net.marfgamer.jraknet.session.RakNetSession#sendMessage(net.marfgamer.jraknet.protocol.Reliability, net.marfgamer.jraknet.Packet)
	 * sendMessage} method.
	 * 
	 * @param buf
	 *            the buffer to send.
	 * @param address
	 *            the address to send the buffer to.
	 */
	public final void sendNettyMessage(ByteBuf buf, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(buf, address));
	}

	/**
	 * Sends a raw message to the specified address. Be careful when using this
	 * method, because if it is used incorrectly it could break server sessions
	 * entirely! If you are wanting to send a message to a session, you are
	 * probably looking for the
	 * {@link net.marfgamer.jraknet.session.RakNetSession#sendMessage(net.marfgamer.jraknet.protocol.Reliability, net.marfgamer.jraknet.Packet)
	 * sendMessage} method.
	 * 
	 * @param packet
	 *            the packet to send.
	 * @param address
	 *            the address to send the packet to.
	 */
	public final void sendNettyMessage(Packet packet, InetSocketAddress address) {
		this.sendNettyMessage(packet.buffer(), address);
	}

	/**
	 * Sends a raw message to the specified address. Be careful when using this
	 * method, because if it is used incorrectly it could break server sessions
	 * entirely! If you are wanting to send a message to a session, you are
	 * probably looking for the
	 * {@link net.marfgamer.jraknet.session.RakNetSession#sendMessage(net.marfgamer.jraknet.protocol.Reliability, int)
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
	 * Updates the discovery data in the client by sending pings and removing
	 * servers that have taken too long to respond to a ping.
	 */
	public final void updateDiscoveryData() {
		// Remove all servers that have timed out
		synchronized (discovered) {
			synchronized (externalServers) {
				ArrayList<InetSocketAddress> forgottenServers = new ArrayList<InetSocketAddress>();
				for (InetSocketAddress discoveredServerAddress : discovered.keySet()) {
					DiscoveredServer discoveredServer = discovered.get(discoveredServerAddress);
					if (System.currentTimeMillis()
							- discoveredServer.getDiscoveryTimestamp() >= DiscoveredServer.SERVER_TIMEOUT_MILLI) {
						forgottenServers.add(discoveredServerAddress);
						listener.onServerForgotten(discoveredServerAddress);
					}
				}
				discovered.keySet().removeAll(forgottenServers);

				// Broadcast ping to local network
				if (discoveryMode != DiscoveryMode.NONE && discoveryPort > -1) {
					UnconnectedPing ping = new UnconnectedPing();
					if (discoveryMode == DiscoveryMode.OPEN_CONNECTIONS) {
						ping = new UnconnectedPingOpenConnections();
					}
					ping.timestamp = this.getTimestamp();
					ping.encode();

					this.sendNettyMessage(ping, new InetSocketAddress("255.255.255.255", discoveryPort));
				}

				// Send ping to external servers
				synchronized (externalServers) {
					if (!externalServers.isEmpty()) {
						UnconnectedPing ping = new UnconnectedPing();
						ping.timestamp = this.getTimestamp();
						ping.encode();

						for (InetSocketAddress externalAddress : externalServers.keySet()) {
							this.sendNettyMessage(ping, externalAddress);
						}
					}
				}
			}
		}
	}

	/**
	 * This method handles the specified <code>UnconnectedPong</code> packet and
	 * updates the discovery data accordingly.
	 * 
	 * @param sender
	 *            the sender of the <code>UnconnectedPong</code> packet.
	 * @param pong
	 *            the <code>UnconnectedPong</code> packet to handle.
	 */
	public final void updateDiscoveryData(InetSocketAddress sender, UnconnectedPong pong) {
		// Is this a local or an external server?
		synchronized (discovered) {
			synchronized (externalServers) {
				if (sender.getAddress().isSiteLocalAddress() && !externalServers.containsKey(sender)) {
					// This is a local server
					if (!discovered.containsKey(sender)) {
						// Server discovered
						discovered.put(sender,
								new DiscoveredServer(sender, System.currentTimeMillis(), pong.identifier));
						listener.onServerDiscovered(sender, pong.identifier);
					} else {
						// Server already discovered, but data has changed
						DiscoveredServer server = discovered.get(sender);
						server.setDiscoveryTimestamp(System.currentTimeMillis());
						if (!pong.identifier.equals(server.getIdentifier())) {
							server.setIdentifier(pong.identifier);
							listener.onServerIdentifierUpdate(sender, pong.identifier);
						}
					}
				} else {
					// This is an external server
					if (externalServers.containsKey(sender)) {
						DiscoveredServer server = externalServers.get(sender);
						server.setDiscoveryTimestamp(System.currentTimeMillis());
						if (!pong.identifier.equals(server.getIdentifier())) {
							server.setIdentifier(pong.identifier);
							listener.onExternalServerIdentifierUpdate(sender, pong.identifier);
						}
					}
				}
			}
		}
	}

	/**
	 * Connects the client to a server with the specified address.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(InetSocketAddress address) throws RakNetException {
		// Make sure we have a listener
		if (this.listener == null) {
			throw new NoListenerException();
		}

		// Reset client data
		if (this.isConnected()) {
			this.disconnect("Disconnected");
		}
		MaximumTransferUnit[] units = MaximumTransferUnit.sort(this.getMaximumTransferUnits());
		this.preparation = new SessionPreparation(this, units[0].getMaximumTransferUnit());
		preparation.address = address;

		// Send OPEN_CONNECTION_REQUEST_ONE with a decreasing MTU
		int retriesLeft = 0;
		for (MaximumTransferUnit unit : units) {
			retriesLeft += unit.getRetries();
			while (unit.retry() > 0 && preparation.loginPackets[0] == false && preparation.cancelReason == null) {
				OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
				connectionRequestOne.maximumTransferUnit = unit.getMaximumTransferUnit();
				connectionRequestOne.protocolVersion = this.getProtocolVersion();
				connectionRequestOne.encode();
				this.sendNettyMessage(connectionRequestOne, address);

				RakNetUtils.threadLock(500);
			}
		}

		// Reset MaximumTransferUnit's so they can be used again
		for (MaximumTransferUnit unit : maximumTransferUnits.values()) {
			unit.reset();
		}

		// If the server didn't respond then it is offline
		if (preparation.loginPackets[0] == false && preparation.cancelReason == null) {
			preparation.cancelReason = new ServerOfflineException(this, preparation.address);
		}

		// Send OPEN_CONNECTION_REQUEST_TWO until a response is received
		while (retriesLeft > 0 && preparation.loginPackets[1] == false && preparation.cancelReason == null) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo();
			connectionRequestTwo.clientGuid = this.guid;
			connectionRequestTwo.address = preparation.address;
			connectionRequestTwo.maximumTransferUnit = preparation.maximumTransferUnit;
			connectionRequestTwo.encode();

			if (!connectionRequestTwo.failed()) {
				this.sendNettyMessage(connectionRequestTwo, address);
				RakNetUtils.threadLock(500);
			} else {
				preparation.cancelReason = new PacketBufferException(this, connectionRequestTwo);
			}
		}

		// If the server didn't respond then it is offline
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
	 * Connects the client to a server with the specified address.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(InetAddress address, int port) throws RakNetException {
		this.connect(new InetSocketAddress(address, port));
	}

	/**
	 * Connects the client to a server with the specified address.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 * @throws UnknownHostException
	 *             if the specified address is an unknown host.
	 */
	public final void connect(String address, int port) throws RakNetException, UnknownHostException {
		this.connect(InetAddress.getByName(address), port);
	}

	/**
	 * Connects the the client to the specified discovered server.
	 * 
	 * @param server
	 *            the discovered server to connect to.
	 * @throws RakNetException
	 *             if an error occurs during connection or login.
	 */
	public final void connect(DiscoveredServer server) throws RakNetException {
		this.connect(server.getAddress());
	}

	/**
	 * Connects the client to a server with the specified address on it's own
	 * <code>Thread</code>.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @return the Thread the client is running on.
	 */
	public final synchronized Thread connectThreaded(InetSocketAddress address) {
		// Give the thread a reference
		RakNetClient client = this;

		// Create and start the thread
		Thread thread = new Thread() {
			@Override
			public synchronized void run() {
				try {
					client.connect(address);
				} catch (Throwable throwable) {
					client.getListener().onThreadException(throwable);
				}
			}
		};
		thread.start();

		// Return the thread so it can be modified
		return thread;
	}

	/**
	 * Connects the client to a server with the specified address on it's own
	 * <code>Thread</code>.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @return the Thread the client is running on.
	 */
	public final Thread connectThreaded(InetAddress address, int port) {
		return this.connectThreaded(new InetSocketAddress(address, port));
	}

	/**
	 * Connects the client to a server with the specified address on it's own
	 * <code>Thread</code>.
	 * 
	 * @param address
	 *            the address of the server to connect to.
	 * @param port
	 *            the port of the server to connect to.
	 * @throws UnknownHostException
	 *             if the specified address is an unknown host.
	 * @return the Thread the client is running on.
	 */
	public final Thread connectThreaded(String address, int port) throws UnknownHostException {
		return this.connectThreaded(InetAddress.getByName(address), port);
	}

	/**
	 * Connects the the client to the specified discovered server on it's own
	 * <code>Thread</code>.
	 * 
	 * @param server
	 *            the discovered server to connect to.
	 * @return the Thread the client is running on.
	 */
	public final Thread connectThreaded(DiscoveredServer server) {
		return this.connectThreaded(server.getAddress());
	}

	/**
	 * Starts the loop needed for the client to stay connected to the server.
	 */
	private final void initConnection() throws RakNetException {
		while (session != null) {
			session.update();
		}
	}

	@Override
	public final void sendMessage(Reliability reliability, int channel, Packet packet) {
		if (this.isConnected()) {
			session.sendMessage(reliability, channel, packet);
		}
	}

	/**
	 * Disconnects the client from the server if it is connected to one.
	 * 
	 * @param reason
	 *            the reason the client disconnected from the server.
	 */
	public final void disconnect(String reason) {
		if (session != null) {
			session.closeConnection();
			listener.onDisconnect(session, reason);
		}
		this.session = null;
	}

	/**
	 * Disconnects the client from the server if it is connected to one.
	 */
	public final void disconnect() {
		this.disconnect("Disconnected");
	}

	/**
	 * Shuts down the client for good, once this is called the client can no
	 * longer connect to servers.
	 */
	public final void shutdown() {
		// Close channel
		channel.close();
		group.shutdownGracefully();

		// Shutdown discovery system if needed
		discoverySystem.removeClient(this);
		if (discoverySystem.getClients().length <= 0) {
			discoverySystem.shutdown();
			discoverySystem = new DiscoveryThread();
		}
	}

	/**
	 * Disconnects from the server and shuts down the client for good, once this
	 * is called the client can no longer connect to servers.
	 * 
	 * @param reason
	 *            the reason the client shutdown.
	 */
	public final void disconnectAndShutdown(String reason) {
		this.disconnect(reason);
		this.shutdown();
	}

	/**
	 * Disconnects from the server and shuts down the client for good, once this
	 * is called the client can no longer connect to servers.
	 */
	public final void disconnectAndShutdown() {
		this.disconnectAndShutdown("Shutdown");
	}

	@Override
	public final void finalize() {
		this.shutdown();
	}

}
