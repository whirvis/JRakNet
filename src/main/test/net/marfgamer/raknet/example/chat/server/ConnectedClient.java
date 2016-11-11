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
package net.marfgamer.raknet.example.chat.server;

import java.util.UUID;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.protocol.AddChannel;
import net.marfgamer.raknet.example.chat.protocol.ChatMessage;
import net.marfgamer.raknet.example.chat.protocol.Kick;
import net.marfgamer.raknet.example.chat.protocol.LoginAccepted;
import net.marfgamer.raknet.example.chat.protocol.RemoveChannel;
import net.marfgamer.raknet.example.chat.protocol.RenameChannel;
import net.marfgamer.raknet.exception.session.InvalidChannelException;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.session.RakNetSession;

/**
 * Represents a client connect to a <code>ChatServer</code> and is used to easy
 * set status, send chat messages, etc.
 *
 * @author MarfGamer
 */
public class ConnectedClient {

	public static final int USER_STATUS_CLIENT_CONNECTED = 0x00;
	public static final int USER_STATUS_CLIENT_DISCONNECTED = 0x01;

	private final RakNetSession session;
	private final UUID uuid;
	private String username;

	public ConnectedClient(RakNetSession session, UUID uuid, String username) {
		this.session = session;
		this.uuid = uuid;
		this.username = username;
	}

	/**
	 * Returns the client's assigned UUID
	 * 
	 * @return The client's assigned UUID
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Returns the client's username
	 * 
	 * @return The client's username
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Returns the client's session
	 * 
	 * @return The client's session
	 */
	public RakNetSession getSession() {
		return this.session;
	}

	/**
	 * Accepts the client's requested login and sends the data required for the
	 * client to display the server data properly
	 * 
	 * @param name
	 *            The name of the server
	 * @param motd
	 *            The server message of the day
	 * @param channels
	 *            The channels the client can use
	 */
	public void acceptLogin(String name, String motd, ServerChannel[] channels) {
		LoginAccepted accepted = new LoginAccepted();
		accepted.userId = this.uuid;
		accepted.serverName = name;
		accepted.serverMotd = motd;
		accepted.channels = channels;
		accepted.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, accepted);
	}

	/**
	 * Tells the client its new username has been accepted
	 * 
	 * @param username
	 *            The client's new username
	 */
	public void acceptUsernameUpdate(String username) {
		this.username = username;
		session.sendMessage(Reliability.RELIABLE_ORDERED, ChatMessageIdentifier.ID_UPDATE_USERNAME_ACCEPTED);
	}

	/**
	 * Tells the client its new username has been denied
	 */
	public void denyUsernameUpdate() {
		session.sendMessage(Reliability.RELIABLE_ORDERED, ChatMessageIdentifier.ID_UPDATE_USERNAME_FAILURE);
	}

	/**
	 * Sends a chat message to the client on the specified channel
	 * 
	 * @param message
	 *            The message to send
	 * @param channel
	 *            The channel to send the message on
	 */
	public void sendChatMessage(String message, int channel) {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}

		ChatMessage chat = new ChatMessage();
		chat.message = message;
		chat.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, channel, chat);
	}

	/**
	 * Notifies the client of a new channel
	 * 
	 * @param channel
	 *            The ID of the channel
	 * @param name
	 *            The name of the channel
	 */
	public void addChannel(int channel, String name) {
		AddChannel addChannel = new AddChannel();
		addChannel.channel = channel;
		addChannel.channelName = name;
		addChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, addChannel);
	}

	/**
	 * Notifies the client of a channel rename
	 * 
	 * @param channel
	 *            The ID of the channel
	 * @param name
	 *            The new name of the channel
	 */
	public void renameChannel(int channel, String name) {
		RenameChannel renameChannel = new RenameChannel();
		renameChannel.channel = channel;
		renameChannel.newChannelName = name;
		renameChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, renameChannel);
	}

	/**
	 * Notifies the client of a removed channel
	 * 
	 * @param channel
	 *            The ID of the channel
	 */
	public void removeChannel(int channel) {
		RemoveChannel removeChannel = new RemoveChannel();
		removeChannel.channel = channel;
		removeChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, removeChannel);
	}

	/**
	 * Kicks the client
	 * 
	 * @param reason
	 *            The reason the client was kicked
	 */
	public void kick(String reason) {
		Kick kick = new Kick();
		kick.reason = reason;
		kick.encode();
		session.sendMessage(Reliability.UNRELIABLE, kick);
	}

}
