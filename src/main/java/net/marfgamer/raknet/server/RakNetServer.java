package net.marfgamer.raknet.server;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.net.InetAddress;
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
import net.marfgamer.raknet.identifier.Identifier;
import net.marfgamer.raknet.identifier.MCPEIdentifier;
import net.marfgamer.raknet.protocol.login.OpenConnectionRequestOne;
import net.marfgamer.raknet.protocol.login.OpenConnectionRequestTwo;
import net.marfgamer.raknet.protocol.login.OpenConnectionResponseOne;
import net.marfgamer.raknet.protocol.login.OpenConnectionResponseTwo;
import net.marfgamer.raknet.protocol.login.error.AlreadyConnected;
import net.marfgamer.raknet.protocol.login.error.ConnectionBanned;
import net.marfgamer.raknet.protocol.login.error.IncompatibleProtocol;
import net.marfgamer.raknet.protocol.login.error.NoFreeIncomingConnections;
import net.marfgamer.raknet.protocol.message.CustomPacket;
import net.marfgamer.raknet.protocol.message.acknowledge.Acknowledge;
import net.marfgamer.raknet.protocol.message.acknowledge.AcknowledgeReceipt;
import net.marfgamer.raknet.protocol.status.UnconnectedPing;
import net.marfgamer.raknet.protocol.status.UnconnectedPong;
import net.marfgamer.raknet.session.RakNetClientSession;
import net.marfgamer.raknet.session.RakNetSession;
import net.marfgamer.raknet.session.RakNetState;
import net.marfgamer.raknet.util.RakNetUtils;

/**
 * This class is used to easily create servers using the RakNet protocol
 *
 * @author MarfGamer
 */
public class RakNetServer implements RakNet {

	// Server data
	private final long guid;
	private final long timestamp;
	private final int port;
	private final int maxConnections;
	private final int maximumTransferUnit;
	private Identifier identifier;

	// Networking data
	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetServerHandler handler;

	// Session data
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
		this(port, maxConnections, RakNetUtils.getMaximumTransferUnit());
	}

	public RakNetServer(int port, int maxConnections, Identifier identifier) {
		this(port, maxConnections);
		this.identifier = identifier;
	}

	/**
	 * Returns the server's globally unique ID (GUID)
	 * 
	 * @return The server's globally unique ID
	 */
	public long getGlobalilyUniqueId() {
		return this.guid;
	}

	/**
	 * Returns the server's timestamp (how long ago it started in milliseconds)
	 * 
	 * @return The server's timestamp
	 */
	public long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	/**
	 * Returns the port the server is bound to
	 * 
	 * @return The port the server is bound to
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Returns the maximum amount of connections the server can handle at once
	 * 
	 * @return The maximum amount of connections the server can handle at once
	 */
	public int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * 
	 */
	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Returns the identifier the server uses for discovery
	 * 
	 * @return The identifier the server uses for discovery
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Sets the server's identifier used for discovery
	 * 
	 * @param identifier
	 *            - The new identifier
	 * @return The server
	 */
	public RakNetServer setIdentifier(Identifier identifier) {
		this.identifier = identifier;
		return this;
	}

	/**
	 * Returns the server's listener
	 * 
	 * @return The server's listener
	 */
	public RakNetServerListener getListener() {
		return this.listener;
	}

	/**
	 * Sets the server's listener
	 * 
	 * @param listener
	 *            - The new listener
	 * @return The server
	 */
	public RakNetServer setListener(RakNetServerListener listener) {
		if (listener == null) {
			throw new NullPointerException("Listener must not be null!");
		}
		this.listener = listener;
		return this;
	}

	/**
	 * Returns the sessions connected to the server
	 * 
	 * @return The sessions connected to the server
	 */
	public RakNetClientSession[] getSessions() {
		return sessions.values().toArray(new RakNetClientSession[sessions.size()]);
	}

	/**
	 * Returns the amount of sessions connected to the server
	 * 
	 * @return The amount of sessions connected to the server
	 */
	public int getSessionCount() {
		return sessions.size();
	}

	/**
	 * Returns whether or not the server has a session with the specified
	 * address
	 * 
	 * @param address
	 *            - The address to check
	 * @return - Whether or not the server has a session with the specified
	 *         address
	 */
	public boolean hasSession(InetSocketAddress address) {
		return sessions.containsKey(address);
	}

	/**
	 * Returns a session connected to the server by their address
	 * 
	 * @param address
	 *            - The address of the session
	 * @return A session connected to the server by their address
	 */
	public RakNetClientSession getSession(InetSocketAddress address) {
		return sessions.get(address);
	}

	/**
	 * Removes a session from the server with the specified reason
	 * 
	 * @param address
	 *            - The address of the session
	 * @param reason
	 *            - The reason the session was removed
	 */
	public void removeSession(InetSocketAddress address, String reason) {
		if (sessions.containsKey(address)) {
			RakNetClientSession session = sessions.get(address);
			if (session.getState() == RakNetState.CONNECTED) {
				listener.onClientDisconnect(session, reason);
			}
			sessions.remove(address);
		}
	}

	/**
	 * Removes a session from the server
	 * 
	 * @param address
	 *            - The address of the session
	 */
	public void removeSession(InetSocketAddress address) {
		this.removeSession(address, "Disconnected from server");
	}

	/**
	 * Removes a session from the server with the specified reason
	 * 
	 * @param session
	 *            - The session to remove
	 * @param reason
	 *            - The reason the session was removed
	 */
	public void removeSession(RakNetClientSession session, String reason) {
		this.removeSession(session.getAddress(), reason);
	}

	/**
	 * Removes a session from the server
	 * 
	 * @param session
	 *            - The session to remove
	 */
	public void removeSession(RakNetClientSession session) {
		this.removeSession(session, "Disconnected from server");
	}

	/**
	 * Blocks the address and disconnects all the clients on the address with
	 * the specified reason for the specified amount of time
	 * 
	 * @param address
	 *            - The address to block
	 * @param reason
	 *            - The reason the address was blocked
	 * @param time
	 *            - How long the address will blocked in milliseconds
	 */
	public void blockAddress(InetAddress address, String reason, long time) {
		for (InetSocketAddress clientAddress : sessions.keySet()) {
			if (clientAddress.getAddress().getAddress().equals(address)) {
				this.removeSession(clientAddress, reason);
			}
		}
		handler.blockAddress(address, time);
	}

	/**
	 * Blocks the address and disconnects all the clients on the address for the
	 * specified amount of time
	 * 
	 * @param address
	 *            - The address to block
	 * @param time
	 *            - How long the address will blocked in milliseconds
	 */
	public void blockAddress(InetAddress address, long time) {
		this.blockAddress(address, "Blocked", time);
	}

	/**
	 * Unblocks the specified address
	 * 
	 * @param address
	 *            - The address to unblock
	 */
	public void unblockAddress(InetAddress address) {
		handler.unblockAddress(address);
	}

	/**
	 * Blocks the address and disconnects all the clients on the address with
	 * the specified reason for the specified amount of time
	 * 
	 * @param address
	 *            - The address to block
	 * @param reason
	 *            - The reason the address was blocked
	 * @param time
	 *            - How long the address will blocked in milliseconds
	 */
	public void blockAddress(InetSocketAddress address, String reason, long time) {
		this.blockAddress(address.getAddress(), reason, time);
	}

	/**
	 * Blocks the address and disconnects all the clients on the address for the
	 * specified amount of time
	 * 
	 * @param address
	 *            - The address to block
	 * @param time
	 *            - How long the address will blocked in milliseconds
	 */
	public void blockAddress(InetSocketAddress address, long time) {
		this.blockAddress(address, "Blocked", time);
	}

	/**
	 * Unblocks the specified address
	 * 
	 * @param address
	 *            - The address to unblock
	 */
	public void unblockAddress(InetSocketAddress address) {
		this.unblockAddress(address.getAddress());
	}

	/**
	 * Handles a packet received by the handler
	 * 
	 * @param packet
	 *            - The packet to handle
	 * @param sender
	 *            - The address of the sender
	 */
	protected void handleMessage(RakNetPacket packet, InetSocketAddress sender) {
		short packetId = packet.getId();

		if (packetId == ID_UNCONNECTED_PING || packetId == ID_UNCONNECTED_PING_OPEN_CONNECTIONS) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();

			if ((packetId == ID_UNCONNECTED_PING || sessions.size() < this.maxConnections) && identifier != null) {
				ServerPing pingEvent = new ServerPing(sender, identifier);
				listener.handlePing(pingEvent);
				if (ping.magic == true && pingEvent.getIdentifier() != null) {
					UnconnectedPong pong = new UnconnectedPong();
					pong.pingId = ping.timestamp;
					pong.pongId = this.getTimestamp();
					pong.identifier = pingEvent.getIdentifier();

					pong.encode();
					this.sendRawMessage(pong, sender);
				}
			}
		} else if (packetId == ID_OPEN_CONNECTION_REQUEST_1) {
			OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne(packet);
			connectionRequestOne.decode();

			if (connectionRequestOne.magic == true) {
				// Are there any problems?
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (connectionRequestOne.protocolVersion != RakNet.SERVER_NETWORK_PROTOCOL) {
						// Incompatible protocol!
						IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol();
						incompatibleProtocol.networkProtocol = RakNet.SERVER_NETWORK_PROTOCOL;
						incompatibleProtocol.serverGuid = this.guid;
						incompatibleProtocol.encode();
						this.sendRawMessage(incompatibleProtocol, sender);
					} else {
						// Everything passed! One last check...
						if (connectionRequestOne.maximumTransferUnit < this.maximumTransferUnit) {
							OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne();
							connectionResponseOne.serverGuid = this.guid;
							connectionResponseOne.maximumTransferUnit = connectionRequestOne.maximumTransferUnit;
							connectionResponseOne.encode();
							this.sendRawMessage(connectionResponseOne, sender);
						}
					}
				} else {
					this.sendRawMessage(errorPacket, sender);
				}
			}
		} else if (packetId == ID_OPEN_CONNECTION_REQUEST_2) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo(packet);
			connectionRequestTwo.decode();

			if (connectionRequestTwo.magic == true) {
				// Are there any problems?
				RakNetPacket errorPacket = this.validateSender(sender);
				if (errorPacket == null) {
					if (connectionRequestTwo.maximumTransferUnit < this.maximumTransferUnit) {
						// Create response
						OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo();
						connectionResponseTwo.serverGuid = this.guid;
						connectionResponseTwo.clientAddress = sender;
						connectionResponseTwo.maximumTransferUnit = connectionRequestTwo.maximumTransferUnit;
						connectionResponseTwo.encryptionEnabled = false;
						connectionResponseTwo.encode();

						// Call event
						this.getListener().onClientPreConnect(sender);

						// Create session
						RakNetClientSession clientSession = new RakNetClientSession(this, System.currentTimeMillis(),
								connectionRequestTwo.clientGuid, connectionRequestTwo.maximumTransferUnit, channel,
								sender);
						sessions.put(sender, clientSession);

						// Send response, we are ready for login!
						this.sendRawMessage(connectionResponseTwo, sender);
					}
				} else {
					this.sendRawMessage(errorPacket, sender);
				}
			}
		} else if (packetId >= ID_RESERVED_3 && packetId <= ID_RESERVED_9) {
			if (sessions.containsKey(sender)) {
				CustomPacket custom = new CustomPacket(packet);
				custom.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleCustom0(custom);
			}
		} else if (packetId == ID_SND_RECEIPT_ACKED || packetId == ID_SND_RECEIPT_LOSS) {
			if (sessions.containsKey(sender)) {
				AcknowledgeReceipt acknowledgeReceipt = new AcknowledgeReceipt(packet);
				acknowledgeReceipt.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleAcknowledgeReceipt(acknowledgeReceipt);
			}
		} else if (packetId == Acknowledge.ACKNOWLEDGED || packetId == Acknowledge.NOT_ACKNOWLEDGED) {
			if (sessions.containsKey(sender)) {
				Acknowledge acknowledge = new Acknowledge(packet);
				acknowledge.decode();

				RakNetClientSession session = sessions.get(sender);
				session.handleAcknowledge(acknowledge);
			}
		}
	}

	/**
	 * Validates the sender during login to make sure there are no problems
	 * 
	 * @param sender
	 *            - The address of the packet sender
	 * @return The packet to respond with if there was an error
	 */
	private RakNetPacket validateSender(InetSocketAddress sender) {
		// Checked throughout all login
		if (sessions.containsKey(sender)) {
			// This client is already connected!
			AlreadyConnected alreadyConnected = new AlreadyConnected();
			alreadyConnected.encode();
			return alreadyConnected;
		} else if (sessions.size() >= this.maxConnections) {
			// We have no free connections!
			NoFreeIncomingConnections noFreeIncomingConnections = new NoFreeIncomingConnections();
			noFreeIncomingConnections.encode();
			return noFreeIncomingConnections;
		} else if (handler.addressBlocked(sender.getAddress())) {
			// Address is blocked!
			ConnectionBanned connectionBanned = new ConnectionBanned();
			connectionBanned.encode();
			return connectionBanned;
		}

		// There were no errors
		return null;
	}

	/**
	 * Sends a raw message to the specified address
	 * 
	 * @param packet
	 *            - The packet to send
	 * @param address
	 *            - The address to send the packet to
	 */
	private void sendRawMessage(Packet packet, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	/**
	 * Starts the server
	 * 
	 * @throws NoListenerException
	 *             - Thrown if the listener has not yet been set
	 */
	public void start() throws NoListenerException {
		// Make sure we have an adapter
		if (listener == null) {
			throw new NoListenerException("The listener must be set in order to start the server!");
		}

		// Create bootstrap and bind the channel
		bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
		bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
		this.channel = bootstrap.bind(port).channel();
		this.running = true;

		// Notify API
		listener.onServerStart();

		// Update system
		while (this.running == true) {
			for (RakNetClientSession session : sessions.values()) {
				try {
					// Update session and make sure it isn't DOSing us
					session.update();
					if (session.getPacketsReceivedThisSecond() >= RakNet.MAX_PACKETS_PER_SECOND) {
						this.blockAddress(session.getAddress(), "Too many packets",
								RakNetSession.MAX_PACKETS_PER_SECOND_BLOCK);
					}
				} catch (Exception e) {
					// An error related to the session occurred, remove it!
					this.removeSession(session, e.getMessage());
				}
			}
		}
	}

	/**
	 * Starts the server on it's own Thread
	 * 
	 * @return The Thread the server is running on
	 */
	public synchronized Thread startThreaded() {
		// Give the thread a reference
		RakNetServer server = this;

		// Create thread and start it
		Thread thread = new Thread() {
			@Override
			public synchronized void run() {
				server.start();
			}
		};
		thread.start();

		// Return the thread so it can be modified
		return thread;
	}

	/**
	 * Stops the server
	 */
	public void stop() {
		this.running = false;
		for (RakNetClientSession session : sessions.values()) {
			this.removeSession(session, "Server shutdown");
		}
		sessions.clear();
		this.getListener().onServerShutdown();
	}

	public static void main(String[] args) {
		MCPEIdentifier identifier = new MCPEIdentifier("A JRakNet Server", 80, "0.15.0", 0, 10, -1, "TheBestWorld",
				"Developer");
		RakNetServer s = new RakNetServer(19132, 10, identifier);

		s.setListener(new RakNetServerListener() {
			@Override
			public void onAddressUnblocked(InetAddress address) {
				System.out.println(address + " has been unblocked");
			}

			@Override
			public void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
				System.out.println("Received packet with ID 0x" + Integer.toHexString(packet.getId()).toUpperCase()
						+ " from " + session.getAddress());
			}

			@Override
			public void onClientPreConnect(InetSocketAddress address) {
				System.out.println("Client from " + address
						+ " has instantiated the connection, waiting for NEW_INCOMING_CONNECTION packet");
			}

			@Override
			public void onClientConnect(RakNetClientSession session) {
				System.out.println("Client from " + session.getAddress() + " has connected");
			}

			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				System.out.println("Client " + session.getAddress() + " because \"" + reason + "\"");
			}
		});
		s.start();
	}

}
