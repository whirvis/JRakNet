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
 * Copyright (c) 2016 Trent Summerlin
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
package net.marfgamer.raknet.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Scanner;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.event.Event;
import net.marfgamer.raknet.event.EventRunnable;
import net.marfgamer.raknet.event.server.ServerPingEvent;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.protocol.Message;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.identifier.MessageIdentifiers;
import net.marfgamer.raknet.protocol.raknet.ConnectedCloseConnection;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestTwo;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionResponseOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionResponseTwo;
import net.marfgamer.raknet.protocol.raknet.UnconnectedIncompatibleProtocol;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPing;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPong;
import net.marfgamer.raknet.protocol.raknet.UnconnectedServerFull;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.protocol.raknet.internal.EncapsulatedPacket;
import net.marfgamer.raknet.session.RakNetSession;
import net.marfgamer.raknet.session.RakNetState;
import net.marfgamer.raknet.utils.RakNetLogger;
import net.marfgamer.raknet.utils.RakNetUtils;

import org.apache.logging.log4j.LogManager;

public class RakNetServer implements RakNet, MessageIdentifiers {

	private final RakNetLogger logger;
	private final long serverId;

	// Server options
	private final int port;
	private final int maxConnections;
	private final int maxTransferUnit;
	private final String identifier;
	private HashMap<InetSocketAddress, RakNetSession> sessions;
	private HashMap<Class<? extends Event>, EventRunnable> events;

	// Netty data
	private final EventLoopGroup group;
	private final Bootstrap bootstrap;
	private volatile DatagramChannel channel;
	private final RakNetServerHandler handler;

	public RakNetServer(int port, int maxConnections, int maxTransferUnit,
			String identifier) {
		// Create logger
		this.logger = new RakNetLogger(LogManager.getLogger(this.getClass()
				.getName()));
		this.serverId = RakNetUtils.getRakNetID();
		logger.log("Initiating server with ID " + serverId + "...");

		// Set server options
		this.port = port;
		this.maxConnections = maxConnections;
		this.maxTransferUnit = maxTransferUnit;
		this.identifier = identifier;
		this.handler = new RakNetServerHandler(this);
		this.sessions = new HashMap<InetSocketAddress, RakNetSession>();
		this.events = new HashMap<Class<? extends Event>, EventRunnable>();
		logger.log("Set server options");

		// Set bootstrap data
		this.group = new NioEventLoopGroup();
		this.bootstrap = new Bootstrap();
		logger.log("Loaded NioEventLoopGroup and Bootstrap");
	}

	public RakNetServer(int port, int maxConnections, int maxTransferUnit) {
		this(port, maxConnections, maxTransferUnit, null);
	}

	public RakNetServer(int port, int maxConnections, String identifier) {
		this(port, maxConnections, RakNetUtils.getNetworkInterfaceMTU(),
				identifier);
	}

	public RakNetServer(int port, int maxConnections) {
		this(port, maxConnections, null);
	}

	/**
	 * Returns the server's port
	 * 
	 * @return int
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Returns the amount of connections the server can have at once
	 * 
	 * @return int
	 */
	public int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * Returns the server's MTU
	 * 
	 * @return int
	 */
	public int getMaxTransferUnit() {
		return this.maxTransferUnit;
	}

	/**
	 * Returns the server's identifier
	 * 
	 * @return String
	 */
	public String getIdentifier() {
		return this.identifier;
	}

	/**
	 * Adds a session to the server
	 * 
	 * @param session
	 *            The session to add
	 */
	public void addSession(RakNetSession session) {
		sessions.put(session.getSocketAddress(), session);
		logger.log("Added session with address " + session.getSocketAddress());
	}

	/**
	 * Removes a session from the server
	 * 
	 * @param session
	 *            The session to remove
	 * @param reason
	 *            The reason the session was removed
	 */
	public void removeSession(RakNetSession session, String reason) {
		sessions.remove(session.getAddress());
		logger.log("Removed session with address " + session.getSocketAddress()
				+ " (\"" + reason + "\")");
	}

	/**
	 * Removes a session from the server
	 * 
	 * @param session
	 *            The session to remove
	 */
	public void removeSession(RakNetSession session) {
		this.removeSession(session, "Removed from server");
	}

	/**
	 * Retrieves a session from the server based on its
	 * <code>InetSocketAddress</code>
	 * 
	 * @param address
	 *            The session's address
	 * @return RakNetSession
	 */
	public RakNetSession getSession(InetSocketAddress address) {
		return sessions.get(address);
	}

	/**
	 * Returns whether or not a session with the specified
	 * <code>InetSocketAddress</code> is currently connected to the server
	 * 
	 * @param address
	 *            The session's address
	 * @return boolean
	 */
	public boolean hasSession(InetSocketAddress address) {
		return sessions.containsKey(address);
	}

	/**
	 * Returns the connected RakNetSession's
	 * 
	 * @return RakNetSession
	 */
	public RakNetSession[] getSessions() {
		return sessions.values().toArray(new RakNetSession[sessions.size()]);
	}

	/**
	 * Adds a <code>Event</code> along with a <code>EventRunnable</code> to call
	 * when the specified event occurs
	 * 
	 * @param event
	 *            The event to listen for
	 * @param runnable
	 *            The code to execute when the event is thrown
	 */
	public void registerEvent(Class<? extends Event> event,
			EventRunnable runnable) {
		events.put(event, runnable);
		logger.log("Registered event listener for " + event.getName() + " to "
				+ runnable.getClass().getName());
	}

	/**
	 * Removes a <code>Event</code> and it's associated
	 * <code>EventRunnable</code>
	 * 
	 * @param event
	 *            The event to stop listening for
	 */
	public void unregisterEvent(Class<? extends Event> event) {
		events.remove(event);
		logger.log("Unregistered event listener for " + event.getName());
	}

	/**
	 * Calls the <code>EventRunnable</code> associated with the specified
	 * <code>Event</code> if it exists
	 * 
	 * @param event
	 *            The event to call
	 */
	public void callEvent(Event event) {
		if (events.containsKey(event.getClass())) {
			EventRunnable runnable = events.get(event.getClass());
			runnable.run(event);
			logger.log("Exectuded event " + event.getClass().getName()
					+ " in class " + runnable.getClass().getName());
		}
	}

	/**
	 * Sets an option in the server's Netty bootstrap
	 * 
	 * @param option
	 *            The option to set
	 * @param value
	 *            The option's new value
	 */
	public <T> void setBootstrapOption(ChannelOption<T> option, T value) {
		bootstrap.option(option, value);
		logger.log("Set bootstrap option " + option.name() + " to value "
				+ value);
	}

	public long getServerId() {
		return this.serverId;
	}

	public void handleRaw(Message packet, InetSocketAddress sender) {
		short pid = packet.getId();
		if (pid == ID_UNCONNECTED_PING) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();

			if (ping.magic == true) {
				ServerPingEvent pingEvent = new ServerPingEvent(
						this.identifier, sender);
				this.callEvent(pingEvent);

				if (pingEvent.getIdentifier() != null) {
					UnconnectedPong pong = new UnconnectedPong();
					pong.identifier = pingEvent.getIdentifier();
					pong.pingId = ping.pingId;
					pong.serverId = this.serverId;
					pong.encode();

					this.sendDatagram(pong, sender);
				}
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_REQUEST_1) {
			UnconnectedConnectionRequestOne connectionRequestOne = new UnconnectedConnectionRequestOne(
					packet);
			connectionRequestOne.decode();

			if (this.hasSession(sender) == false) {
				if (connectionRequestOne.magic == true
						&& connectionRequestOne.mtuSize <= this.maxTransferUnit) {
					if (connectionRequestOne.protocol == SERVER_NETWORK_PROTOCOL
							&& sessions.size() < this.maxConnections) {
						UnconnectedConnectionResponseOne connectionResponseOne = new UnconnectedConnectionResponseOne();
						connectionResponseOne.mtuSize = connectionRequestOne.mtuSize;
						connectionResponseOne.serverId = this.serverId;
						connectionResponseOne.encode();

						// Add session
						RakNetSession session = new RakNetSession(sender,
								channel);
						this.addSession(session);
						this.sendDatagram(connectionResponseOne, sender);
						session.setState(RakNetState.CONNECTING_1);
					} else if (connectionRequestOne.protocol != SERVER_NETWORK_PROTOCOL) {
						UnconnectedIncompatibleProtocol incompatibleProtocol = new UnconnectedIncompatibleProtocol();
						incompatibleProtocol.protocol = SERVER_NETWORK_PROTOCOL;
						incompatibleProtocol.serverId = this.serverId;
						incompatibleProtocol.encode();

						this.sendDatagram(incompatibleProtocol, sender);
					} else if (sessions.size() >= this.maxConnections) {
						UnconnectedServerFull serverFull = new UnconnectedServerFull();
						serverFull.encode();

						this.sendDatagram(serverFull, sender);
					}
				}
			}
		} else if (pid == ID_UNCONNECTED_CONNECTION_REQUEST_2) {
			UnconnectedConnectionRequestTwo connectionRequestTwo = new UnconnectedConnectionRequestTwo(
					packet);
			connectionRequestTwo.decode();

			if (this.hasSession(sender) == true) {
				RakNetSession session = this.getSession(sender);

				if (connectionRequestTwo.magic == true
						&& session.getState() == RakNetState.CONNECTING_1) {
					if (channel.localAddress().getPort() == connectionRequestTwo.serverAddress
							.getPort()) {
						UnconnectedConnectionResponseTwo connectionResponseTwo = new UnconnectedConnectionResponseTwo();
						connectionResponseTwo.serverId = this.serverId;
						connectionResponseTwo.mtuSize = (short) this.maxTransferUnit;
						connectionResponseTwo.clientAddress = sender;
						connectionResponseTwo.encode();

						this.sendDatagram(connectionResponseTwo, sender);
						session.setState(RakNetState.CONNECTING_2);
						this.kickClient(session);
					}
				}
			}
		} else if (pid >= ID_CUSTOM_0 && pid <= ID_CUSTOM_F) {
			CustomPacket custom = new CustomPacket(packet);
			custom.decode();

			if (this.hasSession(sender)) {
				RakNetSession session = this.getSession(sender);
				for (EncapsulatedPacket encapsulated : custom.packets) {
					session.handleDataPacket(encapsulated);
				}
			}
		}
		logger.log("Handled raw packet with ID " + RakNetUtils.toHexString(pid));
	}

	/**
	 * Sends a raw packet to the specified address
	 * 
	 * @param packet
	 *            The packet to send
	 * @param recipent
	 *            The address of the receiver
	 */
	private void sendDatagram(Message packet, InetSocketAddress recipent) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), recipent));
		logger.log("Sent datagram with ID "
				+ RakNetUtils.toHexString(packet.getId()) + " with size of "
				+ packet.length() + " bytes to " + recipent);
	}

	/**
	 * Starts the server
	 * 
	 * @throws RakNetException
	 *             Thrown if an error occurs in startup
	 */
	public void startServer() throws RakNetException {
		try {
			// Set bootstrap options
			bootstrap.group(group).channel(NioDatagramChannel.class)
					.handler(handler).option(ChannelOption.SO_REUSEADDR, false)
					.option(ChannelOption.SO_SNDBUF, this.maxTransferUnit)
					.option(ChannelOption.SO_RCVBUF, this.maxTransferUnit);
			logger.log("Loaded bootstrap and set default options");

			// Bind channel
			this.channel = (DatagramChannel) bootstrap.bind(
					InetAddress.getLocalHost(), this.port).channel();
			logger.log("Binded " + channel.getClass().getName() + " to "
					+ channel.localAddress());
		} catch (Exception e) {
			throw new RakNetException(e);
		}
	}

	/**
	 * Kicks the specified client from the server
	 * 
	 * @param session
	 *            The client to disconnect
	 * @param reason
	 *            The reason the client was disconnected
	 */
	public void kickClient(RakNetSession session, String reason) {
		ConnectedCloseConnection closeConnection = new ConnectedCloseConnection();
		closeConnection.encode();
		session.sendDataPacket(Reliability.UNRELIABLE, closeConnection);
		logger.log("Kicked client (\"" + reason + "\")");
	}

	/**
	 * Kicks the specified client from the server
	 * 
	 * @param session
	 *            The client to disconnect
	 */
	public void kickClient(RakNetSession session) {
		this.kickClient(session, "Kicked from server");
	}

	/**
	 * Kicks the client with the specified address from the server
	 * 
	 * @param address
	 *            The address of the client to disconnect
	 * @param reason
	 *            The reason the client was disconnected
	 */
	public void kickClient(InetSocketAddress address, String reason) {
		if (sessions.containsKey(address)) {
			this.kickClient(sessions.get(address), reason);
		}
	}

	/**
	 * Kicks the client with the specified address from the server
	 * 
	 * @param address
	 *            The address of the client to disconnect
	 */
	public void kickClient(InetSocketAddress address) {
		this.kickClient(address, "Kicked from server");
	}

	/**
	 * Kicks all clients with the specified base address from the server
	 * 
	 * @param address
	 *            The base address of the clients to disconnect
	 * @param reason
	 *            The reason the client was disconnected
	 */
	public void kickClient(InetAddress address, String reason) {
		for (RakNetSession session : this.getSessions()) {
			if (session.getAddress().equals(address)) {
				this.kickClient(session, reason);
			}
		}
	}

	/**
	 * Kicks all clients with the specified base address from the server
	 * 
	 * @param address
	 *            The base address of the clients to disconnect
	 */
	public void kickClient(InetAddress address) {
		this.kickClient(address, "Kicked from server");
	}

	/**
	 * Shuts down the server
	 * 
	 * @param disconnectClients
	 *            Whether or not each client should be disconnected
	 */
	public void shutdown(boolean disconnectClients) {
		if (disconnectClients == true) {
			for (RakNetSession session : this.getSessions()) {
				this.kickClient(session, "Server shutdown");
			}
		}
		group.shutdownGracefully();
	}

	/**
	 * Shuts down the server
	 */
	public void shutdown() {
		this.shutdown(false);
	}

	static String serverName = "The server name!";

	public static void main(String[] args) throws Exception {
		RakNetServer server = new RakNetServer(19132, 1);
		server.registerEvent(ServerPingEvent.class, (Event event) -> {
			ServerPingEvent ping = (ServerPingEvent) event;
			String identifier = "MCPE;" + serverName + ";81;0.15.0;0;10;"
					+ server.getServerId();
			ping.setIdentifier(identifier);
		});
		server.startServer();
		Scanner s = new Scanner(System.in);
		while (true) {
			if (s.hasNextLine()) {
				serverName = s.nextLine();
				if (serverName.length() <= 0) {
					serverName = "The server name!";
				}
				System.out.println("Set the server display name to: "
						+ serverName);
			}
		}
	}

}
