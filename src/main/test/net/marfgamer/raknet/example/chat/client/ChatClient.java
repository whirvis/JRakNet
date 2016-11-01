package net.marfgamer.raknet.example.chat.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetSocketAddress;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.client.RakNetClientListener;
import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.exception.ChatException;
import net.marfgamer.raknet.example.chat.protocol.LoginRequest;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.session.RakNetServerSession;
import net.marfgamer.raknet.util.RakNetUtils;

public class ChatClient implements RakNetClientListener {

	private static final String CHAT_INSTRUCTIONS_DISCONNECTED = "Please connect to a server...";
	private static final String CHAT_INSTRUCTIONS_CONNECTING = "Connecting to the server...";
	private static final String CHAT_INSTRUCTIONS_CONNECTED = "Connected, press enter to chat!";

	private final ChatFrame frame;
	private final RakNetClient client;
	private final ServerChannel[] channels;
	private RakNetServerSession session;
	private String username;

	public ChatClient(ChatFrame frame) {
		this.frame = frame;
		frame.txtpnInstructions.setText(CHAT_INSTRUCTIONS_DISCONNECTED);
		frame.btnConnectServer.setEnabled(true);

		this.client = new RakNetClient().setListener(this);
		this.channels = new ServerChannel[RakNet.MAX_CHANNELS];
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsernameRequest(String username) throws ChatException {
		if (username.length() <= 0) {
			throw new ChatException("Name is too short!");
		} else if (session == null) {
			throw new ChatException("Not connected to a server!");
		}
	}

	public void connect(String address) {
		try {
			InetSocketAddress socketAddress = RakNetUtils.parseAddress(address, 30851);
			frame.txtpnInstructions.setText(CHAT_INSTRUCTIONS_CONNECTING);
			this.username = frame.txtClientUsername.getText();
			client.connectThreaded(socketAddress);
		} catch (Exception e) {
			frame.displayError(e);
		}
	}

	public void disconnect() {
		client.disconnect();
	}

	@Override
	public void onConnect(RakNetServerSession session) {
		this.session = session;

		LoginRequest request = new LoginRequest();
		request.username = this.username;
		request.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, request);
	}

	@Override
	public void onDisconnect(RakNetServerSession session, String reason) {
		this.session = null;
		frame.txtpnInstructions.setText(CHAT_INSTRUCTIONS_DISCONNECTED);
		frame.btnUpdateUsername.setEnabled(false);
	}

	@Override
	public void onThreadException(Throwable throwable) {
		frame.displayError(throwable);
		this.disconnect();
	}

	public static void main(String[] args) {
		try {
			// Create frame
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			ChatFrame frame = new ChatFrame();

			// Create client
			ChatClient client = new ChatClient(frame);

			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent e) {
					int confirm = JOptionPane.showOptionDialog(null, "Are you sure you want to close the client?",
							"Exit Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null,
							null);
					if (confirm == 0) {
						client.disconnect();
						System.exit(0);
					}
				}

			});

			frame.btnConnectServer.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					client.connect(frame.txtServerAddress.getText());
				}

			});

			frame.setVisible(true);
		} catch (Exception e) {

		}
	}

}
