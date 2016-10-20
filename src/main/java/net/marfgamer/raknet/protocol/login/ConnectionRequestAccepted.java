package net.marfgamer.raknet.protocol.login;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectionRequestAccepted extends RakNetPacket {

	public InetSocketAddress clientAddress;
	public long clientTimestamp;
	public long serverTimestamp;

	public ConnectionRequestAccepted() {
		super(MessageIdentifier.ID_CONNECTION_REQUEST_ACCEPTED);
	}

	public ConnectionRequestAccepted(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeAddress(clientAddress);
		this.writeShort(0);
		for (int i = 0; i < 10; i++) {
			this.writeAddress("255.255.255.255", 19132);
		}
		this.writeLong(clientTimestamp);
		this.writeLong(serverTimestamp);
	}

	@Override
	public void decode() {
		this.clientAddress = this.readAddress();
		this.readShort(); // Unknown use
		for (int i = 0; i < 10; i++) {
			this.readAddress(); // Unknown use
		}
		this.clientTimestamp = this.readLong();
		this.serverTimestamp = this.readLong();
	}

}
