package net.marfgamer.raknet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

public class RakNetPacket extends Packet {

	private final short id;

	public RakNetPacket(int id) {
		super();
		if (id < 0) {
			throw new IllegalArgumentException("The packet ID is unsigned, it must be at least 0!");
		}
		this.writeUByte(this.id = (short) id);
	}

	public RakNetPacket(ByteBuf buffer) {
		super(buffer);
		if (this.remaining() < 1) {
			throw new IllegalArgumentException("The packet contains no data, it has no ID to be read!");
		}
		this.id = this.readUByte();
	}

	public RakNetPacket(DatagramPacket datagram) {
		this(datagram.content());
	}

	public RakNetPacket(Packet packet) {
		super(packet);

		// Make sure this isn't an existing RakNetPacket!
		if (packet instanceof RakNetPacket) {
			this.id = ((RakNetPacket) packet).id;
		} else {
			this.id = this.readUByte();
		}
	}

	public final short getId() {
		return this.id;
	}

	public void encode() {
	}

	public void decode() {
	}

}
