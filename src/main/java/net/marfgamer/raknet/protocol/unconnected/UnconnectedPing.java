package net.marfgamer.raknet.protocol.unconnected;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class UnconnectedPing extends RakNetPacket {

	public long time;
	public long pingId;

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
		this.writeLong(time);
		this.writeLong(pingId);
	}

	@Override
	public void decode() {
		this.time = this.readLong();
		this.pingId = this.readLong();
	}

}
