package net.marfgamer.raknet;

public abstract class RakNetPacket extends Packet {

	private final short id;

	public RakNetPacket(int id) {
		super();
		if (id < 0) {
			throw new IllegalArgumentException("The packet ID is unsigned, it must be at least 0!");
		}
		this.writeUByte(this.id = (short) id);
	}

	public RakNetPacket(Packet packet) {
		super(packet);
		if (this.remaining() < 1) {
			throw new IllegalArgumentException("The packet contains no data, it has no ID to be read!");
		}
		this.id = this.readUByte();
	}

	public final short getId() {
		return this.id;
	}

	public abstract void encode();

	public abstract void decode();

}
