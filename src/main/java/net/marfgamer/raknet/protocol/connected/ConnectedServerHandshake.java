package net.marfgamer.raknet.protocol.connected;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectedServerHandshake extends RakNetPacket {
	
	public InetSocketAddress clientAddress;
	public long clientTimestamp;
	public long serverTimestamp;
	
	public ConnectedServerHandshake() {
		super(MessageIdentifier.ID_CONNECTION_REQUEST_ACCEPTED);
	}
	
	public ConnectedServerHandshake(Packet packet) {
		super(packet);
	}
	
	@Override
	public void encode() throws UnknownHostException {
		this.writeAddress(clientAddress);
		this.writeShort(0);
		for (int i = 0; i < 10; i++) {
			this.writeAddress("255.255.255.255", 19132);
		}
		this.writeLong(clientTimestamp);
		this.writeLong(serverTimestamp);
	}

	@Override
	public void decode() throws UnknownHostException {
		this.clientAddress = this.readAddress();
		this.readShort(); // Unknown use
		for (int i = 0; i < 10; i++) {
			this.readAddress(); // Unknown use
		}
		this.clientTimestamp = this.readLong();
		this.serverTimestamp = this.readLong();
	}

}
