package net.marfgamer.raknet.example.chat.protocol;

import java.util.UUID;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.server.ConnectedClient;

public class LoginAccepted extends ChatMessagePacket {

	public UUID userId;
	public ServerChannel[] channels;
	public ConnectedClient[] onlineUsers;

	public LoginAccepted() {
		super(ChatMessageIdentifier.ID_LOGIN_ACCEPTED);
	}

	public LoginAccepted(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeUUID(userId);
		this.writeInt(channels.length);
		for (int i = 0; i < channels.length; i++) {
			this.writeUByte(channels[i].getChannel());
			this.writeString(channels[i].getName());
		}
		this.writeInt(onlineUsers.length);
		for (int i = 0; i < onlineUsers.length; i++) {
			this.writeUUID(onlineUsers[i].getUUID());
			this.writeString(onlineUsers[i].getUsername());
		}
	}

	@Override
	public void decode() {
		this.userId = this.readUUID();
		this.channels = new ServerChannel[this.readInt()];
		for (int i = 0; i < channels.length; i++) {
			short channel = this.readUByte();
			String channelName = this.readString();
			channels[i] = new ServerChannel(channel, channelName);
		}
		this.onlineUsers = new ConnectedClient[this.readInt()];
		for (int i = 0; i < onlineUsers.length; i++) {
			onlineUsers[i] = new ConnectedClient(null, this.readUUID(), this.readString());
		}
	}

}
