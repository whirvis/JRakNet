package net.marfgamer.raknet.example.chat.server;

import java.util.UUID;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.protocol.LoginAccepted;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.session.RakNetSession;

public class ConnectedClient {

	public static final int USER_STATUS_CLIENT_CONNECTED = 0x00;
	public static final int USER_STATUS_CLIENT_DISCONNECTED = 0x01;

	private final RakNetSession session;
	private final UUID uuid;
	private String username;

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

	public void acceptLogin(String name, String motd, ServerChannel[] channels) {
		LoginAccepted accepted = new LoginAccepted();
		accepted.userId = this.uuid;
		accepted.serverName = name;
		accepted.serverMotd = motd;
		accepted.channels = channels;
		accepted.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, accepted);
	}

	public void acceptUsernameUpdate(String username) {
		this.username = username;
		session.sendMessage(Reliability.RELIABLE_ORDERED,
				new RakNetPacket(ChatMessageIdentifier.ID_UPDATE_USERNAME_ACCEPTED));
	}

	public void denyUsernameUpdate() {
		session.sendMessage(Reliability.RELIABLE_ORDERED,
				new RakNetPacket(ChatMessageIdentifier.ID_UPDATE_USERNAME_FAILURE));
	}

	public void sendChatMessage(String message, int channel) {
		RakNetPacket chatPacket = new RakNetPacket(ChatMessageIdentifier.ID_CHAT_MESSAGE);
		chatPacket.writeString(message);
		session.sendMessage(Reliability.RELIABLE_ORDERED, channel, chatPacket);
	}

	public void addChannel(int channel, String name) {
		RakNetPacket addChannel = new RakNetPacket(ChatMessageIdentifier.ID_ADD_CHANNEL);
		addChannel.writeUByte(channel);
		addChannel.writeString(name);
		session.sendMessage(Reliability.RELIABLE_ORDERED, addChannel);
	}

	public void removeChannel(int channel) {
		RakNetPacket removeChannel = new RakNetPacket(ChatMessageIdentifier.ID_REMOVE_CHANNEL);
		removeChannel.writeUByte(channel);
		session.sendMessage(Reliability.RELIABLE_ORDERED, removeChannel);
	}

	public void kick(String reason) {
		RakNetPacket kickPacket = new RakNetPacket(ChatMessageIdentifier.ID_KICK);
		kickPacket.writeString(reason);
		session.sendMessage(Reliability.UNRELIABLE, kickPacket);
	}

}
