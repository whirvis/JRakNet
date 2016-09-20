package net.marfgamer.raknet.protocol;

import io.netty.buffer.Unpooled;
import net.marfgamer.raknet.Packet;

public class EncapsulatedPacket {

	public static final int MINIMUM_BUFFER_LENGTH = 0x04;

	// Bitflags
	public static final byte RELIABILITY_POSITION = (byte) 0b00000101;
	public static final byte FLAG_RELIABILITY = (byte) 0b11100000;
	public static final byte FLAG_SPLIT = (byte) 0b00010000;

	// Used to encode and decode, normally modified by CustomPacket
	public Packet buffer = new Packet();

	// Encapsulation data
	public Reliability reliability;
	public boolean split;
	public int messageIndex;
	public int orderIndex;
	public byte orderChannel;
	public int splitCount;
	public int splitId;
	public int splitIndex;
	public Packet payload;

	public void encode() {
		buffer.writeByte((byte) ((reliability.asByte() << RELIABILITY_POSITION) | (split ? FLAG_SPLIT : 0)));
		buffer.writeUShort(payload.size());

		if (reliability.isReliable()) {
			buffer.writeTriadLE(messageIndex);
		}

		if (reliability.isOrdered() || reliability.isSequenced()) {
			buffer.writeTriadLE(orderIndex);
			buffer.writeUByte(orderChannel);
		}

		if (split == true) {
			buffer.writeInt(splitCount);
			buffer.writeUShort(splitId);
			buffer.writeInt(splitIndex);
		}

		buffer.write(payload.array());
	}

	public void decode() {
		byte flags = buffer.readByte();
		this.reliability = Reliability.lookup((byte) (byte) ((flags & 0b11100000) >> 5));
		this.split = (flags & FLAG_SPLIT) > 0;

		int length = buffer.readUShort();

		if (reliability.isReliable()) {
			this.messageIndex = buffer.readTriadLE();
		}

		if (reliability.isOrdered() || reliability.isSequenced()) {
			this.orderIndex = buffer.readTriadLE();
			this.orderChannel = buffer.readByte();
		}

		if (split == true) {
			this.splitCount = buffer.readInt();
			this.splitId = buffer.readUShort();
			this.splitIndex = buffer.readInt();
		}

		this.payload = new Packet(Unpooled.copiedBuffer(buffer.read(length)));
	}

}
