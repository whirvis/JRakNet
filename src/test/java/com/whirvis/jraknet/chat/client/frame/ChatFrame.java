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
package com.whirvis.jraknet.chat.client.frame;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.chat.TextChannel;
import com.whirvis.jraknet.chat.client.ChatClient;

/**
 * The frame used to visualize the {@link ChatClient}.
 *
 * @author Trent Summerlin
 * @since JRakNet v2.0.0
 */
public final class ChatFrame extends JFrame {

	/**
	 * The render used in place of the default so that the text channels list
	 * uses the actual name of the channel, rather than defaulting to it's
	 * {@link TextChannel#toString()} method.
	 * 
	 * @author Trent Summerlin
	 * @since JRakNet v2.11.0
	 */
	private static class TextChannelRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = -3378705654945270562L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value != null) {
				this.setText(((TextChannel) value).getName());
			}
			return this;
		}

	}

	private static final long serialVersionUID = -5459540662852804663L;

	/**
	 * The width of the chat frame.
	 */
	private static final int FRAME_WIDTH = 500;

	/**
	 * The height of the chat frame.
	 */
	private static final int FRAME_HEIGHT = 330;

	/**
	 * The default server address.
	 */
	private static final String DEFAULT_SERVER_ADDRESS = "localhost";

	/**
	 * The default server name.
	 */
	private static final String DEFAULT_SERVER_NAME = "Server name";

	/**
	 * The default server message of the day.
	 */
	private static final String DEFAULT_SERVER_MOTD = "Server MOTD";

	/**
	 * The default client username.
	 */
	private static final String DEFAULT_CLIENT_USERNAME = "Username";

	protected final JTextField txtServerName;
	protected final JTextField txtServerMotd;
	protected final JTextField txtServerAddress;
	protected final JTextField txtClientUsername;
	protected final JComboBox<TextChannel> cmbTextChannels;
	protected final JScrollPane textChannelPane;
	protected JTextPane txtPaneTextChannel;
	protected boolean connected;
	protected final JButton btnConnectServer;
	protected final JButton btnUpdateUsername;
	protected final JTextPane txtpnInstructions;
	protected final JTextField txtChatBox;

	/**
	 * Creats a chat client frame.
	 */
	public ChatFrame() {
		// Frame and content settings
		this.setResizable(false);
		this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		this.setTitle("JRakNet chat client");
		this.getContentPane().setLayout(null);

		// Current server name
		this.txtServerName = new JTextField(DEFAULT_SERVER_NAME);
		txtServerName.setBounds(10, 40, 205, 20);
		txtServerName.setEditable(false);
		txtServerName.setColumns(10);
		this.getContentPane().add(txtServerName);

		// Server message of the day
		this.txtServerMotd = new JTextField(DEFAULT_SERVER_MOTD);
		txtServerMotd.setBounds(10, 75, 204, 20);
		txtServerMotd.setEditable(false);
		txtServerMotd.setColumns(10);
		this.getContentPane().add(txtServerMotd);

		// Current server address
		this.txtServerAddress = new JTextField();
		txtServerAddress.setText(DEFAULT_SERVER_ADDRESS);
		txtServerAddress.setToolTipText("The address of the server to connect to");
		txtServerAddress.setBounds(359, 10, 125, 20);
		txtServerAddress.setColumns(10);
		this.getContentPane().add(txtServerAddress);

		// Current username
		this.txtClientUsername = new JTextField();
		txtClientUsername.setText(DEFAULT_CLIENT_USERNAME);
		txtClientUsername.setToolTipText("The username this client is using");
		txtClientUsername.setBounds(360, 40, 125, 20);
		txtClientUsername.setColumns(10);
		this.getContentPane().add(txtClientUsername);

		// Box to list channels
		this.cmbTextChannels = new JComboBox<TextChannel>();
		cmbTextChannels.setBounds(224, 72, 260, 20);
		cmbTextChannels.setRenderer(new TextChannelRenderer());
		cmbTextChannels.setEnabled(false);
		this.getContentPane().add(cmbTextChannels);

		// The container for the channel box
		this.textChannelPane = new JScrollPane();
		textChannelPane.setBounds(10, 102, 474, 150);
		textChannelPane.setAutoscrolls(true);
		this.getContentPane().add(textChannelPane);

		// Button to trigger connection request
		this.btnConnectServer = new JButton("Connect to server");
		btnConnectServer.setBounds(224, 10, 125, 21);
		btnConnectServer.setEnabled(true);
		this.getContentPane().add(btnConnectServer);

		// Button to trigger name update request
		this.btnUpdateUsername = new JButton("Update");
		btnUpdateUsername.setBounds(224, 40, 125, 20);
		btnUpdateUsername.setEnabled(false);
		this.getContentPane().add(btnUpdateUsername);

		// Current instructions
		this.txtpnInstructions = new JTextPane();
		txtpnInstructions.setEditable(false);
		txtpnInstructions.setText("Waiting for data...");
		txtpnInstructions.setBackground(UIManager.getColor("Button.background"));
		txtpnInstructions.setBounds(10, 10, 205, 20);
		this.getContentPane().add(txtpnInstructions);

		// The text on the current selected channel
		this.txtPaneTextChannel = new JTextPane();
		txtPaneTextChannel.setEditable(false);
		txtPaneTextChannel.setAutoscrolls(true);
		textChannelPane.setViewportView(txtPaneTextChannel);

		// Text field to chat
		this.txtChatBox = new JTextField();
		txtChatBox.setBounds(10, 260, 474, 20);
		txtChatBox.setColumns(10);
		txtChatBox.setEnabled(false);
		this.getContentPane().add(txtChatBox);
	}

	/**
	 * Returns the text in the username box.
	 * 
	 * @return the text in the username box.
	 */
	public String getUsername() {
		return txtClientUsername.getText();
	}

	/**
	 * Sets the displayed server name.
	 * 
	 * @param serverName
	 *            the name to display.
	 * @throws NullPointerException
	 *             if the <code>serverName</code> is <code>null</code>.
	 */
	public void setServerName(String serverName) throws NullPointerException {
		if (serverName == null) {
			throw new NullPointerException("Server name cannot be null");
		}
		txtServerName.setText(serverName);
	}

	/**
	 * Sets the displayed server message of the day.
	 * 
	 * @param serverMotd
	 *            the message of the day to display.
	 * @throws NullPointerException
	 *             if the <code>serverMotd</code> is <code>null</code>.
	 */
	public void setServerMotd(String serverMotd) throws NullPointerException {
		if (serverMotd == null) {
			throw new NullPointerException("Server message of the day cannot be null");
		}
		txtServerMotd.setText(serverMotd);
	}

	/**
	 * Sets the current displayed channels.
	 * 
	 * @param channels
	 *            the channels to display.
	 * @throws NullPointerException
	 *             if the <code>channels</code> are <code>null</code>.
	 */
	public void setChannels(TextChannel[] channels) throws NullPointerException {
		if (channels == null) {
			throw new NullPointerException("Channels cannot be null");
		}
		ArrayList<TextChannel> cleansed = new ArrayList<TextChannel>();
		for (TextChannel channel : channels) {
			if (channel != null) {
				cleansed.add(channel);
			}
		}
		cmbTextChannels
				.setModel(new DefaultComboBoxModel<TextChannel>(cleansed.toArray(new TextChannel[cleansed.size()])));
		if (cmbTextChannels.getModel().getSize() > 0) {
			cmbTextChannels.setSelectedIndex(0);
		}
	}

	/**
	 * Sets the current channel.
	 * 
	 * @param channel
	 *            the channel to display.
	 * @throws NullPointerException
	 *             if the <code>channel</code> is <code>null</code>.
	 */
	public void updateChannel(TextChannel channel) throws NullPointerException {
		if (channel == null) {
			throw new NullPointerException("Channel cannot be null");
		}
		txtPaneTextChannel.setText(channel.getText());
	}

	/**
	 * Sets the displayed instructions.
	 * 
	 * @param instructions
	 *            the instructions to display.
	 * @throws NullPointerException
	 *             if the <code>instructions</code> are <code>null</code>.
	 */
	public void setInstructions(String instructions) throws NullPointerException {
		txtpnInstructions.setText(instructions);
	}

	/**
	 * Enables/disables server interaction.
	 * 
	 * @param connected
	 *            <code>true</code> to enable server interaction,
	 *            <code>false</code> to disable it.
	 */
	public void toggleServerInteraction(boolean connected) {
		if (this.connected != connected) {
			this.connected = connected;
			txtServerName.setEnabled(connected);
			txtServerMotd.setEnabled(connected);
			txtServerAddress.setEditable(!connected);
			btnConnectServer.setText(!connected ? "Connect" : "Disconnect");
			btnUpdateUsername.setEnabled(connected);
			cmbTextChannels.setEnabled(connected);
			txtChatBox.setEnabled(connected);
			if (connected == false) {
				txtServerName.setText(DEFAULT_SERVER_NAME);
				txtServerMotd.setText(DEFAULT_SERVER_MOTD);
				txtServerAddress.setText(DEFAULT_SERVER_ADDRESS);
				txtClientUsername.setText(DEFAULT_CLIENT_USERNAME);
				cmbTextChannels.setModel(new DefaultComboBoxModel<TextChannel>(new TextChannel[0]));
				txtPaneTextChannel.setText("");
				txtChatBox.setText("");
			}
		}
	}

	/**
	 * Updates the listeners required for the frame to function properly based
	 * on the specified client.
	 * 
	 * @param client
	 *            the client to assign the listeners to.
	 * @throws NullPointerException
	 *             if the <code>client</code> is <code>null</code>.
	 */
	public void updateListeners(ChatClient client) throws NullPointerException {
		if (client == null) {
			throw new NullPointerException("Client cannot be null");
		}
		btnConnectServer.addActionListener(new ConnectServerListener(this, client));
		btnUpdateUsername.addActionListener(new UpdateUsernameBoxListener(this, client));
		cmbTextChannels.addActionListener(new ComboBoxTextChannelListener(this, client));
		txtChatBox.addKeyListener(new ChatBoxKeyListener(this, client));
	}

	/**
	 * Displays a message.
	 * 
	 * @param message
	 *            the message to display.
	 * @throws NullPointerException
	 *             if the <code>message</code> is <code>null</code>.
	 */
	public void displayMessage(String message) throws NullPointerException {
		if (message == null) {
			throw new NullPointerException("Message cannot be null");
		}
		JOptionPane.showMessageDialog(this, message);
	}

	/**
	 * Displays an error with the title and message.
	 * 
	 * @param title
	 *            the title of the error.
	 * @param error
	 *            the error message.
	 * @throws NullPointerException
	 *             if the <code>title</code> or <code>error</code> message are
	 *             <code>null</code>.
	 */
	public void displayError(String title, String error) throws NullPointerException {
		if (title == null) {
			throw new NullPointerException("Title cannot be null");
		} else if (error == null) {
			throw new NullPointerException("Error message cannot be null");
		}
		JOptionPane.showMessageDialog(this, error, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Displays an error using the exception.
	 * 
	 * @param throwable
	 *            the caught exception.
	 * @throws NullPointerException
	 *             if the <code>throwable</code> is <code>null</code>.
	 */
	public void displayError(Throwable throwable) throws NullPointerException {
		if (throwable == null) {
			throw new NullPointerException("Throwable cannot be null");
		}
		this.displayError("Error: " + throwable.getClass().getName(), RakNet.getStackTrace(throwable));
	}

}
