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
import net.marfgamer.raknet.protocol.connected.ConnectedConnectionRequest;
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

	private final long guid;
	private final long timestamp;
	private int maximumTransferUnit;

	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetClientHandler handler;
	private final PreConnection preConnection;

	private Channel channel;
	private RakNetClientListener listener;
	private RakNetServerSession session;

	public RakNetClient() {
		this.guid = RakNet.UNIQUE_ID_BITS.getLeastSignificantBits();
		this.timestamp = System.currentTimeMillis();

		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetClientHandler(this);
		this.preConnection = new PreConnection();
	}
	
	public RakNetSession getSession() {
		return this.session;
	}

	public void connect(InetSocketAddress address) throws Exception {
		this.reset();
		preConnection.serverAddress = address;

		bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
		bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
		this.channel = bootstrap.connect(address).channel();

		// Send OPEN_CONNECTION_REQUEST_ONE with a decreasing MTU
		int retries = 0;
		for (MaximumTransferUnit unit : units) {
			retries += unit.getRetries();
			while (unit.retry() > 0 && preConnection.loginPackets[0] == false && preConnection.cancelled == false) {
				UnconnectedOpenConnectionRequestOne connectionRequestOne = new UnconnectedOpenConnectionRequestOne();
				connectionRequestOne.maximumTransferUnit = unit.getMaximumTransferUnit();
				connectionRequestOne.protocolVersion = RakNet.CLIENT_NETWORK_PROTOCOL;
				connectionRequestOne.encode();
				this.sendRaw(connectionRequestOne, address);

				this.loopSleep(500);
			}
		}

		// Send OPEN_CONNECTION_REQUEST_TWO until a response is received
		while (retries > 0 && preConnection.loginPackets[1] == false && preConnection.cancelled == false) {
			UnconnectedOpenConnectionRequestTwo connectionRequestTwo = new UnconnectedOpenConnectionRequestTwo();
			connectionRequestTwo.clientGuid = this.guid;
			connectionRequestTwo.address = preConnection.serverAddress;
			connectionRequestTwo.maximumTransferUnit = preConnection.maximumTransferUnit;
			connectionRequestTwo.encode();
			this.sendRaw(connectionRequestTwo, address);

			this.loopSleep(500);
		}

		// If the session was set we are connected
		if (preConnection.serverSession != null) {
			// Set session data
			this.maximumTransferUnit = preConnection.maximumTransferUnit;
			this.session = preConnection.serverSession;
			preConnection.reset();

			// Send connection packet
			ConnectedConnectionRequest connectionRequest = new ConnectedConnectionRequest();
			connectionRequest.clientGuid = this.guid;
			connectionRequest.timestamp = (System.currentTimeMillis() - this.timestamp);
			connectionRequest.encode();
			session.sendPacket(Reliability.RELIABLE, connectionRequest);
			System.out.println("Connected to server! Sending packet...");
		} else {
			this.reset();
		}
	}

	public void connect(InetAddress address, int port) throws Exception {
		this.connect(new InetSocketAddress(address, port));
	}

	public void connect(String address, int port) throws Exception {
		this.connect(InetAddress.getByName(address), port);
	}

	private void reset() throws Exception {
		preConnection.reset();
		this.maximumTransferUnit = 0;
		if (session != null) {
			session.closeConnection("Disconnected");
			this.session = null;
		}
	}

	private void sendRaw(RakNetPacket packet, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	public void handleMessage(RakNetPacket packet, InetSocketAddress sender) {
		short packetId = packet.getId();

		if (sender.equals(preConnection.serverAddress)) {
			try {
				if (packetId == ID_OPEN_CONNECTION_REPLY_1) {
					UnconnectedOpenConnectionResponseOne connectionResponseOne = new UnconnectedOpenConnectionResponseOne(
							packet);
					connectionResponseOne.decode();

					if (connectionResponseOne.magic == true && connectionResponseOne.useSecurity == false
							&& connectionResponseOne.maximumTransferUnit > RakNet.MINIMUM_TRANSFER_UNIT
							&& connectionResponseOne.maximumTransferUnit < PHYSICAL_MAXIMUM_TRANSFER_UNIT) {
						preConnection.maximumTransferUnit = connectionResponseOne.maximumTransferUnit;
						preConnection.serverGuid = connectionResponseOne.serverGuid;
						preConnection.loginPackets[0] = true;
					} else {
						preConnection.cancelled = true;
						System.out.println("PACKET ONE");
					}
				} else if (packetId == ID_OPEN_CONNECTION_REPLY_2) {
					UnconnectedOpenConnectionResponseTwo connectionResponseTwo = new UnconnectedOpenConnectionResponseTwo(
							packet);
					connectionResponseTwo.decode();

					if (connectionResponseTwo.magic == true && connectionResponseTwo.encryptionEnabled == false
							&& connectionResponseTwo.serverGuid == preConnection.serverGuid
							&& connectionResponseTwo.maximumTransferUnit == preConnection.maximumTransferUnit) {
						preConnection.serverSession = new RakNetServerSession(this, preConnection.serverGuid,
								preConnection.maximumTransferUnit, channel, preConnection.serverAddress);
						preConnection.loginPackets[1] = true;
					} else {
						preConnection.cancelled = true;
					}
				}
			} catch (Exception e) {
				preConnection.cancelled = true;
				preConnection.cancelReason = e.getMessage();
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
