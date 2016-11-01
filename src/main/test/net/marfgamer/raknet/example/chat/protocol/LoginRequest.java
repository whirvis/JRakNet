package net.marfgamer.raknet.example.chat.protocol;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;

public class LoginRequest extends ChatMessagePacket {

	public String username;

	public LoginRequest() {
		super(ChatMessageIdentifier.ID_LOGIN_REQUEST);
	}

	public LoginRequest(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeString(username);
	}

	@Override
	public void decode() {
		this.username = this.readString();
	}

}
