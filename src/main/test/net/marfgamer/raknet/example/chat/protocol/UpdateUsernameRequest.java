package net.marfgamer.raknet.example.chat.protocol;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;

public class UpdateUsernameRequest extends ChatMessagePacket {

	public String newUsername;

	public UpdateUsernameRequest() {
		super(ChatMessageIdentifier.ID_UPDATE_USERNAME_REQUEST);
	}

	public UpdateUsernameRequest(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeString(newUsername);
	}

	@Override
	public void decode() {
		this.newUsername = this.readString();
	}

}
