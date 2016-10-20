package net.marfgamer.raknet.protocol.status;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.protocol.MessageIdentifier;
import net.marfgamer.raknet.server.identifier.Identifier;

public class UnconnectedPong extends RakNetPacket {

	public long pingId;
	public long pongId;
	public boolean magic;
	public Identifier identifier;

	public UnconnectedPong() {
		super(MessageIdentifier.ID_UNCONNECTED_PONG);
	}

	public UnconnectedPong(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeLong(pingId);
		this.writeLong(pongId);
		this.writeMagic();
		this.writeString(identifier.build());
	}

	@Override
	public void decode() {
		this.pingId = this.readLong();
		this.pongId = this.readLong();
		this.magic = this.checkMagic();
		this.identifier = new Identifier(this.readString());
	}

}
