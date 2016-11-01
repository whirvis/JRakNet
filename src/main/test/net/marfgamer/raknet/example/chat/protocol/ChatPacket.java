package net.marfgamer.raknet.example.chat.protocol;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;

public class ChatPacket extends ChatMessagePacket {

	public String message;

	public ChatPacket() {
		super(ChatMessageIdentifier.ID_CHAT_MESSAGE);
	}

	public ChatPacket(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeString(message);
	}

	@Override
	public void decode() {
		this.message = this.readString();
	}

}
