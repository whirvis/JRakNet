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
 * Copyright (c) 2016, 2017 Trent "MarfGamer" Summerlin
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
package net.marfgamer.jraknet.example.chat.client;

import java.net.InetSocketAddress;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.UtilityTest;
import net.marfgamer.jraknet.client.RakNetClient;
import net.marfgamer.jraknet.client.RakNetClientListener;
import net.marfgamer.jraknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.jraknet.example.chat.ServerChannel;
import net.marfgamer.jraknet.example.chat.client.frame.ChatFrame;
import net.marfgamer.jraknet.example.chat.protocol.AddChannel;
import net.marfgamer.jraknet.example.chat.protocol.ChatMessage;
import net.marfgamer.jraknet.example.chat.protocol.Kick;
import net.marfgamer.jraknet.example.chat.protocol.LoginAccepted;
import net.marfgamer.jraknet.example.chat.protocol.LoginFailure;
import net.marfgamer.jraknet.example.chat.protocol.LoginRequest;
import net.marfgamer.jraknet.example.chat.protocol.RemoveChannel;
import net.marfgamer.jraknet.example.chat.protocol.RenameChannel;
import net.marfgamer.jraknet.example.chat.protocol.UpdateUsername;
import net.marfgamer.jraknet.protocol.Reliability;
import net.marfgamer.jraknet.session.InvalidChannelException;
import net.marfgamer.jraknet.session.RakNetServerSession;
import net.marfgamer.jraknet.util.RakNetUtils;

/**
 * A simple chat client built using JRakNet and a <code>JFrame</code>.
 *
 * @author Trent "MarfGamer" Summerlin
 */
public class ChatClient implements RakNetClientListener {

	private static final String CHAT_INSTRUCTIONS_DISCONNECTED = "Please connect to a server...";
	private static final String CHAT_INSTRUCTIONS_CONNECTING = "Connecting to the server...";
	private static final String CHAT_INSTRUCTIONS_CONNECTED = "Connected, logging in...";
	private static final String CHAT_INSTRUCTIONS_LOGGED_IN = "Logged in, press enter to chat!";

	// Session data
	private int channel;
	private UUID userId;
	private RakNetServerSession session;
	private final ServerChannel[] channels;

	// Client data
	private String username;
	private String newUsername;
	private final ChatFrame frame;
	private final RakNetClient client;

	public ChatClient(ChatFrame frame) {
		this.frame = frame;
		frame.updateListeners(this);
		frame.setInstructions(CHAT_INSTRUCTIONS_DISCONNECTED);

		this.client = new RakNetClient().setListener(this);
		this.channels = new ServerChannel[RakNet.MAX_CHANNELS];
	}

	/**
	 * @return the current user ID.
	 */
	public UUID getUserId() {
		return this.userId;
	}

	/**
	 * @return the user's current username.
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
	 *            the channel to send it to.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 */
	public void sendChatMessage(String message, int channel) throws InvalidChannelException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}

		ChatMessage messagePacket = new ChatMessage();
		messagePacket.message = message;
		messagePacket.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED_WITH_ACK_RECEIPT, channel, messagePacket);
	}

	/**
	 * Requests to change the username mid-session.
	 * 
	 * @param newUsername
	 *            the new username.
	 * @throws ChatException
	 *             if an error occurs during the request.
	 */
	public void setUsernameRequest(String newUsername) throws ChatException {
		// Make sure we can change the username
		if (newUsername.equals(newUsername)) {
			return;
		} else if (newUsername.length() <= 0) {
			throw new ChatException("Name is too short!");
		} else if (session == null) {
			throw new ChatException("Not connected to a server!");
		}
		this.newUsername = newUsername;

		// Send the request
		UpdateUsername request = new UpdateUsername();
		request.newUsername = newUsername;
		request.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, request);
	}

	/**
	 * Sets the current displayed channel.
	 * 
	 * @param channel
	 *            the new channel to display.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 * @throws ChatException
	 *             if an error occurs when setting the channel.
	 */
	public void setChannel(int channel) throws InvalidChannelException, ChatException {
		// Make sure the channel is valid
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		} else if (channels[channel] == null) {
			throw new ChatException("This channel has not been set yet!");
		}

		// Set the current channel and update the text
		this.channel = channel;
		frame.setCurrentChannel(channels[channel]);
		this.updateChannelText();
	}

	/**
	 * Updates the channel text for the frame.
	 */
	public void updateChannelText() {
		frame.setCurrentChannel(channels[channel]);
	}

	/**
	 * Adds a channel to the client.
	 * 
	 * @param channel
	 *            the new channel.
	 */
	public void addChannel(ServerChannel channel) {
		this.channels[channel.getChannel()] = channel;
		frame.setChannels(channels);
	}

	/**
	 * Removes a channel from the client.
	 * 
	 * @param channel
	 *            the ID of the channel to remove.
	 * @throws InvalidChannelException
	 *             if the channel exceeds the limit.
	 */
	public void removeChannel(int channel) throws InvalidChannelException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}
		this.channels[channel] = null;
		frame.setChannels(channels);
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
	 * Connects to a chat server at the specified address.
	 * 
	 * @param address
	 *            the address of the server.
	 */
	public void connect(String address) {
		try {
			InetSocketAddress socketAddress = RakNetUtils.parseAddress(address, UtilityTest.MARFGAMER_DEVELOPMENT_PORT);
			this.username = frame.getUsername();
			client.connectThreaded(socketAddress);

			frame.setInstructions(CHAT_INSTRUCTIONS_CONNECTING);
		} catch (Exception e) {
			frame.setInstructions(CHAT_INSTRUCTIONS_DISCONNECTED);
			frame.displayError(e);
		}
	}

	/**
	 * Disconnects from the server with the specified reason.
	 * 
	 * @param reason
	 *            the reason the client disconnected from the server.
	 */
	public void disconnect(String reason) {
		client.disconnect(reason);
		this.resetChannels();
	}

	@Override
	public void onConnect(RakNetServerSession session) {
		// Set session
		this.session = session;

		// Send login request
		LoginRequest request = new LoginRequest();
		request.username = this.username;
		request.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, request);

		// Set on screen status
		frame.setInstructions(CHAT_INSTRUCTIONS_CONNECTED);
	}

	@Override
	public void handleMessage(RakNetServerSession session, RakNetPacket packet, int channel) {
		short packetId = packet.getId();

		if (packetId == ChatMessageIdentifier.ID_LOGIN_ACCEPTED) {
			LoginAccepted accepted = new LoginAccepted(packet);
			accepted.decode();

			// Set client and server on screen data
			frame.setServerName(accepted.serverName);
			frame.setServerMotd(accepted.serverMotd);
			for (ServerChannel serverChannel : accepted.channels) {
				this.channels[serverChannel.getChannel()] = serverChannel;
				frame.setChannels(channels);
			}
			this.userId = accepted.userId;

			// Set on screen instructions and enable server interaction
			frame.setInstructions(CHAT_INSTRUCTIONS_LOGGED_IN);
			frame.toggleServerInteraction(true);
		} else if (packetId == ChatMessageIdentifier.ID_LOGIN_FAILURE) {
			LoginFailure failure = new LoginFailure(packet);
			failure.decode();

			// Show the error and disable server interaction
			frame.displayError("Connection failure", failure.reason);
			frame.toggleServerInteraction(false);
			this.disconnect(failure.reason);
		} else if (packetId == ChatMessageIdentifier.ID_CHAT_MESSAGE) {
			ChatMessage chat = new ChatMessage(packet);
			chat.decode();

			// Update channel text if the channel is valid
			if (channels[channel] != null) {
				if (channel >= RakNet.MAX_CHANNELS || channel < 0) {
					frame.displayError("Invalid channel", "Channel " + channel + " is an invalid channel");
				}
				channels[channel].addChatMessage(chat.message);
				this.updateChannelText();
			}
		} else if (packetId == ChatMessageIdentifier.ID_UPDATE_USERNAME_ACCEPTED) {
			// Are we even trying to set a new username?
			if (this.newUsername != null) {
				// New name successfully set, notify the client!
				this.username = this.newUsername;
				this.newUsername = null;
				frame.displayMessage("Updated username to " + this.username);
			}
		} else if (packetId == ChatMessageIdentifier.ID_UPDATE_USERNAME_FAILURE) {
			// Are we even trying to set a new username?
			if (this.newUsername != null) {
				// New name was not successfully set, notify the client!
				this.newUsername = null;
				frame.displayError("Message from client", "Failed to update username");
			}
		} else if (packetId == ChatMessageIdentifier.ID_ADD_CHANNEL) {
			// Add the channel
			AddChannel addChannel = new AddChannel(packet);
			addChannel.decode();
			this.addChannel(new ServerChannel(addChannel.channel, addChannel.channelName));
		} else if (packetId == ChatMessageIdentifier.ID_RENAME_CHANNEL) {
			// Does this channel exist?
			RenameChannel renameChannel = new RenameChannel(packet);
			renameChannel.decode();
			if (channels[renameChannel.channel] != null) {
				channels[renameChannel.channel].setName(renameChannel.newChannelName);
			}
		} else if (packetId == ChatMessageIdentifier.ID_REMOVE_CHANNEL) {
			// Remove the channel
			RemoveChannel removeChannel = new RemoveChannel(packet);
			removeChannel.decode();
			this.removeChannel(removeChannel.channel);
		} else if (packetId == ChatMessageIdentifier.ID_KICK) {
			// We were kicked from the server, disconnect
			Kick kick = new Kick(packet);
			kick.decode();
			frame.displayError("Kicked from server", kick.reason);
			this.disconnect(kick.reason);
		}
	}

	@Override
	public void onDisconnect(RakNetServerSession session, String reason) {
		// Set on screen instructions and disable server interactions
		this.session = null;
		frame.setInstructions(CHAT_INSTRUCTIONS_DISCONNECTED);
		frame.toggleServerInteraction(false);
	}

	@Override
	public void onThreadException(Throwable throwable) {
		// Display error and disconnect from server
		frame.displayError(throwable);
		this.disconnect(throwable.getClass().getName() + ": " + throwable.getMessage());
	}

	public static void main(String[] args) {
		try {
			// Create frame
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			ChatFrame frame = new ChatFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			// Enable logging
			RakNet.enableLogging();

			// Create client
			@SuppressWarnings("unused")
			ChatClient client = new ChatClient(frame);

			// Client is ready, show the frame!
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
