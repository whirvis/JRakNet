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
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
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
package com.whirvis.jraknet.chat.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.InvalidChannelException;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.RakNetTest;
import com.whirvis.jraknet.chat.TextChannel;
import com.whirvis.jraknet.chat.protocol.ChatMessage;
import com.whirvis.jraknet.chat.protocol.ChatPacket;
import com.whirvis.jraknet.chat.protocol.LoginFailure;
import com.whirvis.jraknet.chat.protocol.LoginRequest;
import com.whirvis.jraknet.chat.protocol.UpdateUsernameRequest;
import com.whirvis.jraknet.chat.server.command.BroadcastCommand;
import com.whirvis.jraknet.chat.server.command.ChannelCommand;
import com.whirvis.jraknet.chat.server.command.CommandHandler;
import com.whirvis.jraknet.chat.server.command.HelpCommand;
import com.whirvis.jraknet.chat.server.command.KickCommand;
import com.whirvis.jraknet.chat.server.command.StopCommand;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.server.RakNetServer;

/**
 * A simple chat server built using JRakNet.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class ChatServer extends RakNetServer {

	private static final Logger LOG = LogManager.getLogger(ChatServer.class);

	private final String name;
	private final String motd;
	private final TextChannel[] channels;
	private final HashMap<InetSocketAddress, ChatUser> users;

	/**
	 * Creates a chat server.
	 * 
	 * @param port
	 *            the port the server will bind to during startup.
	 * @param maxConnections
	 *            the maximum number of connections, A value of
	 *            {@value RakNetServer#INFINITE_CONNECTIONS} will allow for an
	 *            infinite number of connections.
	 * @param name
	 *            the server name.
	 * @param motd
	 *            the server message of the day.
	 * @throws NullPointerException
	 *             if the server <code>name</code> or server <code>motd</code>
	 *             are <code>null</code>.
	 */
	private ChatServer(int port, int maxConnections, String name, String motd) throws NullPointerException {
		super(port, maxConnections);
		if (name == null) {
			throw new NullPointerException("Server name cannot be null");
		} else if (motd == null) {
			throw new NullPointerException("Server message of the day cannot be null");
		}
		this.name = name;
		this.motd = motd;
		this.channels = new TextChannel[RakNet.CHANNEL_COUNT];
		this.users = new HashMap<InetSocketAddress, ChatUser>();
		this.addChannel(0, "Main hall");
		this.addSelfListener();
	}

	/**
	 * Returns the server name.
	 * 
	 * @return the server name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the server message of the day.
	 * 
	 * @return the server message of the day.
	 */
	public String getMotd() {
		return this.motd;
	}

	/**
	 * Returns whether or not the server has the channel with the specified ID.
	 * 
	 * @param channel
	 *            the channel ID.
	 * @return <code>true</code> if the server has the channel,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasChannel(int channel) {
		if (channel < channels.length) {
			return channels[channel] != null;
		}
		return false;
	}

	/**
	 * Returns all channels.
	 * 
	 * @return all channels.
	 */
	public TextChannel[] getChannels() {
		ArrayList<TextChannel> cleansed = new ArrayList<TextChannel>();
		for (TextChannel channel : channels) {
			if (channel != null) {
				cleansed.add(channel);
			}
		}
		return cleansed.toArray(new TextChannel[cleansed.size()]);
	}

	/**
	 * Returns the text channel with the specified channel ID.
	 * 
	 * @param channel
	 *            the channel ID.
	 * @return the text channel, <code>null</code> if it does not exist.
	 */
	public TextChannel getChannel(int channel) {
		if (channel < channels.length) {
			return channels[channel];
		}
		return null;
	}

	/**
	 * Adds a channel to the server.
	 * 
	 * @param channel
	 *            the channel ID.
	 * @param name
	 *            the name of the channel.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code>. ID is greater than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 * @throws NullPointerException
	 *             if the <code>name</code> is <code>null</code>.
	 */
	public void addChannel(int channel, String name) throws InvalidChannelException, NullPointerException {
		if (channel >= channels.length) {
			throw new InvalidChannelException(channel);
		} else if (name == null) {
			throw new NullPointerException("Channel name cannot be null");
		}
		this.channels[channel] = new TextChannel(channel, name);
		for (ChatUser user : users.values()) {
			user.addChannel(channel, name);
		}
	}

	/**
	 * Renames the channel.
	 * 
	 * @param channel
	 *            the channel ID.
	 * @param name
	 *            the new name of the channel.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code>. ID is greater than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 * @throws NullPointerException
	 *             if the <code>name</code> is <code>null</code>.
	 */
	public void renameChannel(int channel, String name) throws InvalidChannelException {
		if (channel >= channels.length) {
			throw new InvalidChannelException(channel);
		} else if (name == null) {
			throw new NullPointerException("Channel name cannot be null");
		}
		channels[channel].setName(name);
		for (ChatUser user : users.values()) {
			user.renameChannel(channel, name);
		}
	}

	/**
	 * Removes the channel.
	 * 
	 * @param channel
	 *            the channel ID.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code>. ID is greater than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public void removeChannel(int channel) throws InvalidChannelException {
		if (channel >= channels.length) {
			throw new InvalidChannelException(channel);
		}
		this.channels[channel] = null;
		for (ChatUser user : users.values()) {
			user.removeChannel(channel);
		}
	}

	/**
	 * Broadcasts the specified message on the specified channel.
	 * 
	 * @param message
	 *            the message to send.
	 * @param channel
	 *            the channel to send the message to.
	 * @param print
	 *            <code>true</code> the message should be printed to the
	 *            console, <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>message</code> is <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code>. ID is greater than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}
	 */
	private void broadcastMessage(String message, int channel, boolean print) throws NullPointerException, InvalidChannelException {
		if (message == null) {
			throw new NullPointerException("Message cannot be null");
		} else if (channel >= channels.length) {
			throw new InvalidChannelException(channel);
		}
		for (ChatUser user : users.values()) {
			user.sendChatMessage(message, channel);
		}
		if (print == true) {
			LOG.info(message + " [" + channels[channel].getName() + "]");
		}
	}

	/**
	 * Broadcasts the specified message on the specified channel and prints it
	 * to the console.
	 * 
	 * @param message
	 *            the message to send.
	 * @param channel
	 *            the channel to send the message to.
	 * @throws NullPointerException
	 *             if the <code>message</code> is <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code>. ID is greater than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}
	 */
	public void broadcastMessage(String message, int channel) throws NullPointerException, InvalidChannelException {
		this.broadcastMessage(message, channel, true);
	}

	/**
	 * Broadcasts the specified message on every. channel.
	 * 
	 * @param message
	 *            the message to send.
	 * @throws NullPointerException
	 *             if the <code>message</code> is <code>null</code>.
	 */
	public void broadcastMessage(String message) throws NullPointerException {
		for (TextChannel channel : getChannels()) {
			this.broadcastMessage(message + " [Global]", channel.getChannel(), false);
		}
		LOG.info(message + " [Global]");
	}

	/**
	 * Returns whether or not a user with the specified username exists.
	 * 
	 * @param username
	 *            the username.
	 * @return <code>true</code> if a user with the specified username exists,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasUser(String username) {
		if (username != null) {
			for (ChatUser user : users.values()) {
				if (user.getUsername().equalsIgnoreCase(username)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the user with the specified username.
	 * 
	 * @param username
	 *            the username.
	 * @return the user with the specified username, <code>null</code> if they
	 *         do no exist.
	 */
	public ChatUser getUser(String username) {
		if (username != null) {
			for (ChatUser user : users.values()) {
				if (user.getUsername().equals(username)) {
					return user;
				}
			}
		}
		return null;
	}

	/**
	 * Kicks the specified user from the server.
	 * 
	 * @param user
	 *            the user to kick.
	 * @param reason
	 *            the reason the user was kicked, a <code>null</code> value will
	 *            have "Kicked" be used instead.
	 * @throws NullPointerException
	 *             if the <code>user</code> is <code>null</code>.
	 */
	public void kickUser(ChatUser user, String reason) throws NullPointerException {
		if (user == null) {
			throw new NullPointerException("User cannot be null");
		}
		if (users.containsKey(user.getPeer().getAddress())) {
			user.kick(reason == null ? "Kicked" : reason);
			users.remove(user.getPeer().getAddress());
		}
	}

	@Override
	public void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
		ChatUser user = users.get(peer.getAddress());
		if (packet.getId() == ChatPacket.ID_LOGIN_REQUEST && user == null) {
			LoginRequest loginRequest = new LoginRequest(packet);
			loginRequest.decode();

			// Validate user
			String failureReason = null;
			if (loginRequest.username.length() <= 0) {
				failureReason = "Username is too short";
			} else if (this.hasUser(loginRequest.username)) {
				failureReason = "Client with that name is already on this server!";
			}
			if (failureReason != null) {
				LoginFailure loginFailure = new LoginFailure();
				loginFailure.reason = failureReason;
				loginFailure.encode();
				peer.sendMessage(Reliability.UNRELIABLE, loginFailure);
				return;
			}

			// Register user if they are valid
			ChatUser registered = new ChatUser(this, peer, UUID.randomUUID(), loginRequest.username);
			users.put(peer.getAddress(), registered);
			this.broadcastMessage(registered.getUsername() + " has connected");
		} else if (packet.getId() == ChatPacket.ID_CHAT_MESSAGE && user != null) {
			ChatMessage chatMessage = new ChatMessage(packet);
			chatMessage.decode();
			if (chatMessage.message.length() > 0) {
				broadcastMessage("<" + user.getUsername() + "> " + chatMessage.message, channel);
			}
		} else if (packet.getId() == ChatPacket.ID_UPDATE_USERNAME_REQUEST && user != null) {
			UpdateUsernameRequest request = new UpdateUsernameRequest(packet);
			request.decode();
			if (!this.hasUser(request.newUsername)) {
				this.broadcastMessage(user.getUsername() + " has changed their name to " + request.newUsername);
				user.acceptUsernameUpdate(request.newUsername);
			} else {
				user.denyUsernameUpdate();
			}
		}
	}

	@Override
	public void onDisconnect(RakNetServer server, InetSocketAddress address, RakNetClientPeer peer, String reason) {
		ChatUser user = users.remove(peer.getAddress());
		if (user != null) {
			this.broadcastMessage(user.getUsername() + " has disconnected");
		}
	}

	/**
	 * The entry point for the chat server.
	 * 
	 * @param args
	 *            the program arguments. These values are ignored.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 */
	public static void main(String[] args) throws RakNetException {
		// Create and start server
		ChatServer server = new ChatServer(RakNetTest.WHIRVIS_DEVELOPMENT_PORT, 10, "JRakNet chat server",
				"This is a JRakNet chat server made both for example and testing purposes");
		server.start();

		// Register commands
		CommandHandler commandHandler = new CommandHandler(server);
		commandHandler.registerCommand(new StopCommand(server));
		commandHandler.registerCommand(new KickCommand(server));
		commandHandler.registerCommand(new ChannelCommand(server));
		commandHandler.registerCommand(new BroadcastCommand(server));
		commandHandler.registerCommand(HelpCommand.class);

		// Listen for command input
		Scanner commandScanner = new Scanner(System.in);
		while (server.isRunning() && !Thread.currentThread().isInterrupted()) {
			try {
				Thread.sleep(0, 1); // Lower CPU usage
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (commandScanner.hasNextLine()) {
				commandHandler.handleInput(commandScanner.nextLine());
			}
		}
		commandScanner.close();
	}

}
