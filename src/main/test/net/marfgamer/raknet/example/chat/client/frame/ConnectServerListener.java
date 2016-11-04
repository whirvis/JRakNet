package net.marfgamer.raknet.example.chat.client.frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.marfgamer.raknet.example.chat.client.ChatClient;

public class ConnectServerListener implements ActionListener {

	private final ChatFrame frame;
	private final ChatClient client;

	public ConnectServerListener(ChatFrame frame, ChatClient client) {
		this.frame = frame;
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (frame.connected == false) {
			client.connect(frame.txtServerAddress.getText());
		} else {
			frame.txtServerAddress.setText("");
			frame.txtClientUsername.setText("");
			client.disconnect("Client disconnected");
		}
	}

}
