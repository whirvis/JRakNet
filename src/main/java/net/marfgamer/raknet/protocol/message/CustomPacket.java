package net.marfgamer.raknet.protocol.message;

import java.util.ArrayList;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class CustomPacket extends RakNetPacket {

	public static final int SEQUENCE_NUMBER_LENGTH = 0x03;

	public int sequenceNumber;
	public ArrayList<EncapsulatedPacket> messages;

	public CustomPacket() {
		super(MessageIdentifier.ID_RESERVED_4);
		this.messages = new ArrayList<EncapsulatedPacket>();
	}

	public CustomPacket(Packet packet) {
		super(packet);
		this.messages = new ArrayList<EncapsulatedPacket>();
	}

	@Override
	public void encode() {
		this.writeTriadLE(sequenceNumber);
		for (EncapsulatedPacket packet : messages) {
			// Encode packet and write to buffer
			packet.buffer = this;
			packet.encode();

			// Buffer is no longer needed, proceed
			packet.buffer = null;
		}
	}

	@Override
	public void decode() {
		this.sequenceNumber = this.readTriadLE();
		while (this.remaining() >= EncapsulatedPacket.MINIMUM_BUFFER_LENGTH) {
			// Decode packet
			EncapsulatedPacket packet = new EncapsulatedPacket();
			packet.buffer = new Packet(this.buffer());
			packet.decode();

			// Buffer is no longer needed, add the packet to the list
			packet.buffer = null;
			messages.add(packet);
		}
	}

	public int calculateSize() {
		int packetSize = 1; // Packet ID
		packetSize += SEQUENCE_NUMBER_LENGTH;
		for (EncapsulatedPacket message : this.messages) {
			packetSize += message.calculateSize();
		}
		return packetSize;
	}
	
	public boolean containsUnreliables() {
		for (EncapsulatedPacket encapsulated : this.messages) {
			if (!encapsulated.reliability.isReliable()) {
				return true;
			}
		}
		return false;
	}
	
	public void removeUnreliables() {
		for (EncapsulatedPacket encapsulated : this.messages) {
			if (!encapsulated.reliability.isReliable()) {
				messages.remove(encapsulated);
			}
		}
	}

	public static int calculateDummy() {
		CustomPacket custom = new CustomPacket();
		custom.encode();
		return custom.size();
	}

}
