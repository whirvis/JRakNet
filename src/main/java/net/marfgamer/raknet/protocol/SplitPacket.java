package net.marfgamer.raknet.protocol;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.protocol.message.EncapsulatedPacket;
import net.marfgamer.raknet.util.map.IntMap;

public class SplitPacket {

	private final int splitId;
	private final int splitCount;
	private final Reliability reliability;

	private final IntMap<Packet> payloads;

	public SplitPacket(int splitId, int splitCount, Reliability reliability) {
		this.splitId = splitId;
		this.splitCount = splitCount;
		this.reliability = reliability;
		this.payloads = new IntMap<Packet>();
	}

	public Packet update(EncapsulatedPacket encapsulated) {
		// Update payload data
		if (encapsulated.split != true || encapsulated.splitId != this.splitId
				|| encapsulated.splitCount != this.splitCount || encapsulated.reliability != this.reliability) {
			throw new IllegalArgumentException("This split packet does not belong to this one!");
		}
		payloads.put(encapsulated.splitIndex, encapsulated.payload);

		// If the map is large enough then put the packet together and return it
		if (payloads.size() >= this.splitCount) {
			Packet finalPayload = new Packet();
			for (int i = 0; i < payloads.size(); i++) {
				finalPayload.write(payloads.get(i).array());
			}
			return finalPayload;
		}

		// The packet is not yet ready
		return null;
	}

}
