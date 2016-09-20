package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.CustomPacket;
import net.marfgamer.raknet.protocol.EncapsulatedPacket;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.SplitPacket;
import net.marfgamer.raknet.protocol.acknowledge.Acknowledge;
import net.marfgamer.raknet.util.ArrayUtils;
import net.marfgamer.raknet.util.map.IntMap;

public class RakNetSession {

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
			for(int missingPacket : missing) {
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

	}

	public void sendMessage(Packet packet, Reliability reliability, int channel) {

	}

	public void sendMessage(Packet packet, Reliability reliability) {
		this.sendMessage(packet, reliability, 0);
	}

	public void sendMessage(Packet[] packets, Reliability reliability, int channel) {
		CustomPacket custom = new CustomPacket();
		custom.seqNumber = sendSeqNumber++;

		custom.encode();
	}

	public void sendMessage(Packet[] packets, Reliability reliability) {
		this.sendMessage(packets, reliability, 0);
	}
	
}
