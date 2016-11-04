package net.marfgamer.raknet.example.chat.client.frame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.client.ChatClient;

public class ChatBoxKeyListener implements KeyListener {

	private final ChatFrame frame;
	private final ChatClient client;

	public ChatBoxKeyListener(ChatFrame frame, ChatClient client) {
		this.frame = frame;
		this.client = client;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			ServerChannel channel = (ServerChannel) frame.cmbServerChannels.getSelectedItem();
			client.sendChatMessage(frame.txtChatBox.getText(), channel.getChannel());
			frame.txtChatBox.setText("");
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// not used
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Not used
	}

}
