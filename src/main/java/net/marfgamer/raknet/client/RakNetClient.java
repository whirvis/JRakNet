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
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.acknowledge.Acknowledge;
import net.marfgamer.raknet.protocol.connected.ConnectedConnectionRequest;
import net.marfgamer.raknet.protocol.message.CustomPacket;
import net.marfgamer.raknet.protocol.unconnected.UnconnectedIncompatibleProtocol;
import net.marfgamer.raknet.protocol.unconnected.UnconnectedOpenConnectionRequestOne;
import net.marfgamer.raknet.protocol.unconnected.UnconnectedOpenConnectionRequestTwo;
import net.marfgamer.raknet.protocol.unconnected.UnconnectedOpenConnectionResponseOne;
import net.marfgamer.raknet.protocol.unconnected.UnconnectedOpenConnectionResponseTwo;
import net.marfgamer.raknet.session.RakNetServerSession;
import net.marfgamer.raknet.session.RakNetSession;

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

	// Client data
	private final long guid;
	private final long timestamp;
	private final boolean threaded;

	// Networking data
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetClientHandler handler;

	// Session management
	private Channel channel;
	private RakNetClientListener listener;
	private SessionPreparation preparation;
	private volatile RakNetServerSession session;

	public RakNetClient(boolean threaded) {
		this.guid = RakNet.UNIQUE_ID_BITS.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();
		this.threaded = threaded;

		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetClientHandler(this);
	}

	public RakNetClient() {
		this(true);
	}

	public RakNetSession getSession() {
		return this.session;
	}

	public void connect(InetSocketAddress address) throws Exception {
		// Reset client data
		this.reset();
		this.preparation = new SessionPreparation(this);
		preparation.address = address;

		// Set handler data
		bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
		bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
		this.channel = bootstrap.connect(address).channel();

		// Send OPEN_CONNECTION_REQUEST_ONE with a decreasing MTU
		int retries = 0;
		for (MaximumTransferUnit unit : units) {
			retries += unit.getRetries();
			while (unit.retry() > 0 && preparation.loginPackets[0] == false && preparation.cancelled == false) {
				UnconnectedOpenConnectionRequestOne connectionRequestOne = new UnconnectedOpenConnectionRequestOne();
				connectionRequestOne.maximumTransferUnit = unit.getMaximumTransferUnit();
				connectionRequestOne.protocolVersion = RakNet.CLIENT_NETWORK_PROTOCOL;
				connectionRequestOne.encode();
				this.sendRaw(connectionRequestOne, address);

				this.loopSleep(500);
			}
		}

		// Send OPEN_CONNECTION_REQUEST_TWO until a response is received
		while (retries > 0 && preparation.loginPackets[1] == false && preparation.cancelled == false) {
			UnconnectedOpenConnectionRequestTwo connectionRequestTwo = new UnconnectedOpenConnectionRequestTwo();
			connectionRequestTwo.clientGuid = this.guid;
			connectionRequestTwo.address = preparation.address;
			connectionRequestTwo.maximumTransferUnit = preparation.maximumTransferUnit;
			connectionRequestTwo.encode();
			this.sendRaw(connectionRequestTwo, address);

			this.loopSleep(500);
		}

		// If the session was set we are connected
		if (preparation.readyForSession()) {
			// Set session and delete preparation data
			this.session = preparation.createSession(channel);
			this.preparation = null;

			// Send connection packet
			ConnectedConnectionRequest connectionRequest = new ConnectedConnectionRequest();
			connectionRequest.clientGuid = this.guid;
			connectionRequest.timestamp = (System.currentTimeMillis() - this.timestamp);
			connectionRequest.encode();
			session.sendPacket(Reliability.RELIABLE, connectionRequest);

			// Initiate connection loop required for the session to function
			if (this.threaded == true) {
				this.initConnectionThreaded();
			} else {
				this.initConnection();
			}
		} else {
			// Reset the connection data, the connection failed
			this.reset();
		}
	}

	public void connect(InetAddress address, int port) throws Exception {
		this.connect(new InetSocketAddress(address, port));
	}

	public void connect(String address, int port) throws Exception {
		this.connect(InetAddress.getByName(address), port);
	}

	private void initConnection() {
		while (session != null) {
			try {
				session.update();
			} catch (Exception e) {
				session.closeConnection(e.getMessage());
			}
		}
		System.out.println("SES NULL");
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

	private void reset() {
		// Disconnect current session
		if (session != null) {
			session.closeConnection("Disconnected");
		}

		// Delete preparation and session data
		this.preparation = null;
		this.session = null;
	}

	private void sendRaw(RakNetPacket packet, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	public void handleMessage(RakNetPacket packet, InetSocketAddress sender) throws Exception {
		short packetId = packet.getId();

		// Handle the packets in this block only when we are connecting!
		if (preparation != null) {
			if (sender.equals(preparation.address) == false) {
				return; // Only handle these packets from the actual sender
			}

			if (packetId == ID_OPEN_CONNECTION_REPLY_1) {
				UnconnectedOpenConnectionResponseOne connectionResponseOne = new UnconnectedOpenConnectionResponseOne(
						packet);
				connectionResponseOne.decode();

				if (connectionResponseOne.magic == true && connectionResponseOne.useSecurity == false
						&& connectionResponseOne.maximumTransferUnit > RakNet.MINIMUM_TRANSFER_UNIT
						&& connectionResponseOne.maximumTransferUnit < PHYSICAL_MAXIMUM_TRANSFER_UNIT) {
					preparation.maximumTransferUnit = connectionResponseOne.maximumTransferUnit;
					preparation.guid = connectionResponseOne.serverGuid;
					preparation.loginPackets[0] = true;
				} else {
					;
					preparation.cancelled = true;
				}
			} else if (packetId == ID_OPEN_CONNECTION_REPLY_2) {
				UnconnectedOpenConnectionResponseTwo connectionResponseTwo = new UnconnectedOpenConnectionResponseTwo(
						packet);
				connectionResponseTwo.decode();

				if (connectionResponseTwo.magic == true && connectionResponseTwo.encryptionEnabled == false
						&& connectionResponseTwo.serverGuid == preparation.guid
						&& connectionResponseTwo.maximumTransferUnit == preparation.maximumTransferUnit) {
					preparation.loginPackets[1] = true;
				} else {
					preparation.cancelled = true;
				}
			} else if (packetId == ID_INCOMPATIBLE_PROTOCOL_VERSION) {
				UnconnectedIncompatibleProtocol incompatibleProtocol = new UnconnectedIncompatibleProtocol(packet);
				incompatibleProtocol.decode();
				preparation.cancelException = new Exception("Incompatible protocol!");
			}
			// TODO: Exceptions and possibly improved login state management
		} else {
			// Like login packets, we only handle these packets from the server
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

	private void loopSleep(int time) {
		long waitStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - waitStart < time)
			;
	}

	public static void main(String[] args) throws Exception {
		RakNetClient client = new RakNetClient();
		client.connect("127.0.0.1", 19132);
	}

}
