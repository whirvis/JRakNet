package net.marfgamer.raknet.protocol.status;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectedPing extends RakNetPacket {

	public long identifier;

	public ConnectedPing() {
		super(MessageIdentifier.ID_CONNECTED_PING);
	}

	public ConnectedPing(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeLong(identifier);
	}

	@Override
	public void decode() {
		this.identifier = this.readLong();
	}

}
