package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.CustomPacket;
import net.marfgamer.raknet.protocol.EncapsulatedPacket;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.SplitPacket;
import net.marfgamer.raknet.protocol.acknowledge.Acknowledge;
import net.marfgamer.raknet.util.ArrayUtils;
import net.marfgamer.raknet.util.map.IntMap;

public abstract class RakNetSession {

	private static final int GLOBAL_CHANNEL = 0x00;

	// Netty data
	private final InetSocketAddress address;
	private final Channel channel;

	// Message ordering
	private int[] orderedSendIndex;
	private int[] orderedReceiveIndex;
	private int[] sequencedSendIndex;
	private int[] sequencedReceiveIndex;
	private IntMap<IntMap<Packet>> orderedHandleQueue;

	// Data ordering
	private int sendSeqNumber = 0;
	private int receiveSeqNumber = 0;
	private int sendMessageIndex = 0;
	private int receiveMessageIndex = 0;
	private ArrayList<Integer> reliableHandledPackets;

	// SplitPacket handling
	private IntMap<SplitPacket> splitPackets;

	public RakNetSession(InetSocketAddress address, Channel channel) {
		this.address = address;
		this.channel = channel;

		// Initialize all channel based HashMaps
		this.orderedHandleQueue = new IntMap<IntMap<Packet>>();
		for (int i = 0; i < RakNet.MAX_CHANNELS; i++) {
			orderedHandleQueue.put(i, new IntMap<Packet>());
		}

		this.splitPackets = new IntMap<SplitPacket>();
	}

	public InetSocketAddress getSocketAddress() {
		return this.address;
	}

	public void handleCustom(CustomPacket packet) {
		// Send needed ACK packets
		int[] missing = ArrayUtils.subtractionArray(packet.seqNumber, this.receiveSeqNumber);
		if (missing.length > 0) {
			Acknowledge ack = new Acknowledge(Acknowledge.ACKNOWLEDGED);
			for (int missingPacket : missing) {
				ack.addRecord(missingPacket);
			}
			ack.encode();
		}

		// Handle the EncapsulatedPackets
		for (EncapsulatedPacket message : packet.messages) {
			this.handleMessage(message);
		}
	}

	public void handleMessage(EncapsulatedPacket packet) {
		// Prevent duplicates
		if (packet.reliability.isReliable()) {
			if (reliableHandledPackets.contains(packet.messageIndex)) {
				return; // We have already handled this packet!
			}
			reliableHandledPackets.add(packet.messageIndex);
		}

		// Update split packet data
		if (packet.split == true) {
			if (!splitPackets.containsKey(packet.splitIndex)) {
				splitPackets.put(packet.splitId, new SplitPacket(packet.splitId, packet.splitCount));
			}
			SplitPacket split = splitPackets.get(packet.splitId);

			// If this is not null the packet has been fully assembled
			Packet handle = split.update(packet);
			if (handle != null) {
				this.handle(handle);
			}
		} else {
			this.handle(packet.payload);
		}
	}

	public void sendMessage(Packet packet, Reliability reliability) {
		CustomPacket custom = new CustomPacket();

		// Generate encapsulated packet
		EncapsulatedPacket encapsulated = new EncapsulatedPacket();
		encapsulated.payload = packet;
		encapsulated.reliability = reliability;
		
		// Bump message index
		if (reliability.isReliable()) {
			encapsulated.messageIndex = this.sendMessageIndex++;
		}
		
		// Set channel and bump order index
		if (reliability.isOrdered() || reliability.isSequenced()) {
			encapsulated.orderChannel = GLOBAL_CHANNEL; // Not sure why we have
														// this, TODO maybe?
			encapsulated.orderIndex = (reliability.isOrdered() ? this.orderedSendIndex[encapsulated.orderChannel]++
					: this.sequencedSendIndex[encapsulated.orderChannel]++);
		}
		

		custom.messages.add(encapsulated);

		custom.encode();
		channel.writeAndFlush(new DatagramPacket(custom.buffer(), address));
	}

	public void sendMessage(Packet[] packets, Reliability reliability) {
		CustomPacket custom = new CustomPacket();
		custom.seqNumber = sendSeqNumber++;

		custom.encode();
	}

	public abstract void handle(Packet packet);

}
