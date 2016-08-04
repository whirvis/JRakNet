package net.marfgamer.raknet.event.reliability;

import net.marfgamer.raknet.event.Event;

public class PacketAcknowledgedEvent extends Event {
	
	private final int orderIndex;
	private final int orderChannel;
	
	public PacketAcknowledgedEvent(int orderIndex, int orderChannel) {
		this.orderIndex = orderIndex;
		this.orderChannel = orderChannel;
	}
	
	public int getOrderIndex() {
		return this.orderIndex;
	}
	
	public int getOrderChannel() {
		return this.orderChannel;
	}
	
}
