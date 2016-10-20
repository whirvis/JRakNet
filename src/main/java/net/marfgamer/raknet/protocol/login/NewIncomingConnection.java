package net.marfgamer.raknet.protocol.login;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class NewIncomingConnection extends RakNetPacket {

	public InetSocketAddress serverAddress;
	public long serverTimestamp;
	public long clientTimestamp;

	public NewIncomingConnection(Packet packet) {
		super(packet);
	}

	public NewIncomingConnection() {
		super(MessageIdentifier.ID_NEW_INCOMING_CONNECTION);
	}

	@Override
	public void encode() {
		this.writeAddress(serverAddress);
		for (int i = 0; i < 10; i++) {
			this.writeAddress("255.255.255.255", 19132);
		}
		this.writeLong(serverTimestamp);
		this.writeLong(clientTimestamp);
	}

	@Override
	public void decode() {
		this.serverAddress = this.readAddress();
		for (int i = 0; i < 10; i++) {
			this.readAddress(); // Ignore, unknown use
		}
		this.serverTimestamp = this.readLong();
		this.clientTimestamp = this.readLong();
	}

}
