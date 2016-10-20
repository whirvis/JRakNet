package net.marfgamer.raknet.client;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.exception.client.DiscoveryDisabledException;
import net.marfgamer.raknet.exception.client.IncompatibleProtocolException;
import net.marfgamer.raknet.exception.client.NettyHandlerException;
import net.marfgamer.raknet.exception.client.NoFreeIncomingConnectionsException;
import net.marfgamer.raknet.exception.client.ServerOfflineException;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.login.ConnectionRequest;
import net.marfgamer.raknet.protocol.login.OpenConnectionRequestOne;
import net.marfgamer.raknet.protocol.login.OpenConnectionRequestTwo;
import net.marfgamer.raknet.protocol.login.OpenConnectionResponseOne;
import net.marfgamer.raknet.protocol.login.OpenConnectionResponseTwo;
import net.marfgamer.raknet.protocol.login.error.IncompatibleProtocol;
import net.marfgamer.raknet.protocol.login.error.NoFreeIncomingConnections;
import net.marfgamer.raknet.protocol.message.CustomPacket;
import net.marfgamer.raknet.protocol.message.acknowledge.Acknowledge;
import net.marfgamer.raknet.protocol.status.UnconnectedPing;
import net.marfgamer.raknet.protocol.status.UnconnectedPingOpenConnections;
import net.marfgamer.raknet.session.RakNetServerSession;

public class RakNetClient {

	// JRakNet plans to use it's own dynamic MTU system later
	private static int PHYSICAL_MAXIMUM_TRANSFER_UNIT = -1;
	private static final MaximumTransferUnit[] units = new MaximumTransferUnit[] { new MaximumTransferUnit(1172, 4),
			new MaximumTransferUnit(548, 5) };

	// Attempt to detect the MTU
	static {
		try {
			PHYSICAL_MAXIMUM_TRANSFER_UNIT = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU();
		} catch (IOException e) {
			System.err.println("Warning: Failed to locate the physical maximum transfer unit! Defaulting to "
					+ units[0].getMaximumTransferUnit() + "...");
			PHYSICAL_MAXIMUM_TRANSFER_UNIT = units[0].getMaximumTransferUnit();
		}
	}

	// Discovery system (static because we only need one)
	private static final DiscoveryThread discoverySystem = new DiscoveryThread();

	// Client data
	private final long guid;
	private final long timestamp;
	private final boolean threaded;
	private DiscoverMode discoverMode;
	private long pingId;

	// Networking data
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetClientHandler handler;

	// Session management
	private Channel channel;
	private RakNetClientListener listener;
	private SessionPreparation preparation;
	private volatile RakNetServerSession session; // Allow other threads to
													// modify this

	public RakNetClient(boolean threaded) {
		this.guid = RakNet.UNIQUE_ID_BITS.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();
		this.threaded = threaded;

		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetClientHandler(this);

		// Create bootstrap for server discovery
		bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
		bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
		this.channel = bootstrap.bind(0).channel();

		try {
			discoverySystem.addClient(this);
			if (discoverySystem.isRunning() == false) {
				discoverySystem.start();
			}
		} catch (RakNetException e) {
			e.printStackTrace();
		}
	}

	public RakNetClient() {
		this(true);
	}

	public void enableDiscovery(DiscoverMode discoverMode) {
		this.discoverMode = discoverMode;
	}

	public void enableDiscovery() {
		this.enableDiscovery(DiscoverMode.ALL_CONNECTIONS);
	}

	public void disableDiscovery() {
		this.discoverMode = null;
	}

	public boolean discoveryEnabled() {
		return (this.discoverMode != null);
	}

	public RakNetClientListener getListener() {
		return this.listener;
	}
	
	public void setListener(RakNetClientListener listener) {
		this.listener = listener;
	}

	public RakNetServerSession getSession() {
		return this.session;
	}

	public void connect(InetSocketAddress address) throws RakNetException {
		// Reset client data
		this.preparation = new SessionPreparation(this);
		preparation.address = address;

		// Set handler data
		this.channel = bootstrap.connect(address).channel();

		// Send OPEN_CONNECTION_REQUEST_ONE with a decreasing MTU
		int retries = 0;
		for (MaximumTransferUnit unit : units) {
			retries += unit.getRetries();
			while (unit.retry() > 0 && preparation.loginPackets[0] == false && preparation.cancelled == false) {
				OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
				connectionRequestOne.maximumTransferUnit = unit.getMaximumTransferUnit();
				connectionRequestOne.protocolVersion = RakNet.CLIENT_NETWORK_PROTOCOL;
				connectionRequestOne.encode();
				this.sendRaw(connectionRequestOne, address);

				this.loopSleep(500);
			}
		}

		// If the server didn't respond then it is offline
		if (retries <= 0 && preparation.cancelled == false) {
			preparation.cancelReason = new ServerOfflineException(this, preparation.address);
			preparation.cancelled = true;
		}

		// Send OPEN_CONNECTION_REQUEST_TWO until a response is received
		while (retries > 0 && preparation.loginPackets[1] == false && preparation.cancelled == false) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo();
			connectionRequestTwo.clientGuid = this.guid;
			connectionRequestTwo.address = preparation.address;
			connectionRequestTwo.maximumTransferUnit = preparation.maximumTransferUnit;
			connectionRequestTwo.encode();
			this.sendRaw(connectionRequestTwo, address);

			this.loopSleep(500);
		}

		// If the session was set we are connected
		if (preparation.readyForSession() == true) {
			// Set session and delete preparation data
			this.session = preparation.createSession(channel);
			this.preparation = null;

			// Send connection packet
			ConnectionRequest connectionRequest = new ConnectionRequest();
			connectionRequest.clientGuid = this.guid;
			connectionRequest.timestamp = (System.currentTimeMillis() - this.timestamp);
			connectionRequest.encode();
			session.sendPacket(Reliability.RELIABLE_ORDERED, connectionRequest);

			// Initiate connection loop required for the session to function
			if (this.threaded == true) {
				this.initConnectionThreaded();
			} else {
				this.initConnection();
			}
		} else {
			// Reset the connection data, the connection failed
			InetSocketAddress preparationAddress = preparation.address;
			RakNetException preparationCancelReason = preparation.cancelReason;
			this.preparation = null;

			// Why was the exception cancelled?
			if (preparationCancelReason != null) {
				throw preparationCancelReason;
			} else {
				// We assume the server is offline
				throw new ServerOfflineException(this, preparationAddress);
			}
		}
	}

	public void connect(InetAddress address, int port) throws Throwable {
		this.connect(new InetSocketAddress(address, port));
	}

	public void connect(String address, int port) throws Throwable {
		this.connect(InetAddress.getByName(address), port);
	}

	protected void handleHandlerException(Throwable cause) {
		if (preparation != null) {
			preparation.cancelReason = new NettyHandlerException(this, handler, cause);
			preparation.cancelled = true;
		}
	}

	private void sendRaw(RakNetPacket packet, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	private void broadcastRaw(RakNetPacket packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), new InetSocketAddress("255.255.255.255", 19132)));
	}

	public void sendPing() throws RakNetException {
		if (this.discoveryEnabled() == false) {
			throw new DiscoveryDisabledException(this);
		}

		UnconnectedPing ping = (discoverMode == DiscoverMode.ALL_CONNECTIONS ? new UnconnectedPing()
				: new UnconnectedPingOpenConnections());
		ping.time = System.currentTimeMillis();
		ping.pingId = pingId++;
		ping.encode();

		this.broadcastRaw(ping);
	}

	private void initConnection() {
		while (session != null) {
			try {
				session.update();
			} catch (Exception e) {
				session.closeConnection(e.getMessage());
			}
		}
	}

	private void initConnectionThreaded() {
		// Give the thread a reference
		RakNetClient client = this;

		// Create and start the thread
		Thread thread = new Thread() {
			@Override
			public synchronized void run() {
				client.initConnection();
			}
		};
		thread.start();
	}

	public void handleMessage(RakNetPacket packet, InetSocketAddress sender) throws Exception {
		short packetId = packet.getId();

		// Handle the packets in this block only when we are connecting!
		if (preparation != null) {
			if (sender.equals(preparation.address) == false) {
				return; // Only handle these packets from the actual sender!
			}

			if (packetId == ID_OPEN_CONNECTION_REPLY_1) {
				OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne(packet);
				connectionResponseOne.decode();

				if (connectionResponseOne.magic == true && connectionResponseOne.useSecurity == false
						&& connectionResponseOne.maximumTransferUnit > RakNet.MINIMUM_TRANSFER_UNIT
						&& connectionResponseOne.maximumTransferUnit < PHYSICAL_MAXIMUM_TRANSFER_UNIT) {
					preparation.maximumTransferUnit = connectionResponseOne.maximumTransferUnit;
					preparation.guid = connectionResponseOne.serverGuid;
					preparation.loginPackets[0] = true;
				} else {
					preparation.cancelled = true;
				}
			} else if (packetId == ID_OPEN_CONNECTION_REPLY_2) {
				OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo(packet);
				connectionResponseTwo.decode();

				if (connectionResponseTwo.magic == true && connectionResponseTwo.encryptionEnabled == false
						&& connectionResponseTwo.serverGuid == preparation.guid
						&& connectionResponseTwo.maximumTransferUnit == preparation.maximumTransferUnit) {
					preparation.loginPackets[1] = true;
				} else {
					preparation.cancelled = true;
				}
			} else if (packetId == ID_ALREADY_CONNECTED) {

			} else if (packetId == ID_NO_FREE_INCOMING_CONNECTIONS) {
				NoFreeIncomingConnections noFreeIncomingConnections = new NoFreeIncomingConnections(packet);
				noFreeIncomingConnections.decode();

				preparation.cancelReason = new NoFreeIncomingConnectionsException(this);
				preparation.cancelled = true;
			} else if (packetId == ID_CONNECTION_BANNED) {

			} else if (packetId == ID_INCOMPATIBLE_PROTOCOL_VERSION) {
				IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol(packet);
				incompatibleProtocol.decode();

				if (incompatibleProtocol.serverGuid == preparation.guid) {
					preparation.cancelReason = new IncompatibleProtocolException(this, RakNet.CLIENT_NETWORK_PROTOCOL,
							incompatibleProtocol.networkProtocol);
					preparation.cancelled = true;
				}
			}
			return; // We can't handle anything else until we're logged in!
		}

		// Like login packets, we only handle these packets from the server
		if (session == null) {
			return; // We are not connected
		}

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

	private void loopSleep(int time) {
		long waitStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - waitStart < time)
			;
	}

	public static void main(String[] args) throws Throwable {
		RakNetClient client = new RakNetClient();
		client.enableDiscovery();
		// client.connect("127.0.0.1", 19132);
	}

}
