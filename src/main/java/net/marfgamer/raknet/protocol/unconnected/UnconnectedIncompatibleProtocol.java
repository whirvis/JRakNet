package net.marfgamer.raknet.protocol.unconnected;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class UnconnectedIncompatibleProtocol extends RakNetPacket {

	public int networkProtocol;
	public long serverGuid;

	public UnconnectedIncompatibleProtocol(Packet packet) {
		super(packet);
	}

	public UnconnectedIncompatibleProtocol() {
		super(MessageIdentifier.ID_INCOMPATIBLE_PROTOCOL_VERSION);
	}

	@Override
	public void encode() {
		this.writeUByte(networkProtocol);
		this.writeMagic();
		this.writeLong(serverGuid);
	}

	@Override
	public void decode() {

	}

}
