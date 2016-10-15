package net.marfgamer.raknet.protocol.client;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectedClientHandshake extends RakNetPacket {

	public InetSocketAddress clientAddress;
	public long serverTimestamp;
	public long timestamp;

	public ConnectedClientHandshake(Packet packet) {
		super(packet);
	}

	public ConnectedClientHandshake() {
		super(MessageIdentifier.ID_NEW_INCOMING_CONNECTION);
	}

	@Override
	public void encode() throws UnknownHostException {
		this.writeAddress(clientAddress);
		for (int i = 0; i < 10; i++) {
			this.writeAddress("255.255.255.255", 19132);
		}
		this.writeLong(serverTimestamp);
		this.writeLong(timestamp);
	}

	@Override
	public void decode() throws UnknownHostException {
		this.clientAddress = this.readAddress();
		for (int i = 0; i < 10; i++) {
			this.readAddress(); // Ignore, unknown use
		}
		this.serverTimestamp = this.readLong();
		this.timestamp = this.readLong();
	}

}
