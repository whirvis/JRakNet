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
package net.marfgamer.raknet.client;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.client.discovery.DiscoveredServer;
import net.marfgamer.raknet.client.discovery.DiscoveryMode;
import net.marfgamer.raknet.client.discovery.DiscoveryThread;
import net.marfgamer.raknet.exception.NoListenerException;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.exception.client.NettyHandlerException;
import net.marfgamer.raknet.exception.client.ServerOfflineException;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.login.ConnectionRequest;
import net.marfgamer.raknet.protocol.login.OpenConnectionRequestOne;
import net.marfgamer.raknet.protocol.login.OpenConnectionRequestTwo;
import net.marfgamer.raknet.protocol.message.CustomPacket;
import net.marfgamer.raknet.protocol.message.acknowledge.Acknowledge;
import net.marfgamer.raknet.protocol.status.UnconnectedPing;
import net.marfgamer.raknet.protocol.status.UnconnectedPingOpenConnections;
import net.marfgamer.raknet.protocol.status.UnconnectedPong;
import net.marfgamer.raknet.session.UnumRakNetPeer;
import net.marfgamer.raknet.session.RakNetServerSession;
import net.marfgamer.raknet.session.RakNetSession;
import net.marfgamer.raknet.session.RakNetState;
import net.marfgamer.raknet.util.RakNetUtils;

/**
 * This class is used to connection to servers using the RakNet protocol
 *
 * @author MarfGamer
 */
public class RakNetClient implements UnumRakNetPeer {

	// JRakNet plans to use it's own dynamic MTU system later
	protected static final MaximumTransferUnit[] units = new MaximumTransferUnit[] { new MaximumTransferUnit(1172, 4),
			new MaximumTransferUnit(548, 5) };

	// Used to discover systems without relying on the main thread
	private static final DiscoveryThread discoverySystem = new DiscoveryThread();

	// Client data
	private final long guid;
	private final long timestamp;
	private int discoveryPort;
	private DiscoveryMode discoveryMode;
	private final ConcurrentHashMap<InetSocketAddress, DiscoveredServer> discovered;

	// Networking data
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetClientHandler handler;

	// Session management
	private Channel channel;
	private SessionPreparation preparation;
	private volatile RakNetServerSession session;
	private volatile RakNetClientListener listener;

	public RakNetClient(DiscoveryMode discoveryMode, int discoveryPort) {
		// Set client data
		this.guid = RakNet.UNIQUE_ID_BITS.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();

		// Set discovery data
		this.discoveryPort = discoveryPort;
		this.discoveryMode = discoveryMode;
		if (discoveryMode == null) {
			this.discoveryMode = (discoveryPort > -1 ? DiscoveryMode.ALL_CONNECTIONS : DiscoveryMode.NONE);
		}
		this.discovered = new ConcurrentHashMap<InetSocketAddress, DiscoveredServer>();

		// Set networking data
		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetClientHandler(this);

		// Initiate bootstrap data
		try {
			bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
			bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
			this.channel = bootstrap.bind(0).sync().channel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Initiate discovery system if it is not yet started
		if (discoverySystem.isRunning() == false) {
			discoverySystem.start();
		}
		discoverySystem.addClient(this);
	}

	public RakNetClient(int discoveryPort) {
		this(null, discoveryPort);
	}

	public RakNetClient() {
		this(-1);
	}

	/**
	 * Returns the client's globally unique ID (GUID)
	 * 
	 * @return The client's globally unique ID
	 */
	public long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * Returns the client's timestamp (how long ago it started in milliseconds)
	 * 
	 * @return The client's timestamp
	 */
	public long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	/**
	 * Returns the client's discovery port
	 * 
	 * @return The client's discovery port
	 */
	public int getDiscoveryPort() {
		return this.discoveryPort;
	}

	/**
	 * Sets the client's discovery port
	 * 
	 * @param discoveryPort
	 *            The new discovery port
	 * @return The client
	 */
	public RakNetClient setDiscoveryPort(int discoveryPort) {
		this.discoveryPort = discoveryPort;
		return this;
	}

	/**
	 * Returns the client's discovery mode
	 * 
	 * @return The client's discovery mode
	 */
	public DiscoveryMode getDiscoveryMode() {
		return this.discoveryMode;
	}

	/**
	 * Sets the client's discovery mode
	 * 
	 * @param mode
	 *            How the client will discover servers on the local network
	 * @return The client
	 */
	public RakNetClient setDiscoveryMode(DiscoveryMode mode) {
		this.discoveryMode = (mode != null ? mode : DiscoveryMode.NONE);
		if (this.discoveryMode == DiscoveryMode.NONE) {
			if (listener != null) {
				for (InetSocketAddress address : discovered.keySet()) {
					listener.onServerForgotten(address);
				}
			}
			discovered.clear(); // We are not discovering servers anymore!
		}
		return this;
	}

	/**
	 * Returns the session the client is connected to
	 * 
	 * @return The session the client is connected to
	 */
	public RakNetServerSession getSession() {
		return this.session;
	}

	/**
	 * Returns the client's listener
	 * 
	 * @return The client's listener
	 */
	public RakNetClientListener getListener() {
		return this.listener;
	}

	/**
	 * Sets the client's listener
	 * 
	 * @param listener
	 *            The client's new listener
	 * @return The client
	 */
	public RakNetClient setListener(RakNetClientListener listener) {
		if (listener == null) {
			throw new NullPointerException("Listener must not be null!");
		}
		this.listener = listener;
		return this;
	}

	/**
	 * Returns whether or not the client is connected
	 * 
	 * @return Whether or not the client is connected
	 */
	public boolean isConnected() {
		if (session != null) {
			return (session.getState() == RakNetState.CONNECTED);
		}
		return false;
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
	 * Handles a packet received by the handler
	 * 
	 * @param packet
	 *            The packet to handle
	 * @param sender
	 *            The address of the sender
	 */
	public void handleMessage(RakNetPacket packet, InetSocketAddress sender) {
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
				preparation.handlePacket(packet);
				return;
			}
		}

		// Only handle these from the server we're connected to!
		if (session != null) {
			if (sender.equals(session.getAddress())) {
				if (packetId >= ID_RESERVED_3 && packetId <= ID_RESERVED_9) {
					CustomPacket custom = new CustomPacket(packet);
					custom.decode();

					session.handleCustom0(custom);
				} else if (packetId == Acknowledge.ACKNOWLEDGED || packetId == Acknowledge.NOT_ACKNOWLEDGED) {
					Acknowledge acknowledge = new Acknowledge(packet);
					acknowledge.decode();

					session.handleAcknowledge(acknowledge);
				}
			}
		}
	}

	/**
	 * Sends a raw packet to the specified address
	 * 
	 * @param packet
	 *            The packet to send
	 * @param address
	 *            The address to send the packet to
	 */
	private void sendRawMessage(RakNetPacket packet, InetSocketAddress address) {
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
	@SuppressWarnings("unused") // Currently not needed by the client
	private void sendRawMessage(int packetId, InetSocketAddress address) {
		this.sendRawMessage(new RakNetPacket(packetId), address);
	}

	/**
	 * Updates the discovery data in the client by sending pings and removing
	 * servers that have taken too long to respond to a ping
	 */
	public void updateDiscoveryData() {
		// Make sure we have a listener
		if (listener == null) {
			return;// throw new NoListenerException("Unable to start client,
					// there is no listener!");
		}

		// Remove all servers that have timed out
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

		// Broadcast ping
		if (discoveryMode != DiscoveryMode.NONE && discoveryPort > -1) {
			UnconnectedPing ping = new UnconnectedPing();
			if (discoveryMode == DiscoveryMode.OPEN_CONNECTIONS) {
				ping = new UnconnectedPingOpenConnections();
			}

			ping.timestamp = this.getTimestamp();
			ping.encode();

			this.sendRawMessage(ping, new InetSocketAddress("255.255.255.255", discoveryPort));
		}
	}

	/**
	 * This method handles the specified pong packet and updates the discovery
	 * data accordingly
	 * 
	 * @param sender
	 *            The sender of the pong packet
	 * @param pong
	 *            The pong packet to handle
	 */
	public void updateDiscoveryData(InetSocketAddress sender, UnconnectedPong pong) {
		if (!discovered.containsKey(sender)) {
			// Server discovered
			discovered.put(sender, new DiscoveredServer(sender, System.currentTimeMillis(), pong.identifier));
			if (listener != null) {
				listener.onServerDiscovered(sender, pong.identifier);
			}
		} else {
			// Server already discovered, but data has changed
			DiscoveredServer server = discovered.get(sender);
			server.setDiscoveryTimestamp(System.currentTimeMillis());
			if (server.getIdentifier().equals(pong.identifier) == false) {
				server.setIdentifier(pong.identifier);
				if (listener != null) {
					listener.onServerIdentifierUpdate(sender, pong.identifier);
				}
			}
		}
	}

	/**
	 * Connects the client to a server with the specified address
	 * 
	 * @param address
	 *            The address of the server to connect to
	 * @throws RakNetException
	 *             Thrown if an error occurs during connection or login
	 */
	public void connect(InetSocketAddress address) throws RakNetException {
		// Make sure we have a listener
		if (this.listener == null) {
			throw new NoListenerException("Unable to start client, there is no listener!");
		}

		// Reset client data
		if (this.isConnected()) {
			this.disconnect("Disconnected");
		}
		this.preparation = new SessionPreparation(this, units[0].getMaximumTransferUnit());
		preparation.address = address;

		// Send OPEN_CONNECTION_REQUEST_ONE with a decreasing MTU
		int retries = 0;
		for (MaximumTransferUnit unit : units) {
			retries += unit.getRetries();
			while (unit.retry() > 0 && preparation.loginPackets[0] == false && preparation.cancelReason == null) {
				OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
				connectionRequestOne.maximumTransferUnit = unit.getMaximumTransferUnit();
				connectionRequestOne.protocolVersion = RakNet.CLIENT_NETWORK_PROTOCOL;
				connectionRequestOne.encode();
				this.sendRawMessage(connectionRequestOne, address);

				RakNetUtils.passiveSleep(500);
			}
		}

		// Reset MaximumTransferUnit's so they can be used again
		for (MaximumTransferUnit unit : units) {
			unit.reset();
		}

		// If the server didn't respond then it is offline
		if (preparation.loginPackets[0] == false && preparation.cancelReason == null) {
			preparation.cancelReason = new ServerOfflineException(this, preparation.address);
		}

		// Send OPEN_CONNECTION_REQUEST_TWO until a response is received
		while (retries > 0 && preparation.loginPackets[1] == false && preparation.cancelReason == null) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo();
			connectionRequestTwo.clientGuid = this.guid;
			connectionRequestTwo.address = preparation.address;
			connectionRequestTwo.maximumTransferUnit = preparation.maximumTransferUnit;
			connectionRequestTwo.encode();
			this.sendRawMessage(connectionRequestTwo, address);

			RakNetUtils.passiveSleep(500);
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
	 * Connects the client to a server with the specified address
	 * 
	 * @param address
	 *            The address of the server to connect to
	 * @param port
	 *            The port of the server to connect to
	 * @throws RakNetException
	 *             Thrown if an error occurs during connection or login
	 */
	public void connect(InetAddress address, int port) throws RakNetException {
		this.connect(new InetSocketAddress(address, port));
	}

	/**
	 * Connects the client to a server with the specified address
	 * 
	 * @param address
	 *            The address of the server to connect to
	 * @param port
	 *            The port of the server to connect to
	 * @throws RakNetException
	 *             Thrown if an error occurs during connection or login
	 * @throws UnknownHostException
	 *             Thrown if the specified address is an unknown host
	 */
	public void connect(String address, int port) throws RakNetException, UnknownHostException {
		this.connect(InetAddress.getByName(address), port);
	}

	/**
	 * Connects the the client to the specified discovered server
	 * 
	 * @param server
	 *            The discovered server to connect to
	 * @throws RakNetException
	 *             Thrown if an error occurs during connection or login
	 */
	public void connect(DiscoveredServer server) throws RakNetException {
		this.connect(server.getAddress());
	}

	/**
	 * Connects the client to a server with the specified address on it's own
	 * Thread
	 * 
	 * @param address
	 *            The address of the server to connect to
	 * @return The Thread the client is running on
	 */
	public Thread connectThreaded(InetSocketAddress address) {
		// Give the thread a reference
		RakNetClient client = this;

		// Create and start the thread
		Thread thread = new Thread() {
			@Override
			public synchronized void run() {
				try {
					client.connect(address);
				} catch (Exception e) {
					client.getListener().onThreadException(e);
				}
			}
		};
		thread.start();

		// Return the thread so it can be modified
		return thread;
	}

	/**
	 * Connects the client to a server with the specified address on it's own
	 * Thread
	 * 
	 * @param address
	 *            The address of the server to connect to
	 * @param port
	 *            The port of the server to connect to
	 * @return The Thread the client is running on
	 */
	public Thread connectThreaded(InetAddress address, int port) {
		return this.connectThreaded(new InetSocketAddress(address, port));
	}

	/**
	 * Connects the client to a server with the specified address on it's own
	 * Thread
	 * 
	 * @param address
	 *            The address of the server to connect to
	 * @param port
	 *            The port of the server to connect to
	 * @throws UnknownHostException
	 *             Thrown if the specified address is an unknown host
	 * @return The Thread the client is running on
	 */
	public Thread connectThreaded(String address, int port) throws UnknownHostException {
		return this.connectThreaded(InetAddress.getByName(address), port);
	}

	/**
	 * Connects the the client to the specified discovered server on it's own
	 * Thread
	 * 
	 * @param server
	 *            The discovered server to connect to
	 * @return The Thread the client is running on
	 */
	public Thread connectThreaded(DiscoveredServer server) {
		return this.connectThreaded(server.getAddress());
	}

	/**
	 * Starts the loop needed for the client to stay connected to the server
	 */
	private void initConnection() throws RakNetException {
		while (session != null) {
			long lastPacketReceiveTime = session.getLastPacketReceiveTime();
			session.update();

			if (System.currentTimeMillis() - lastPacketReceiveTime > RakNetSession.SESSION_TIMEOUT) {
				this.disconnect("The session has timed out!");
				break;
			}
		}
	}

	@Override
	public void sendMessage(Reliability reliability, int channel, Packet packet) {
		if (this.isConnected()) {
			session.sendMessage(reliability, channel, packet);
		}
	}

	/**
	 * Disconnects the client from the server if it is connected to one
	 * 
	 * @param reason
	 *            The reason the client disconnected from the server
	 */
	public void disconnect(String reason) {
		if (session != null) {
			session.closeConnection();
			this.getListener().onDisconnect(session, reason);
		}
		this.session = null;
		this.preparation = null;
	}

	/**
	 * Disconnects the client from the server if it is connected to one
	 */
	public void disconnect() {
		this.disconnect("Disconnected");
	}

	/**
	 * Shuts down the client for good, once this is called the client can no
	 * longer connect to servers
	 */
	public void shutdown() {
		throw new RuntimeException("This method is not yet implemented!");
	}

	/**
	 * Disconnects from the server and shuts down the client for good, once this
	 * is called the client can no longer connect to servers
	 * 
	 * @param reason
	 *            The reason the client shutdown
	 */
	public void disconnectAndShutdown(String reason) {
		this.disconnect(reason);
		this.shutdown();
	}

	/**
	 * Disconnects from the server and shuts down the client for good, once this
	 * is called the client can no longer connect to servers
	 */
	public void disconnectAndShutdown() {
		this.disconnectAndShutdown("Shutdown");
	}

	@Override
	public void finalize() {
		this.shutdown();
		discoverySystem.removeClient(this);
	}

}
