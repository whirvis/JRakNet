package net.marfgamer.raknet.server;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.net.InetSocketAddress;
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
import net.marfgamer.raknet.exception.NoListenerException;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.protocol.login.OpenConnectionRequestOne;
import net.marfgamer.raknet.protocol.login.OpenConnectionRequestTwo;
import net.marfgamer.raknet.protocol.login.OpenConnectionResponseOne;
import net.marfgamer.raknet.protocol.login.OpenConnectionResponseTwo;
import net.marfgamer.raknet.protocol.login.error.IncompatibleProtocol;
import net.marfgamer.raknet.protocol.login.error.NoFreeIncomingConnections;
import net.marfgamer.raknet.protocol.message.CustomPacket;
import net.marfgamer.raknet.protocol.message.acknowledge.Acknowledge;
import net.marfgamer.raknet.protocol.message.acknowledge.AcknowledgeReceipt;
import net.marfgamer.raknet.protocol.status.UnconnectedPing;
import net.marfgamer.raknet.protocol.status.UnconnectedPong;
import net.marfgamer.raknet.server.identifier.Identifier;
import net.marfgamer.raknet.server.identifier.MCPEIdentifier;
import net.marfgamer.raknet.session.RakNetClientSession;
import net.marfgamer.raknet.session.RakNetState;

/**
 * 
 *
 * @author MarfGamer
 */
public class RakNetServer implements RakNet {

	private final long guid;
	private final long timestamp;

	private final int port;
	private final int maxConnections;
	private final int maximumTransferUnit;
	private Identifier identifier;

	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetServerHandler handler;

	private Channel channel;
	private RakNetServerListener listener;
	private volatile boolean running; // Allow other threads to modify this
	private final ConcurrentHashMap<InetSocketAddress, RakNetClientSession> sessions;

	public RakNetServer(int port, int maxConnections, int maximumTransferUnit, Identifier identifier) {
		this.guid = UNIQUE_ID_BITS.getMostSignificantBits();
		this.timestamp = System.currentTimeMillis();

		this.port = port;
		this.maxConnections = maxConnections;
		this.maximumTransferUnit = maximumTransferUnit;
		this.identifier = identifier;

		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetServerHandler(this);

		this.sessions = new ConcurrentHashMap<InetSocketAddress, RakNetClientSession>();

		if (this.maximumTransferUnit < RakNet.MINIMUM_TRANSFER_UNIT) {
			throw new IllegalArgumentException(
					"Maximum transfer unit can be no smaller than " + RakNet.MINIMUM_TRANSFER_UNIT + "!");
		}
	}

	public RakNetServer(int port, int maxConnections, int maximumTransferUnit) {
		this(port, maxConnections, maximumTransferUnit, null);
	}

	public RakNetServer(int port, int maxConnections) {
		this(port, maxConnections, DEFAULT_MAXIMUM_TRANSFER_UNIT);
	}

	public RakNetServer(int port, int maxConnections, Identifier identifier) {
		this(port, maxConnections);
		this.identifier = identifier;
	}

	public long getGlobalilyUniqueId() {
		return this.guid;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public int getPort() {
		return this.port;
	}

	public int getMaxConnections() {
		return this.maxConnections;
	}

	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	public Identifier getIdentifier() {
		return this.identifier;
	}

	public RakNetServer setIdentifier(Identifier identifier) {
		this.identifier = identifier;
		return this;
	}

	public RakNetServerListener getListener() {
		return this.listener;
	}

	public RakNetServer setListener(RakNetServerListener listener) {
		this.listener = listener;
		return this;
	}

	public RakNetClientSession[] getSessions() {
		return sessions.values().toArray(new RakNetClientSession[sessions.size()]);
	}

	protected void handleMessage(RakNetPacket packet, InetSocketAddress sender) throws Exception {
		short packetId = packet.getId();

		if (packetId == ID_UNCONNECTED_PING || packetId == ID_UNCONNECTED_PING_OPEN_CONNECTIONS) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();

			if ((packetId == ID_UNCONNECTED_PING || sessions.size() < this.maxConnections) && identifier != null) {
				ServerPing pingEvent = new ServerPing(sender, identifier);
				listener.handlePing(pingEvent);
				if (pingEvent.getIdentifier() != null) {
					UnconnectedPong pong = new UnconnectedPong();
					pong.pingId = ping.pingId;
					pong.pongId = this.guid;
					pong.identifier = pingEvent.getIdentifier();

					pong.encode();
					this.sendRaw(pong, sender);
				}
			}
		} else if (packetId == ID_OPEN_CONNECTION_REQUEST_1) {
			OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne(packet);
			connectionRequestOne.decode();

			if (connectionRequestOne.magic == true) {
				if (connectionRequestOne.protocolVersion != RakNet.SERVER_NETWORK_PROTOCOL) {
					IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol();
					incompatibleProtocol.networkProtocol = RakNet.SERVER_NETWORK_PROTOCOL;
					incompatibleProtocol.serverGuid = this.guid;
					incompatibleProtocol.encode();
					this.sendRaw(incompatibleProtocol, sender);
				} else if (sessions.size() >= this.maxConnections) {
					NoFreeIncomingConnections noFreeIncomingConnections = new NoFreeIncomingConnections();
					noFreeIncomingConnections.encode();
					this.sendRaw(noFreeIncomingConnections, sender);
				} else {
					// Everything passed! One last check...
					if (connectionRequestOne.maximumTransferUnit < this.maximumTransferUnit) {
						OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne();
						connectionResponseOne.serverGuid = this.guid;
						connectionResponseOne.maximumTransferUnit = connectionRequestOne.maximumTransferUnit;
						connectionResponseOne.encode();
						this.sendRaw(connectionResponseOne, sender);
					}
				}
			}
		} else if (packetId == ID_OPEN_CONNECTION_REQUEST_2) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo(packet);
			connectionRequestTwo.decode();

			if (connectionRequestTwo.magic == true) {
				if (connectionRequestTwo.maximumTransferUnit < this.maximumTransferUnit) {
					// Create response
					OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo();
					connectionResponseTwo.serverGuid = this.guid;
					connectionResponseTwo.clientAddress = sender;
					connectionResponseTwo.maximumTransferUnit = connectionRequestTwo.maximumTransferUnit;
					connectionResponseTwo.encryptionEnabled = false;
					connectionResponseTwo.encode();

					// Call event
					this.getListener().clientPreConnected(sender);

					// Create session
					RakNetClientSession clientSession = new RakNetClientSession(this, System.currentTimeMillis(),
							connectionRequestTwo.clientGuid, connectionRequestTwo.maximumTransferUnit, channel, sender);
					sessions.put(sender, clientSession);

					// Send response, we are ready for login!
					this.sendRaw(connectionResponseTwo, sender);
				}
			}
		} else if (packetId >= ID_RESERVED_3 && packetId <= ID_RESERVED_9) {
			if (sessions.containsKey(sender) == true) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();

				RakNetClientSession session = sessions.get(sender);
				session.bumpLastPacketReceiveTime();
				session.handleCustom0(custom);
			}
		} else if (packetId == ID_SND_RECEIPT_ACKED || packetId == ID_SND_RECEIPT_LOSS) {
			if (sessions.containsKey(sender) == true) {
				AcknowledgeReceipt acknowledgeReceipt = new AcknowledgeReceipt(packet);
				acknowledgeReceipt.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleAcknowledgeReceipt(acknowledgeReceipt);
			}
		} else if (packetId == Acknowledge.ACKNOWLEDGED || packetId == Acknowledge.NOT_ACKNOWLEDGED) {
			if (sessions.containsKey(sender) == true) {
				Acknowledge acknowledge = new Acknowledge(packet);
				acknowledge.decode();

				RakNetClientSession session = sessions.get(sender);
				session.bumpLastPacketReceiveTime();
				session.handleAcknowledge(acknowledge);
			}
		}
	}

	private void sendRaw(Packet packet, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	public boolean hasSession(InetSocketAddress address) {
		return sessions.containsKey(address);
	}

	public RakNetClientSession getSession(InetSocketAddress address) {
		return sessions.get(address);
	}

	public void removeSession(RakNetClientSession session, String reason) {
		// We don't want to call clientDisconnected for an unconnected client
		if (sessions.containsKey(session.getAddress())) {
			sessions.remove(session.getAddress());
			if (session.getState() == RakNetState.CONNECTED) {
				this.getListener().clientDisconnected(session, reason);
			}
		}
	}

	public void removeSession(InetSocketAddress address, String reason) {
		this.removeSession(sessions.get(address), reason);
	}

	public void start() throws RakNetException {
		// Make sure we have an adapter
		if (listener == null) {
			throw new NoListenerException("The listener must be set in order to start the server!");
		}

		// Create bootstrap and bind the channel
		bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
		bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
		this.channel = bootstrap.bind(port).channel();
		this.running = true;

		// Timer system
		while (this.running == true) {
			for (RakNetClientSession session : sessions.values()) {
				try {
					session.update();
				} catch (Exception e) {
					this.removeSession(session, e.getMessage());
				}
			}
		}
	}

	public synchronized Thread startThreaded() {
		// Give the thread a reference
		RakNetServer server = this;

		// Create thread and start it
		Thread thread = new Thread() {
			@Override
			public synchronized void run() {
				try {
					server.start();
				} catch (RakNetException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();

		// Return the thread so it can be modified
		return thread;
	}

	public void stop() {
		this.running = false;
		this.getListener().serverShutdown();
	}

	public static void main(String[] args) throws RakNetException {
		MCPEIdentifier identifier = new MCPEIdentifier("A JRakNet Server", 80, "0.15.0", 0, 10);
		RakNetServer s = new RakNetServer(19132, 10, identifier);

		s.setListener(new RakNetServerListener() {
			@Override
			public void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
				System.out.println("Received packet with ID 0x" + Integer.toHexString(packet.getId()).toUpperCase()
						+ " from " + session.getAddress());
			}

			@Override
			public void clientPreConnected(InetSocketAddress address) {
				System.out.println("Client from " + address
						+ " has instantiated the connection, waiting for NEW_INCOMING_CONNECTION packet");
			}

			@Override
			public void clientConnected(RakNetClientSession session) {
				System.out.println("Client from " + session.getAddress() + " has connected");
			}

			@Override
			public void clientDisconnected(RakNetClientSession session, String reason) {
				System.out.println("Client " + session.getAddress() + " because \"" + reason + "\"");
			}
		});
		s.start();
	}

}
