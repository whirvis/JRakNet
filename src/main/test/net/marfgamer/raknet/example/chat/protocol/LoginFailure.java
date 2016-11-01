package net.marfgamer.raknet.example.chat.protocol;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;

public class LoginFailure extends ChatMessagePacket {

	public String reason;

	public LoginFailure() {
		super(ChatMessageIdentifier.ID_LOGIN_FAILURE);
	}

	public LoginFailure(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeString(this.reason);
	}

	@Override
	public void decode() {
		this.reason = this.readString();
	}

}
