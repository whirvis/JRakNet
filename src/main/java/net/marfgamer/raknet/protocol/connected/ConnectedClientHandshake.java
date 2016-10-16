package net.marfgamer.raknet.protocol.connected;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectedClientHandshake extends RakNetPacket {

	public InetSocketAddress serverAddress;
	public long serverTimestamp;
	public long clientTimestamp;

	public ConnectedClientHandshake(Packet packet) {
		super(packet);
	}

	public ConnectedClientHandshake() {
		super(MessageIdentifier.ID_NEW_INCOMING_CONNECTION);
	}

	@Override
	public void encode() throws UnknownHostException {
		this.writeAddress(serverAddress);
		for (int i = 0; i < 10; i++) {
			this.writeAddress("255.255.255.255", 19132);
		}
		this.writeLong(serverTimestamp);
		this.writeLong(clientTimestamp);
	}

	@Override
	public void decode() throws UnknownHostException {
		this.serverAddress = this.readAddress();
		for (int i = 0; i < 10; i++) {
			this.readAddress(); // Ignore, unknown use
		}
		this.serverTimestamp = this.readLong();
		this.clientTimestamp = this.readLong();
	}

}
