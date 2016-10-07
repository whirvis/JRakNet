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

public abstract class RakNetSession {

	public static final int DEFAULT_ORDER_CHANNEL = 0x00;

	// Session data
	private final long guid;
	private final int maximumTransferUnit;
	private RakNetState state;

	// Networking data
	private final Channel channel;
	private final InetSocketAddress address;
	private final int[] sendOrderIndex;
	private final int[] receiveOrderIndex;
	private int sendSequenceNumber;
	private int receiveSequenceNumber;

	// Network queuing
	private final ArrayList<EncapsulatedPacket> sendQueue;

	public RakNetSession(long guid, int maximumTransferUnit, Channel channel, InetSocketAddress address) {
		this.guid = guid;
		this.maximumTransferUnit = maximumTransferUnit;
		this.state = RakNetState.DISCONNECTED;
		this.channel = channel;
		this.address = address;
		this.sendOrderIndex = new int[RakNet.MAX_CHANNELS];
		this.receiveOrderIndex = new int[RakNet.MAX_CHANNELS];
		this.sendSequenceNumber = 0;
		this.receiveSequenceNumber = 0;
		this.sendQueue = new ArrayList<EncapsulatedPacket>();
	}

	public final long getGUID() {
		return this.guid;
	}

	public final InetSocketAddress getAddress() {
		return this.address;
	}

	public final void sendPacket(Reliability reliability, int channel, Packet packet) {
		EncapsulatedPacket encapsulated = new EncapsulatedPacket();
		encapsulated.reliability = reliability;
		encapsulated.orderChannel = (byte) channel;
		encapsulated.payload = packet;

		/*
		 * SYSTEM DEFINED
		 * 
		 * split messageIndex orderIndex splitCount splitId splitIndex
		 * 
		 */
	}

	public final void sendPacket(Reliability reliability, int channel, Packet... packets) {
		for (Packet packet : packets) {
			this.sendPacket(reliability, channel, packet);
		}
	}

	public final void sendPacket(Reliability reliability, Packet packet) {
		this.sendPacket(reliability, DEFAULT_ORDER_CHANNEL, packet);
	}

	public final void sendPacket(Reliability reliability, Packet... packets) {
		for (Packet packet : packets) {
			this.sendPacket(reliability, DEFAULT_ORDER_CHANNEL, packet);
		}
	}

	public final void update() {
		CustomPacket packet = new CustomPacket();
		while (packet.calculateSize() < this.maximumTransferUnit && !sendQueue.isEmpty()) {
			packet.messages.add(sendQueue.iterator().next());
		}
		if (packet.messages.size() > 0) {
			packet.seqNumber = this.sendSequenceNumber++;
			packet.encode();
			channel.writeAndFlush(new DatagramPacket(packet.buffer(), this.address));
		}
	}

	public abstract void handle(Packet packet);

	/**
	 * PROCESS OF SPLITTING PACKETS
	 * 
	 * On the update, we go through all the packets and fill up one big
	 * CustomPacket with it until it is a big as possible. When it comes time
	 * that a single EncapsulatedPacket is too big to fit in it's own
	 * EncapulatedPacket we will split it up.
	 * 
	 * Now when splitting it we need to split the actual buffer depending on the
	 * MTU and lower the maximum buffer size depending on the CustomPacket size
	 * and the EncapsulatedPacket size without the payload. Next, we add all the
	 * CustomPackets to the update queue and when the time comes they are sent
	 * out.
	 * 
	 */

}
