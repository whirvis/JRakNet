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

import java.net.InetSocketAddress;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.InvalidChannelException;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
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

	private static final Logger LOG = LogManager.getLogger(ChatClient.class);

	/**
	 * The instructions that are displayed when the client is connecting to a
	 * server.
	 */
	public static final String INSTRUCTIONS_CONNECTING = "Connecting...";

	/**
	 * The instructions that are displayed when the client is logging in to a
	 * server.
	 */
	public static final String INSTRUCTIONS_CONNECTED = "Logging in...";

	/**
	 * The instructions that are displayed when the client is logged in to the
	 * server.
	 */
	public static final String INSTRUCTIONS_LOGGED_IN = "Logged in, time to chat!";

	private final ChatFrame frame;
	private final TextChannel[] channels;
	private int currentChannel;
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
		this.channels = new TextChannel[RakNet.CHANNEL_COUNT];
		frame.updateListeners(this);
		frame.setInstructions(null);
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
	 *             {@value RakNet#CHANNEL_COUNT}.
	 */
	public void sendChatMessage(String message, int channel) throws NullPointerException, InvalidChannelException {
		if (message == null) {
			throw new NullPointerException("Message cannot be null");
		} else if (channel >= RakNet.CHANNEL_COUNT) {
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
	 *             {@value RakNet#CHANNEL_COUNT}.
	 * @throws NullPointerException
	 *             if the <code>channel</code> does not exist.
	 */
	public void setChannel(int channel) throws InvalidChannelException, NullPointerException {
		if (channel >= RakNet.CHANNEL_COUNT) {
			throw new InvalidChannelException(channel);
		} else if (channels[channel] == null) {
			throw new NullPointerException("Channel does not exist");
		}
		this.currentChannel = channel;
		frame.updateChannel(channels[channel]);
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
		if (channel < RakNet.CHANNEL_COUNT) {
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
	 * Called when an error has been caught anywhere in the client.
	 * 
	 * @param throwable
	 *            the caught error.
	 */
	public void caughtError(Throwable throwable) {
		frame.displayError(throwable);
		if (this.isConnected()) {
			this.disconnect();
		}
	}

	@Override
	public void connect(InetSocketAddress address) throws NullPointerException, IllegalStateException, RakNetException {
		frame.setInstructions(INSTRUCTIONS_CONNECTING);
		super.connect(address);
		this.username = frame.getUsername();
	}

	@Override
	public void onLogin(RakNetClient client, RakNetServerPeer peer) {
		this.peer = peer;
		LoginRequest request = new LoginRequest();
		request.username = this.username;
		request.encode();
		peer.sendMessage(Reliability.RELIABLE_ORDERED, request);
		frame.setInstructions(INSTRUCTIONS_CONNECTED);
	}

	@Override
	public void onDisconnect(RakNetClient client, InetSocketAddress address, RakNetServerPeer peer, String reason) {
		this.peer = null;
		this.resetChannels();
		frame.setInstructions(null);
		frame.toggleServerInteraction(false);
		JOptionPane.showMessageDialog(frame, "Disconnected from server: " + reason);
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
			frame.setInstructions(INSTRUCTIONS_LOGGED_IN);
			frame.toggleServerInteraction(true);
		} else if (packet.getId() == ChatPacket.ID_LOGIN_FAILURE) {
			LoginFailure failure = new LoginFailure(packet);
			failure.decode();
			frame.displayError("Connection failure", failure.reason);
			frame.toggleServerInteraction(false);
			this.disconnect(failure.reason);
		} else if (packet.getId() == ChatPacket.ID_CHAT_MESSAGE && channels[channel] != null) {
			ChatMessage chat = new ChatMessage(packet);
			chat.decode();
			channels[channel].addChatMessage(chat.message);
			if (currentChannel == channel) {
				frame.updateChannel(channels[channel]);
			}
			LOG.info(chat.message + " [" + channels[channel].getName() + "]");
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
		this.caughtError(throwable);
	}

	/**
	 * The entry point for the chat client.
	 * 
	 * @param args
	 *            the program arguments. These values are ignored.
	 */
	public static void main(String[] args) {
		ChatClient client = null;
		ChatFrame frame = null;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			frame = new ChatFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			client = new ChatClient(frame);
			frame.setVisible(true);
		} catch (Exception e) {
			if (client != null) {
				client.caughtError(e);
			} else {
				frame.displayError(e);
			}
			RakNet.sleep(10000);
			System.exit(0);
		}
	}

}
