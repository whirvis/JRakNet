package net.marfgamer.raknet.example.chat.server;

import java.util.UUID;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.session.RakNetSession;

public class ConnectedClient {

	private final RakNetSession session;
	private final UUID uuid;
	private final String username;

	public ConnectedClient(RakNetSession session, UUID uuid, String username) {
		this.session = session;
		this.uuid = uuid;
		this.username = username;
	}

	public UUID getUUID() {
		return this.uuid;
	}

	public String getUsername() {
		return this.username;
	}

	public void sendChatMessage(String message, int channel) {
		RakNetPacket chatPacket = new RakNetPacket(ChatMessageIdentifier.ID_CHAT_MESSAGE);
		chatPacket.writeString(message);
		session.sendMessage(Reliability.RELIABLE_ORDERED, channel, chatPacket);
	}

	public void kick(String reason) {
		RakNetPacket kickPacket = new RakNetPacket(ChatMessageIdentifier.ID_KICK);
		kickPacket.writeString(reason);
		session.sendMessage(Reliability.UNRELIABLE, kickPacket);
	}

}
