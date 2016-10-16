package net.marfgamer.raknet.protocol.connected;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectedConnectionRequest extends RakNetPacket {

	public long clientGuid;
	public long timestamp;

	public ConnectedConnectionRequest() {
		super(MessageIdentifier.ID_CONNECTION_REQUEST);
	}

	public ConnectedConnectionRequest(Packet packet) {
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
