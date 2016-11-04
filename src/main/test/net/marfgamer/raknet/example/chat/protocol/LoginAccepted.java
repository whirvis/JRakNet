package net.marfgamer.raknet.example.chat.protocol;

import java.util.UUID;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.raknet.example.chat.ServerChannel;

public class LoginAccepted extends ChatMessagePacket {

	public UUID userId;
	public String serverName;
	public String serverMotd;
	public ServerChannel[] channels;

	public LoginAccepted() {
		super(ChatMessageIdentifier.ID_LOGIN_ACCEPTED);
	}

	public LoginAccepted(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeUUID(userId);
		this.writeString(serverName);
		this.writeString(serverMotd);
		this.writeInt(channels.length);
		for (int i = 0; i < channels.length; i++) {
			this.writeUByte(channels[i].getChannel());
			this.writeString(channels[i].getName());
		}
	}

	@Override
	public void decode() {
		this.userId = this.readUUID();
		this.serverName = this.readString();
		this.serverMotd = this.readString();
		this.channels = new ServerChannel[this.readInt()];
		for (int i = 0; i < channels.length; i++) {
			short channel = this.readUByte();
			String channelName = this.readString();
			channels[i] = new ServerChannel(channel, channelName);
		}
	}

}
