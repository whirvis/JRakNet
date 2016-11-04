package net.marfgamer.raknet.example.chat.client.frame;

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

import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.client.ChatClient;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.interactive.FrameResources;

public class ChatFrame extends JFrame {

	private static final long serialVersionUID = -5459540662852804663L;

	private static final int FRAME_WIDTH = 500;
	private static final int FRAME_HEIGHT = 315;

	private static final String DEFAULT_SERVER_ADDRESS = "localhost";
	private static final String DEFAULT_SERVER_NAME = "Server name";
	private static final String DEFAULT_SERVER_MOTD = "Server MOTD";
	private static final String DEFAULT_CLIENT_USERNAME = "Username";
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
		setIconImage(FrameResources.TERRARIA_RAKNET_ICON.getImage());

		// Content settings
		getContentPane().setLayout(null);

		// Current server address
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

		// Server address to connect to
		this.txtServerAddress = new JTextField();
		txtServerAddress.setText("localhost");
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
		txtpnInstructions.setText("Waiting for data...");
		txtpnInstructions.setBackground(UIManager.getColor("Button.background"));
		txtpnInstructions.setBounds(10, 10, 205, 20);
		getContentPane().add(txtpnInstructions);

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

	public String getUsername() {
		return txtClientUsername.getText();
	}

	public void setServerName(String serverName) {
		txtServerName.setText(serverName);
	}

	public void setServerMotd(String serverMotd) {
		txtServerMotd.setText(serverMotd);
	}

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

	public void setCurrentChannel(ServerChannel channel) {
		txtPaneServerChannel.setText(channel.getChannelText());
	}

	public void setInstructions(String instructions) {
		txtpnInstructions.setText(instructions);
	}

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

	public void updateListeners(ChatClient client) {
		btnConnectServer.addActionListener(new ConnectServerListener(this, client));
		btnUpdateUsername.addActionListener(new UpdateUsernameBoxListener(this, client));
		cmbServerChannels.addActionListener(new ComboBoxServerChannelListener(this, client));
		txtChatBox.addKeyListener(new ChatBoxKeyListener(this, client));
	}

	public void displayMessage(String message) {
		JOptionPane.showMessageDialog(this, message);
	}

	public void displayError(String title, String error) {
		JOptionPane.showMessageDialog(this, error, title, JOptionPane.ERROR_MESSAGE);
	}

	public void displayError(Throwable throwable) {
		this.displayError("Error: " + throwable.getClass().getName(), throwable.getMessage());
	}

	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		ChatFrame frame = new ChatFrame();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.displayError(new RakNetException("I AM TRIGGERED"));
	}

}
