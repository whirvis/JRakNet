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
 * Copyright (c) 2016-2019 Trent Summerlin
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
package com.whirvis.jraknet.chat.client;

import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.whirvis.jraknet.InvalidChannelException;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.RakNetTest;
import com.whirvis.jraknet.chat.TextChannel;
import com.whirvis.jraknet.chat.client.frame.ChatFrame;
import com.whirvis.jraknet.chat.protocol.AddChannel;
import com.whirvis.jraknet.chat.protocol.ChatMessage;
import com.whirvis.jraknet.chat.protocol.ChatPacket;
import com.whirvis.jraknet.chat.protocol.Kick;
import com.whirvis.jraknet.chat.protocol.LoginAccepted;
import com.whirvis.jraknet.chat.protocol.LoginFailure;
import com.whirvis.jraknet.chat.protocol.LoginRequest;
import com.whirvis.jraknet.chat.protocol.RemoveChannel;
import com.whirvis.jraknet.chat.protocol.RenameChannel;
import com.whirvis.jraknet.chat.protocol.UpdateUsernameRequest;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.peer.RakNetServerPeer;
import com.whirvis.jraknet.protocol.Reliability;

/**
 * A simple chat client built using JRakNet.
 * 
 * @author Trent Summerlin
 * @since JRakNet v2.0.0
 */
public class ChatClient extends RakNetClient {

	/**
	 * The instructions that are displayed when the client is not connected to a
	 * server.
	 */
	private static final String CHAT_INSTRUCTIONS_DISCONNECTED = "Please connect to a server...";

	/**
	 * The instructions that are displayed when the client is connecting to a
	 * server.
	 */
	private static final String CHAT_INSTRUCTIONS_CONNECTING = "Connecting to the server...";

	/**
	 * The instructions that are displayed when the client is logging in to a
	 * server.
	 */
	private static final String CHAT_INSTRUCTIONS_CONNECTED = "Connected, logging in...";

	/**
	 * The instructions that are displayed when the client is logged in to the
	 * server.
	 */
	private static final String CHAT_INSTRUCTIONS_LOGGED_IN = "Logged in, press enter to chat!";

	private final ChatFrame frame;
	private final TextChannel[] channels;
	private UUID userId;
	private RakNetServerPeer peer;
	private String username;
	private String newUsername;

	/**
	 * Creates a chat client.
	 * 
	 * @param frame
	 *            the chat frame that client will display to.
	 * @throws NullPointerException
	 *             if the chat <code>frame</code> is <code>null</code>.
	 */
	private ChatClient(ChatFrame frame) throws NullPointerException {
		if (frame == null) {
			throw new NullPointerException("Chat frame cannot be null");
		}
		this.frame = frame;
		this.channels = new TextChannel[RakNet.MAX_CHANNELS];
		frame.updateListeners(this);
		frame.setInstructions(CHAT_INSTRUCTIONS_DISCONNECTED);
		this.addSelfListener();
	}

	/**
	 * Returns the current user ID.
	 * 
	 * @return the current user ID.
	 */
	public UUID getUserId() {
		return this.userId;
	}

	/**
	 * Returns the current username.
	 * 
	 * @return the current username.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sends a chat message to the specified channel.
	 * 
	 * @param message
	 *            the message to send.
	 * @param channel
	 *            the channel to send the message to.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code> ID is greater than or equal to
	 *             {@value RakNet#MAX_CHANNELS}.
	 */
	public void sendChatMessage(String message, int channel) throws NullPointerException, InvalidChannelException {
		if (message == null) {
			throw new NullPointerException("Message cannot be null");
		} else if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException(channel);
		}
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.message = message;
		chatMessage.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, channel, chatMessage);
	}

	/**
	 * Requests to change the username.
	 * 
	 * @param username
	 *            the new username.
	 * @throws IllegalStateException
	 *             if the client is not connected to a server.
	 * @throws IllegalArgumentException
	 *             if the <code>username</code> length is less than or equal
	 *             <code>0</code>.
	 */
	public void updateUsername(String username) throws IllegalStateException, IllegalArgumentException {
		if (peer == null) {
			throw new IllegalStateException("Client is not connected to a server");
		} else if (username.length() <= 0) {
			throw new IllegalArgumentException("Username is too short");
		}
		UpdateUsernameRequest updateUsernameRequest = new UpdateUsernameRequest();
		updateUsernameRequest.newUsername = this.newUsername = username;
		updateUsernameRequest.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, updateUsernameRequest);
	}

	/**
	 * Sets the currently displayed channel.
	 * 
	 * @param channel
	 *            the new channel to display.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code> ID is greater than or equal to
	 *             {@value RakNet#MAX_CHANNELS}.
	 * @throws NullPointerException
	 *             if the <code>channel</code> does not exist.
	 */
	public void setChannel(int channel) throws InvalidChannelException, NullPointerException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException(channel);
		} else if (channels[channel] == null) {
			throw new NullPointerException("Channel does not exist");
		}
		frame.setCurrentChannel(channels[channel]);
	}

	/**
	 * Adds the specified channel to the client.
	 * 
	 * @param channel
	 *            the channel.
	 * @throws NullPointerException
	 *             if the <code>channel</code> is <code>null</code>.
	 */
	public void addChannel(TextChannel channel) throws NullPointerException {
		if (channel == null) {
			throw new NullPointerException("Channel cannot be null");
		}
		this.channels[channel.getChannel()] = channel;
		frame.setChannels(channels);
	}

	/**
	 * Removes the channel from the client.
	 * 
	 * @param channel
	 *            the ID of the channel to remove.
	 */
	public void removeChannel(int channel) {
		if (channel < RakNet.MAX_CHANNELS) {
			this.channels[channel] = null;
			frame.setChannels(channels);
		}
	}

	/**
	 * Removes every channel from the client and resets the frame data.
	 */
	public void resetChannels() {
		for (int i = 0; i < channels.length; i++) {
			this.channels[i] = null;
		}
		frame.setChannels(channels);
	}

	/**
	 * Joins the chat server with the specified address.
	 * 
	 * @param address
	 *            the address of the server.
	 */
	public void join(String address) {
		try {
			if (this.isConnected()) {
				throw new IllegalStateException("Already connected to a server");
			}
			this.username = frame.getUsername();
			this.connect(RakNet.parseAddress(address, RakNetTest.WHIRVIS_DEVELOPMENT_PORT));
			frame.setInstructions(CHAT_INSTRUCTIONS_CONNECTING);
		} catch (Exception e) {
			frame.setInstructions(CHAT_INSTRUCTIONS_DISCONNECTED);
			frame.displayError(e);
		}
	}

	@Override
	public void onLogin(RakNetClient client, RakNetServerPeer peer) {
		this.peer = peer;
		LoginRequest request = new LoginRequest();
		request.username = this.username;
		request.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, request);
		frame.setInstructions(CHAT_INSTRUCTIONS_CONNECTED);
	}

	@Override
	public void onDisconnect(RakNetClient client, RakNetServerPeer peer, String reason) {
		this.peer = null;
		frame.setInstructions(CHAT_INSTRUCTIONS_DISCONNECTED);
		frame.toggleServerInteraction(false);
		this.resetChannels();
	}

	@Override
	public void handleMessage(RakNetClient client, RakNetServerPeer peer, RakNetPacket packet, int channel) {
		if (packet.getId() == ChatPacket.ID_LOGIN_ACCEPTED) {
			LoginAccepted accepted = new LoginAccepted(packet);
			accepted.decode();
			frame.setServerName(accepted.serverName);
			frame.setServerMotd(accepted.serverMotd);
			for (TextChannel serverChannel : accepted.channels) {
				this.channels[serverChannel.getChannel()] = serverChannel;
				frame.setChannels(channels);
			}
			this.userId = accepted.userId;
			frame.setInstructions(CHAT_INSTRUCTIONS_LOGGED_IN);
			frame.toggleServerInteraction(true);
		} else if (packet.getId() == ChatPacket.ID_LOGIN_FAILURE) {
			LoginFailure failure = new LoginFailure(packet);
			failure.decode();
			frame.displayError("Connection failure", failure.reason);
			frame.toggleServerInteraction(false);
			this.disconnect(failure.reason);
		} else if (packet.getId() == ChatPacket.ID_CHAT_MESSAGE) {
			ChatMessage chat = new ChatMessage(packet);
			chat.decode();
			if (channels[channel] != null) {
				channels[channel].addChatMessage(chat.message);
			}
		} else if (packet.getId() == ChatPacket.ID_UPDATE_USERNAME_ACCEPTED) {
			if (newUsername != null) {
				this.username = newUsername;
				this.newUsername = null;
				frame.displayMessage("Updated username to " + username);
			}
		} else if (packet.getId() == ChatPacket.ID_UPDATE_USERNAME_FAILURE) {
			if (newUsername != null) {
				this.newUsername = null;
				frame.displayError("Message from client", "Failed to update username");
			}
		} else if (packet.getId() == ChatPacket.ID_ADD_CHANNEL) {
			AddChannel addChannel = new AddChannel(packet);
			addChannel.decode();
			this.addChannel(new TextChannel(addChannel.channel, addChannel.channelName));
		} else if (packet.getId() == ChatPacket.ID_RENAME_CHANNEL) {
			RenameChannel renameChannel = new RenameChannel(packet);
			renameChannel.decode();
			if (channels[renameChannel.channel] != null) {
				channels[renameChannel.channel].setName(renameChannel.newChannelName);
			}
		} else if (packet.getId() == ChatPacket.ID_REMOVE_CHANNEL) {
			RemoveChannel removeChannel = new RemoveChannel(packet);
			removeChannel.decode();
			this.removeChannel(removeChannel.channel);
		} else if (packet.getId() == ChatPacket.ID_KICK) {
			Kick kick = new Kick(packet);
			kick.decode();
			frame.displayError("Kicked from server", kick.reason);
			this.disconnect(kick.reason);
		}
	}

	@Override
	public void onPeerException(RakNetClient client, RakNetServerPeer peer, Throwable throwable) {
		frame.displayError(throwable);
		this.disconnect(throwable);
	}

	/**
	 * The entry point for the chat client.
	 * 
	 * @param args
	 *            the program arguments. These values are ignored.
	 */
	public static void main(String[] args) {
		ChatClient client = null;
		ChatFrame frame = new ChatFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			client = new ChatClient(frame);
			frame.setVisible(true);
		} catch (Exception e) {
			frame.displayError(e);
			if (client != null) {
				client.disconnect(e);
			}
		}
	}

}
