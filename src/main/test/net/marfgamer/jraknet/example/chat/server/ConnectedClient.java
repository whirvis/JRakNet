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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet.example.chat.server;

import java.util.UUID;

import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.jraknet.example.chat.ServerChannel;
import net.marfgamer.jraknet.example.chat.protocol.AddChannel;
import net.marfgamer.jraknet.example.chat.protocol.ChatMessage;
import net.marfgamer.jraknet.example.chat.protocol.Kick;
import net.marfgamer.jraknet.example.chat.protocol.LoginAccepted;
import net.marfgamer.jraknet.example.chat.protocol.RemoveChannel;
import net.marfgamer.jraknet.example.chat.protocol.RenameChannel;
import net.marfgamer.jraknet.protocol.Reliability;
import net.marfgamer.jraknet.session.InvalidChannelException;
import net.marfgamer.jraknet.session.RakNetClientSession;

/**
 * Represents a client connect to a <code>ChatServer</code> and is used to easy
 * set status, send chat messages, etc.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class ConnectedClient {

	public static final int USER_STATUS_CLIENT_CONNECTED = 0x00;
	public static final int USER_STATUS_CLIENT_DISCONNECTED = 0x01;

	private final RakNetClientSession session;
	private final UUID uuid;
	private String username;

	/**
	 * Constructs a <code>ConnectedClient</code> with the specified
	 * <code>RakNetClientSession</code>, <code>UUID</code> and username.
	 * 
	 * @param session
	 *            the <code>RakNetSession</code>.
	 * @param uuid
	 *            the <code>UUID</code>.
	 * @param username
	 *            the username.
	 */
	public ConnectedClient(RakNetClientSession session, UUID uuid, String username) {
		this.session = session;
		this.uuid = uuid;
		this.username = username;
	}

	/**
	 * @return the client's assigned UUID.
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * @return the client's username.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * @return the client's session.
	 */
	public RakNetClientSession getSession() {
		return this.session;
	}

	/**
	 * Accepts the client's requested login and sends the data required for the
	 * client to display the server data properly.
	 * 
	 * @param name
	 *            the name of the server.
	 * @param motd
	 *            the server message of the day.
	 * @param channels
	 *            the channels the client can use.
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
	 * Tells the client its new username has been accepted.
	 * 
	 * @param username
	 *            the client's new username.
	 */
	public void acceptUsernameUpdate(String username) {
		this.username = username;
		session.sendMessage(Reliability.RELIABLE_ORDERED, ChatMessageIdentifier.ID_UPDATE_USERNAME_ACCEPTED);
	}

	/**
	 * Tells the client its new username has been denied.
	 */
	public void denyUsernameUpdate() {
		session.sendMessage(Reliability.RELIABLE_ORDERED, ChatMessageIdentifier.ID_UPDATE_USERNAME_FAILURE);
	}

	/**
	 * Sends a chat message to the client on the specified channel.
	 * 
	 * @param message
	 *            the message to send.
	 * @param channel
	 *            the channel to send the message on.
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
	 * Notifies the client of a new channel.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 * @param name
	 *            the name of the channel.
	 */
	public void addChannel(int channel, String name) {
		AddChannel addChannel = new AddChannel();
		addChannel.channel = channel;
		addChannel.channelName = name;
		addChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, addChannel);
	}

	/**
	 * Notifies the client of a channel rename.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 * @param name
	 *            the new name of the channel.
	 */
	public void renameChannel(int channel, String name) {
		RenameChannel renameChannel = new RenameChannel();
		renameChannel.channel = channel;
		renameChannel.newChannelName = name;
		renameChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, renameChannel);
	}

	/**
	 * Notifies the client of a removed channel.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 */
	public void removeChannel(int channel) {
		RemoveChannel removeChannel = new RemoveChannel();
		removeChannel.channel = channel;
		removeChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, removeChannel);
	}

	/**
	 * Kicks the client.
	 * 
	 * @param reason
	 *            the reason the client was kicked.
	 */
	public void kick(String reason) {
		Kick kick = new Kick();
		kick.reason = reason;
		kick.encode();
		session.sendMessage(Reliability.UNRELIABLE, kick);
	}

}
