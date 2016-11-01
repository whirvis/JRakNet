package net.marfgamer.raknet.example.chat.protocol;

import java.util.UUID;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;

public class AddUserPacket extends ChatMessagePacket {

	public UUID userId;
	public String username;

	public AddUserPacket() {
		super(ChatMessageIdentifier.ADD_USER);
	}

	public AddUserPacket(RakNetPacket packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeUUID(userId);
		this.writeString(username);
	}

	@Override
	public void decode() {
		this.userId = this.readUUID();
		this.username = this.readString();
	}

}
