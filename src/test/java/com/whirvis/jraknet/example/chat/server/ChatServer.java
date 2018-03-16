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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.example.chat.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.UtilityTest;
import com.whirvis.jraknet.example.chat.ChatMessageIdentifier;
import com.whirvis.jraknet.example.chat.ServerChannel;
import com.whirvis.jraknet.example.chat.protocol.LoginFailure;
import com.whirvis.jraknet.example.chat.protocol.UpdateUsername;
import com.whirvis.jraknet.example.chat.server.command.BroadcastCommand;
import com.whirvis.jraknet.example.chat.server.command.ChannelCommand;
import com.whirvis.jraknet.example.chat.server.command.CommandHandler;
import com.whirvis.jraknet.example.chat.server.command.HelpCommand;
import com.whirvis.jraknet.example.chat.server.command.KickCommand;
import com.whirvis.jraknet.example.chat.server.command.StopCommand;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import com.whirvis.jraknet.session.InvalidChannelException;
import com.whirvis.jraknet.session.RakNetClientSession;

/**
 * A simple chat server built using JRakNet.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class ChatServer implements RakNetServerListener {

	private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

	// Server data
	private final String name;
	private final String motd;
	private final RakNetServer server;
	private final ServerChannel[] serverChannels;
	private final HashMap<InetSocketAddress, ConnectedClient> connected;

	/**
	 * Constructs a <code>ChatServer</code> with the specified name, message of
	 * the day, port, and maximum amount of connections.
	 * 
	 * @param name
	 *            the name.
	 * @param motd
	 *            the message of the day.
	 * @param port
	 *            the port.
	 * @param maxConnections
	 *            the maximum amount of connections.
	 */
	public ChatServer(String name, String motd, int port, int maxConnections) {
		this.name = name;
		this.motd = motd;
		this.server = new RakNetServer(port, maxConnections);
		this.serverChannels = new ServerChannel[RakNet.MAX_CHANNELS];
		serverChannels[0] = new ServerChannel(0, "Main hall");
		this.connected = new HashMap<InetSocketAddress, ConnectedClient>();
	}

	/**
	 * Starts the server.
	 */
	public void start() {
		server.addListener(this);
		server.startThreaded();
	}

	/**
	 * Stops the server.
	 */
	public void stop() {
		server.shutdown();
	}

	/**
	 * Denies a login to the specified client with the specified reason.
	 * 
	 * @param session
	 *            the session to deny the login to.
	 * @param reason
	 *            the reason the login was denied.
	 */
	private void denyLogin(RakNetClientSession session, String reason) {
		LoginFailure loginFailure = new LoginFailure();
		loginFailure.reason = reason;
		session.sendMessage(Reliability.UNRELIABLE, loginFailure);
	}

	/**
	 * @param username
	 *            the username to check.
	 * @return <code>true</code> if the server has a client with the specified
	 *         username.
	 */
	private boolean hasUsername(String username) {
		for (ConnectedClient client : connected.values()) {
			if (client.getUsername().equalsIgnoreCase(username)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds a channel to the server with the specified ID and name.
	 * 
	 * @param channel
	 *            the channel ID.
	 * @param name
	 *            the name of the channel.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 */
	public void addChannel(int channel, String name) throws InvalidChannelException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}
		serverChannels[channel] = new ServerChannel(channel, name);
		for (ConnectedClient client : connected.values()) {
			client.addChannel(channel, name);
		}
	}

	/**
	 * Renames the channel with the specified ID a new name.
	 * 
	 * @param channel
	 *            the channel ID.
	 * @param name
	 *            the new name of the channel.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 */
	public void renameChannel(int channel, String name) throws InvalidChannelException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}
		serverChannels[channel].setName(name);
		for (ConnectedClient client : connected.values()) {
			client.renameChannel(channel, name);
		}
	}

	/**
	 * Removes the channel with the specified ID.
	 * 
	 * @param channel
	 *            the channel ID.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 */
	public void removeChannel(int channel) throws InvalidChannelException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}
		serverChannels[channel] = null;
		for (ConnectedClient client : connected.values()) {
			client.removeChannel(channel);
		}
	}

	/**
	 * @param channel
	 *            the channel ID.
	 * @return <code>true</code> if the server has a channel with the specified
	 *         ID.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 */
	public boolean hasChannel(int channel) throws InvalidChannelException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}
		return (serverChannels[channel] != null);
	}

	/**
	 * @param channel
	 *            the channel ID.
	 * @return the channel's name.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 */
	public String getChannelName(int channel) throws InvalidChannelException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}
		return serverChannels[channel].getName();
	}

	/**
	 * @return All (non-null) channels on the server.
	 */
	public ServerChannel[] getChannels() {
		ArrayList<ServerChannel> channels = new ArrayList<ServerChannel>();
		for (ServerChannel channel : this.serverChannels) {
			if (channel != null) {
				channels.add(channel);
			}
		}
		return channels.toArray(new ServerChannel[channels.size()]);
	}

	/**
	 * Broadcasts the specified message to the specified channel and prints it
	 * out to the console if needed.
	 * 
	 * @param message
	 *            the message to send.
	 * @param channel
	 *            the channel to send the message to.
	 * @param print
	 *            Whether or not the message should be printed to the console.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 */
	private void broadcastMessage(String message, int channel, boolean print) throws InvalidChannelException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}
		for (ConnectedClient client : connected.values()) {
			client.sendChatMessage(message, channel);
		}
		if (print == true) {
			log.info(message + " [" + serverChannels[channel].getName() + "]");
		}
	}

	/**
	 * Broadcasts the specified message on the specified channel.
	 * 
	 * @param message
	 *            the message to send.
	 * @param channel
	 *            the channel to send the message to.
	 */
	public void broadcastMessage(String message, int channel) {
		this.broadcastMessage(message, channel, true);
	}

	/**
	 * Broadcasts the specified message on every channel possible.
	 * 
	 * @param message
	 *            the message to send.
	 */
	public void broadcastMessage(String message) {
		for (ServerChannel channel : getChannels()) {
			this.broadcastMessage(message + " [Global]", channel.getChannel(), false);
		}
		log.info(message + " [Global]");
	}

	/**
	 * @param username
	 *            the client's username.
	 * @return the client based on it's username.
	 */
	public ConnectedClient getClient(String username) {
		for (ConnectedClient client : connected.values()) {
			if (client.getUsername().equals(username)) {
				return client;
			}
		}
		return null;
	}

	/**
	 * @param username
	 *            the username to check.
	 * @return <code>true</code> if the server has a client with the specified
	 *         username.
	 */
	public boolean hasClient(String username) {
		return (getClient(username) != null);
	}

	/**
	 * Kicks a client from the server with the specified reason.
	 * 
	 * @param client
	 *            the client to kick.
	 * @param reason
	 *            the reason the client was kicked.
	 */
	public void kickClient(ConnectedClient client, String reason) {
		if (connected.containsKey(client.getSession().getAddress())) {
			client.kick(reason);
			connected.remove(client.getSession().getAddress());
		}
	}

	@Override
	public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
		InetSocketAddress sender = session.getAddress();
		short packetId = packet.getId();

		if (packetId == ChatMessageIdentifier.ID_LOGIN_REQUEST) {
			if (!connected.containsKey(sender)) {
				// Should we add the user?
				String username = packet.readString();
				if (username.length() <= 0) {
					this.denyLogin(session, "Username is too short!");
					return;
				} else if (hasUsername(username)) {
					this.denyLogin(session, "Client with that name is already on this server!");
					return;
				}

				// Accept request
				ConnectedClient client = new ConnectedClient(session, UUID.randomUUID(), username);
				connected.put(sender, client);
				client.acceptLogin(this.name, this.motd, this.getChannels());

				// Welcome our new user!
				this.broadcastMessage(username + " has connected");
			}
		} else if (packetId == ChatMessageIdentifier.ID_CHAT_MESSAGE) {
			if (connected.containsKey(sender)) {
				// Broadcast the chat message
				ConnectedClient client = connected.get(sender);
				String message = packet.readString();
				if (message.length() > 0) {
					broadcastMessage("<" + client.getUsername() + "> " + message, channel);
				}
			}
		} else if (packetId == ChatMessageIdentifier.ID_UPDATE_USERNAME_REQUEST) {
			if (connected.containsKey(sender)) {
				ConnectedClient client = connected.get(sender);
				UpdateUsername request = new UpdateUsername(packet);
				request.decode();

				// Do any other clients have the same name already?
				if (!this.hasUsername(request.newUsername)) {
					this.broadcastMessage(client.getUsername() + " has changed their name to " + request.newUsername);
					client.acceptUsernameUpdate(request.newUsername);
				} else {
					client.denyUsernameUpdate();
				}
			}
		}
	}

	@Override
	public void onClientDisconnect(RakNetClientSession session, String reason) {
		if (connected.containsKey(session.getAddress())) {
			broadcastMessage(connected.get(session.getAddress()).getUsername() + " has disconnected");
			connected.remove(session.getAddress());
		}
	}

	public static void main(String[] args) {
		// Create and start server
		ChatServer server = new ChatServer("JRakNet Server Example", "This is a test server made for JRakNet",
				UtilityTest.WHIRVIS_DEVELOPMENT_PORT, 10);
		server.start();
		log.info("Started server!");

		// Register commands
		CommandHandler commandHandler = new CommandHandler();
		commandHandler.registerCommand(new StopCommand(server));
		commandHandler.registerCommand(new KickCommand(server));
		commandHandler.registerCommand(new ChannelCommand(server));
		commandHandler.registerCommand(new BroadcastCommand(server));
		commandHandler.registerCommand(HelpCommand.class);

		// Listen for commands
		@SuppressWarnings("resource")
		Scanner commandScanner = new Scanner(System.in);
		while (true) {
			if (commandScanner.hasNextLine()) {
				String input = commandScanner.nextLine();
				commandHandler.handleInput(input);
			}

			try {
				Thread.sleep(0, 1); // Lower CPU usage
			} catch (InterruptedException e) {
				log.warn("Chat server sleep interrupted");
			}
		}
	}

}
