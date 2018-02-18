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
 * Copyright (c) 2016-2018 Trent Summerlin
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
package com.whirvis.jraknet.example.chat.client.frame;

import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import com.whirvis.jraknet.example.chat.ServerChannel;
import com.whirvis.jraknet.example.chat.client.ChatClient;

/**
 * The frame used by the <code>ChatClient</code> to visualize it's data.
 *
 * @author Trent Summerlin
 */
public class ChatFrame extends JFrame {

	private static final long serialVersionUID = -5459540662852804663L;

	private static final int FRAME_WIDTH = 500;
	private static final int FRAME_HEIGHT = 315;

	private static final String DEFAULT_SERVER_ADDRESS = "localhost";
	private static final String DEFAULT_SERVER_NAME = "Server name";
	private static final String DEFAULT_SERVER_MOTD = "Server MOTD";
	private static final String DEFAULT_CLIENT_USERNAME = "Username";
	private static final String DEFAULT_CLIENT_INSTRUCTIONS = "Waiting for data...";
	private static final String CONNECT_BUTTON_TEXT = "Connect to server";
	private static final String DISCONNECT_BUTTON_TEXT = "Disconnect";
	private static final String UPDATE_USERNAME_BUTTON_TEXT = "Update username";

	// Server, client and channel data
	protected final JTextField txtServerName;
	protected final JTextField txtServerMotd;
	protected final JTextField txtServerAddress;
	protected final JTextField txtClientUsername;
	protected final JComboBox<ServerChannel> cmbServerChannels;
	protected final JScrollPane serverChannelPane;
	protected JTextPane txtPaneServerChannel;
	protected boolean connected;

	// Basic info, instructions, and commands
	protected final JButton btnConnectServer;
	protected final JButton btnUpdateUsername;
	protected final JTextPane txtpnInstructions;
	protected final JTextField txtChatBox;

	public ChatFrame() {
		// Frame settings
		setResizable(false);
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setTitle("JRakNet Client Example");

		// Content settings
		getContentPane().setLayout(null);

		// Current server name
		this.txtServerName = new JTextField(DEFAULT_SERVER_NAME);
		txtServerName.setBounds(10, 40, 205, 20);
		txtServerName.setEditable(false);
		getContentPane().add(txtServerName);
		txtServerName.setColumns(10);

		// Server message of the day
		this.txtServerMotd = new JTextField(DEFAULT_SERVER_MOTD);
		txtServerMotd.setBounds(10, 75, 204, 20);
		txtServerMotd.setEditable(false);
		getContentPane().add(txtServerMotd);
		txtServerMotd.setColumns(10);

		// Current server address
		this.txtServerAddress = new JTextField();
		txtServerAddress.setText(DEFAULT_SERVER_ADDRESS);
		txtServerAddress.setToolTipText("The address of the server to connect to");
		txtServerAddress.setBounds(359, 10, 125, 20);
		getContentPane().add(txtServerAddress);
		txtServerAddress.setColumns(10);

		// Current username
		this.txtClientUsername = new JTextField();
		txtClientUsername.setText(DEFAULT_CLIENT_USERNAME);
		txtClientUsername.setToolTipText("The username this client is using");
		txtClientUsername.setBounds(360, 40, 125, 20);
		getContentPane().add(txtClientUsername);
		txtClientUsername.setColumns(10);

		// Box to list channels
		this.cmbServerChannels = new JComboBox<ServerChannel>();
		cmbServerChannels.setBounds(224, 72, 260, 20);
		cmbServerChannels.setEnabled(false);
		getContentPane().add(cmbServerChannels);

		// The container for the channel box
		this.serverChannelPane = new JScrollPane();
		serverChannelPane.setBounds(10, 102, 474, 150);
		serverChannelPane.setAutoscrolls(true);
		getContentPane().add(serverChannelPane);

		// Button to trigger connection request
		this.btnConnectServer = new JButton(CONNECT_BUTTON_TEXT);
		btnConnectServer.setBounds(224, 10, 125, 21);
		btnConnectServer.setEnabled(true);
		getContentPane().add(btnConnectServer);

		// Button to trigger name update request
		this.btnUpdateUsername = new JButton(UPDATE_USERNAME_BUTTON_TEXT);
		btnUpdateUsername.setBounds(224, 40, 125, 20);
		btnUpdateUsername.setEnabled(false);
		getContentPane().add(btnUpdateUsername);

		// Current instructions
		this.txtpnInstructions = new JTextPane();
		txtpnInstructions.setEditable(false);
		txtpnInstructions.setText(DEFAULT_CLIENT_INSTRUCTIONS);
		txtpnInstructions.setBackground(UIManager.getColor("Button.background"));
		txtpnInstructions.setBounds(10, 10, 205, 20);
		getContentPane().add(txtpnInstructions);

		// The text on the current selected channel
		this.txtPaneServerChannel = new JTextPane();
		txtPaneServerChannel.setEditable(false);
		txtPaneServerChannel.setAutoscrolls(true);
		serverChannelPane.setViewportView(txtPaneServerChannel);

		// Text field to chat
		this.txtChatBox = new JTextField();
		txtChatBox.setBounds(10, 260, 474, 20);
		txtChatBox.setColumns(10);
		txtChatBox.setEnabled(false);
		getContentPane().add(txtChatBox);
	}

	/**
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
	 */
	public void setServerName(String serverName) {
		txtServerName.setText(serverName);
	}

	/**
	 * Sets the displayed server message of the day.
	 * 
	 * @param serverMotd
	 *            the message of the day to display.
	 */
	public void setServerMotd(String serverMotd) {
		txtServerMotd.setText(serverMotd);
	}

	/**
	 * Sets the current displayed channels.
	 * 
	 * @param channels
	 *            the channels to display.
	 */
	public void setChannels(ServerChannel[] channels) {
		ArrayList<ServerChannel> cleaned = new ArrayList<ServerChannel>();
		for (ServerChannel channel : channels) {
			if (channel != null) {
				cleaned.add(channel);
			}
		}
		cmbServerChannels
				.setModel(new DefaultComboBoxModel<ServerChannel>(cleaned.toArray(new ServerChannel[cleaned.size()])));
	}

	/**
	 * Sets the current channel.
	 * 
	 * @param channel
	 *            the channel to display.
	 */
	public void setCurrentChannel(ServerChannel channel) {
		txtPaneServerChannel.setText(channel.getChannelText());
	}

	/**
	 * Sets the displayed instructions.
	 * 
	 * @param instructions
	 *            the instructions to display.
	 */
	public void setInstructions(String instructions) {
		txtpnInstructions.setText(instructions);
	}

	/**
	 * Enables or disables server interaction, and is used to easily reset the
	 * displayed client data.
	 * 
	 * @param connected
	 *            whether or not the client is connected.
	 */
	public void toggleServerInteraction(boolean connected) {
		this.connected = connected;

		txtServerName.setEnabled(connected);
		txtServerMotd.setEnabled(connected);
		txtServerAddress.setEditable(!connected);
		btnConnectServer.setText(!connected ? CONNECT_BUTTON_TEXT : DISCONNECT_BUTTON_TEXT);
		btnUpdateUsername.setEnabled(connected);
		cmbServerChannels.setEnabled(connected);
		txtChatBox.setEnabled(connected);

		if (connected == false) {
			txtServerName.setText(DEFAULT_SERVER_NAME);
			txtServerMotd.setText(DEFAULT_SERVER_MOTD);
			txtServerAddress.setText(DEFAULT_SERVER_ADDRESS);
			txtClientUsername.setText(DEFAULT_CLIENT_USERNAME);
			cmbServerChannels.setModel(new DefaultComboBoxModel<ServerChannel>(new ServerChannel[0]));
			txtPaneServerChannel.setText("");
			txtChatBox.setText("");
		}
	}

	/**
	 * Updates the listeners required for the frame to function properly based
	 * on the client.
	 * 
	 * @param client
	 *            the client to assign the listeners to.
	 */
	public void updateListeners(ChatClient client) {
		btnConnectServer.addActionListener(new ConnectServerListener(this, client));
		btnUpdateUsername.addActionListener(new UpdateUsernameBoxListener(this, client));
		cmbServerChannels.addActionListener(new ComboBoxServerChannelListener(this, client));
		txtChatBox.addKeyListener(new ChatBoxKeyListener(this, client));
	}

	/**
	 * Displays a message.
	 * 
	 * @param message
	 *            the message to display.
	 */
	public void displayMessage(String message) {
		JOptionPane.showMessageDialog(this, message);
	}

	/**
	 * Displays an error with the specified title and message.
	 * 
	 * @param title
	 *            the title of the error.
	 * @param error
	 *            the error message.
	 */
	public void displayError(String title, String error) {
		JOptionPane.showMessageDialog(this, error, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Displays an error using the specified exception.
	 * 
	 * @param throwable
	 *            the caught exception.
	 */
	public void displayError(Throwable throwable) {
		this.displayError("Error: " + throwable.getClass().getName(), throwable.getMessage());
	}

}
