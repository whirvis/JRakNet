package net.marfgamer.raknet.example.chat.protocol;

import java.util.UUID;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ChatMessagePacket extends RakNetPacket {

	public ChatMessagePacket(int id) {
		super(id);
		if (id < MessageIdentifier.ID_USER_PACKET_ENUM) {
			throw new IllegalArgumentException("Packet ID too low!");
		}
	}

	public ChatMessagePacket(Packet packet) {
		super(packet);
	}

	public UUID readUUID() {
		long mostSigBits = this.readLong();
		long leastSigBits = this.readLong();
		return new UUID(mostSigBits, leastSigBits);
	}

	public void writeUUID(UUID uuid) {
		this.writeLong(uuid.getMostSignificantBits());
		this.writeLong(uuid.getLeastSignificantBits());
	}

}
