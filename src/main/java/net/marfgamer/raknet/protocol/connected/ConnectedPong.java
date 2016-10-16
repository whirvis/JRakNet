package net.marfgamer.raknet.protocol.connected;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectedPong extends RakNetPacket {

	public long identifier;

	public ConnectedPong() {
		super(MessageIdentifier.ID_CONNECTED_PONG);
	}

	public ConnectedPong(Packet packet) {
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
