/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 Trent "Whirvis" Summerlin
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

import java.util.UUID;

import com.whirvis.jraknet.InvalidChannelException;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.chat.protocol.AddChannel;
import com.whirvis.jraknet.chat.protocol.ChatMessage;
import com.whirvis.jraknet.chat.protocol.ChatPacket;
import com.whirvis.jraknet.chat.protocol.Kick;
import com.whirvis.jraknet.chat.protocol.LoginAccepted;
import com.whirvis.jraknet.chat.protocol.RemoveChannel;
import com.whirvis.jraknet.chat.protocol.RenameChannel;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.protocol.Reliability;

/**
 * Represents a user connect to the {@link ChatServer}.
 *
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v2.0.0
 */
public final class ChatUser {

	private final ChatServer server;
	private final RakNetClientPeer peer;
	private final UUID uuid;
	private String username;

	/**
	 * Creates a chat user.
	 * <p>
	 * As soon as this user is created, a {@link LoginAccepted LOGIN_ACCEPTED}
	 * packet will be sent to it. When a chat user is created, it is assumed
	 * that they are already validated.
	 * 
	 * @param server
	 *            the server that the user belongs to.
	 * @param peer
	 *            the peer.
	 * @param uuid
	 *            the universally unique ID.
	 * @param username
	 *            the username.
	 * @throws NullPointerException
	 *             if the <code>server</code>, <code>peer</code>,
	 *             <code>uuid</code> or <code>username</code> are
	 *             <code>null</code>.
	 */
	public ChatUser(ChatServer server, RakNetClientPeer peer, UUID uuid, String username) throws NullPointerException {
		if (server == null) {
			throw new NullPointerException("Server cannot be null");
		} else if (peer == null) {
			throw new NullPointerException("Peer cannot be null");
		} else if (uuid == null) {
			throw new NullPointerException("UUID cannot be null");
		} else if (username == null) {
			throw new NullPointerException("Username cannot be null");
		}
		this.server = server;
		this.peer = peer;
		this.uuid = uuid;
		this.username = username;

		// Accept login
		LoginAccepted accepted = new LoginAccepted();
		accepted.userId = this.uuid;
		accepted.serverName = server.getName();
		accepted.serverMotd = server.getMotd();
		accepted.channels = server.getChannels();
		accepted.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, accepted);
	}

	/**
	 * Returns the server that the user belongs to.
	 * 
	 * @return the server that the user belongs to.
	 */
	public ChatServer getServer() {
		return this.server;
	}

	/**
	 * Returns the peer associated with the user.
	 * 
	 * @return the peer associated with the user.
	 */
	public RakNetClientPeer getPeer() {
		return this.peer;
	}

	/**
	 * Returns the UUID of the user.
	 * 
	 * @return the UUID of the user.
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Returns the username of the user.
	 * 
	 * @return the username of the user.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Tells the user its new username update request has been accepted.
	 * 
	 * @param username
	 *            the new username of the user.
	 * @throws NullPointerException
	 *             if the <code>username</code> is <code>null</code>.
	 */
	public void acceptUsernameUpdate(String username) throws NullPointerException {
		if (username == null) {
			throw new NullPointerException("Username cannot be null");
		}
		this.username = username;
		peer.sendMessage(Reliability.RELIABLE_ORDERED, ChatPacket.ID_UPDATE_USERNAME_ACCEPTED);
	}

	/**
	 * Tells the user its new username update request has been denied.
	 */
	public void denyUsernameUpdate() {
		peer.sendMessage(Reliability.RELIABLE_ORDERED, ChatPacket.ID_UPDATE_USERNAME_FAILURE);
	}

	/**
	 * Sends a chat message to the user on the specified channel.
	 * 
	 * @param message
	 *            the message to send.
	 * @param channel
	 *            the channel to send the message on.
	 * @throws NullPointerException
	 *             if the <code>message</code> is <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code> is greater than or equal to
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public void sendChatMessage(String message, int channel) throws NullPointerException, InvalidChannelException {
		if (message == null) {
			throw new NullPointerException("Message cannot be null");
		} else if (channel >= RakNet.CHANNEL_COUNT) {
			throw new InvalidChannelException(channel);
		}
		ChatMessage chat = new ChatMessage();
		chat.message = message;
		chat.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, channel, chat);
	}

	/**
	 * Notifies the user of a new channel.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 * @param name
	 *            the name of the channel.
	 * @throws NullPointerException
	 *             if the channel <code>name</code> is <code>null</code>.
	 */
	public void addChannel(int channel, String name) throws NullPointerException {
		if (name == null) {
			throw new NullPointerException("Channel name cannot be null");
		}
		AddChannel addChannel = new AddChannel();
		addChannel.channel = channel;
		addChannel.channelName = name;
		addChannel.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, addChannel);
	}

	/**
	 * Notifies the user of a channel rename.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 * @param name
	 *            the new name of the channel.
	 * @throws NullPointerException
	 *             if the channel <code>name</code> is <code>null</code>.
	 */
	public void renameChannel(int channel, String name) throws NullPointerException {
		if (name == null) {
			throw new NullPointerException("Channel name cannot be null");
		}
		RenameChannel renameChannel = new RenameChannel();
		renameChannel.channel = channel;
		renameChannel.newChannelName = name;
		renameChannel.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, renameChannel);
	}

	/**
	 * Notifies the user of a removed channel.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 */
	public void removeChannel(int channel) {
		RemoveChannel removeChannel = new RemoveChannel();
		removeChannel.channel = channel;
		removeChannel.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, removeChannel);
	}

	/**
	 * Kicks the user.
	 * 
	 * @param reason
	 *            the reason the user was kicked, <code>null</code> value will
	 *            have "Kicked" be used as the reason instead.
	 */
	public void kick(String reason) {
		Kick kick = new Kick();
		kick.reason = reason == null ? "Kicked" : reason;
		kick.encode();
		peer.sendMessage(Reliability.UNRELIABLE, kick);
	}

}
