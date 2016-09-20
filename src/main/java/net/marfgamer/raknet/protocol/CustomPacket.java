package net.marfgamer.raknet.protocol;

import java.util.ArrayList;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.Packet;

public class CustomPacket extends RakNetPacket {

	public int seqNumber;
	public ArrayList<EncapsulatedPacket> messages;

	public CustomPacket() {
		super(MessageIdentifier.ID_RESERVED_4);
	}

	@Override
	public void encode() {
		this.writeTriadLE(seqNumber);
		for (EncapsulatedPacket packet : messages) {
			// Encode packet and write to buffer
			packet.encode();
			this.write(packet.buffer.array());

			// Buffer is no longer needed
			packet.buffer = null;
		}
	}

	@Override
	public void decode() {
		this.seqNumber = this.readTriadLE();
		while (this.remaining() >= EncapsulatedPacket.MINIMUM_BUFFER_LENGTH) {
			// Encode packet
			EncapsulatedPacket packet = new EncapsulatedPacket();
			packet.buffer = new Packet(this.buffer());
			packet.decode();

			// Buffer is no longer needed, add the packet to the list
			packet.buffer = null;
			messages.add(packet);
		}
	}

}
