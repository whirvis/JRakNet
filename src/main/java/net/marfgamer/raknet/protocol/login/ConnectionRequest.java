package net.marfgamer.raknet.protocol.login;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectionRequest extends RakNetPacket {

	public long clientGuid;
	public long timestamp;

	public ConnectionRequest() {
		super(MessageIdentifier.ID_CONNECTION_REQUEST);
	}

	public ConnectionRequest(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeLong(clientGuid);
		this.writeLong(timestamp);
	}

	@Override
	public void decode() {
		this.clientGuid = this.readLong();
		this.timestamp = this.readLong();
	}

}
