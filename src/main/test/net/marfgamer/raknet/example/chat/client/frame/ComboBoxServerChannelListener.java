package net.marfgamer.raknet.example.chat.client.frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.client.ChatClient;

public class ComboBoxServerChannelListener implements ActionListener {

	private final ChatFrame frame;
	private final ChatClient client;

	public ComboBoxServerChannelListener(ChatFrame frame, ChatClient client) {
		this.frame = frame;
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ServerChannel serverChannel = (ServerChannel) frame.cmbServerChannels.getSelectedItem();
		client.setChannel(serverChannel.getChannel());
	}

}
