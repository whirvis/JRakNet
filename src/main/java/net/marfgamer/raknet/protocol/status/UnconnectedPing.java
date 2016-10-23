package net.marfgamer.raknet.protocol.status;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class UnconnectedPing extends RakNetPacket {

	public long timestamp;
	public boolean magic;

	protected UnconnectedPing(boolean requiresOpenConnections) {
		super((requiresOpenConnections ? MessageIdentifier.ID_UNCONNECTED_PING_OPEN_CONNECTIONS
				: MessageIdentifier.ID_UNCONNECTED_PING));
	}

	public UnconnectedPing(Packet packet) {
		super(packet);
	}

	public UnconnectedPing() {
		this(false);
	}

	@Override
	public void encode() {
		this.writeLong(timestamp);
		this.writeMagic();
	}

	@Override
	public void decode() {
		this.timestamp = this.readLong();
		this.magic = this.checkMagic();
	}

}
