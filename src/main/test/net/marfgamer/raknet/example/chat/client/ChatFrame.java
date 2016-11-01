package net.marfgamer.raknet.example.chat.client;

import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.interactive.FrameResources;

public class ChatFrame extends JFrame {

	private static final long serialVersionUID = -5459540662852804663L;
	private static final int FRAME_WIDTH = 500;
	private static final int FRAME_HEIGHT = 315;

	// Server, client and channel data
	public final JTextField txtCurrentServerName;
	public final JTextField txtServerMotd;
	public final JTextField txtServerAddress;
	public final JTextField txtClientUsername;
	public final JComboBox<ServerChannel> cmbServerChannels;
	public final JScrollPane serverChannelPane;
	public final JTextPane txtPaneServerChannel;

	// Basic info, instructions, and commands
	public final JButton btnConnectServer;
	public final JButton btnUpdateUsername;
	public final JTextPane txtpnInstructions;
	public final JTextField txtChatBox;

	public ChatFrame() {
		// Frame settings
		setResizable(false);
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setTitle("JRakNet Client Example");
		setIconImage(FrameResources.TERRARIA_RAKNET_ICON.getImage());

		// Content settings
		getContentPane().setLayout(null);

		// Current server address
		this.txtCurrentServerName = new JTextField();
		txtCurrentServerName.setBounds(10, 10, 205, 20);
		txtCurrentServerName.setEditable(false);
		getContentPane().add(txtCurrentServerName);
		txtCurrentServerName.setColumns(10);

		// Server message of the day
		this.txtServerMotd = new JTextField();
		txtServerMotd.setBounds(10, 41, 204, 20);
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
		txtClientUsername.setText("Username");
		txtClientUsername.setToolTipText("The username this client is using");
		txtClientUsername.setBounds(360, 40, 125, 20);
		getContentPane().add(txtClientUsername);
		txtClientUsername.setColumns(10);

		// Button to trigger connection request
		this.btnConnectServer = new JButton("Connect to server");
		btnConnectServer.setBounds(224, 10, 125, 21);
		btnConnectServer.setEnabled(false);
		getContentPane().add(btnConnectServer);

		// Button to trigger name update request
		this.btnUpdateUsername = new JButton("Update username");
		btnUpdateUsername.setBounds(224, 40, 125, 20);
		btnUpdateUsername.setEnabled(false);
		getContentPane().add(btnUpdateUsername);

		// Current instructions
		this.txtpnInstructions = new JTextPane();
		txtpnInstructions.setEditable(false);
		txtpnInstructions.setText("Waiting for data...");
		txtpnInstructions.setBackground(UIManager.getColor("Button.background"));
		txtpnInstructions.setBounds(10, 70, 205, 20);
		getContentPane().add(txtpnInstructions);

		// Box to list channels
		this.cmbServerChannels = new JComboBox<ServerChannel>();
		cmbServerChannels.setBounds(224, 72, 260, 20);
		cmbServerChannels.setEnabled(false);
		getContentPane().add(cmbServerChannels);

		// The container for the channel box
		this.serverChannelPane = new JScrollPane();
		serverChannelPane.setBounds(10, 102, 474, 150);
		getContentPane().add(serverChannelPane);

		// The text on the current selected channel
		this.txtPaneServerChannel = new JTextPane();
		txtPaneServerChannel.setEditable(false);
		serverChannelPane.setViewportView(txtPaneServerChannel);

		// Text field to chat
		this.txtChatBox = new JTextField();
		txtChatBox.setBounds(10, 260, 474, 20);
		txtChatBox.setColumns(10);
		txtChatBox.setEnabled(false);
		txtChatBox.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
		txtChatBox.getActionMap().put("enter", null); // TODO
		getContentPane().add(txtChatBox);
	}

	public void displayError(Throwable throwable) {
		JOptionPane.showMessageDialog(this, throwable.getMessage(), "Error: " + throwable.getClass().getSimpleName(),
				JOptionPane.ERROR_MESSAGE);
	}

	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		ChatFrame frame = new ChatFrame();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.displayError(new RakNetException("I AM TRIGGERED"));
	}

}
