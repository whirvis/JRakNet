package net.marfgamer.raknet.example.chat.client.frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.marfgamer.raknet.example.chat.client.ChatClient;
import net.marfgamer.raknet.example.chat.exception.ChatException;

public class UpdateUsernameBoxListener implements ActionListener {

	private final ChatFrame frame;
	private final ChatClient client;

	public UpdateUsernameBoxListener(ChatFrame frame, ChatClient client) {
		this.frame = frame;
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			client.setUsernameRequest(frame.txtClientUsername.getText());
		} catch (ChatException e1) {
			frame.displayError(e1);
		}
	}

}
