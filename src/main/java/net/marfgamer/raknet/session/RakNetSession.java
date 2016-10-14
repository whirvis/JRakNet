package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.SplitPacket;
import net.marfgamer.raknet.protocol.acknowledge.Acknowledge;
import net.marfgamer.raknet.protocol.acknowledge.AcknowledgeType;
import net.marfgamer.raknet.protocol.acknowledge.Record;
import net.marfgamer.raknet.protocol.message.CustomPacket;
import net.marfgamer.raknet.protocol.message.EncapsulatedPacket;
import net.marfgamer.raknet.util.ArrayUtils;
import net.marfgamer.raknet.util.map.IntMap;

public abstract class RakNetSession {

	public static final int DEFAULT_ORDER_CHANNEL = 0x00;

	// Session data
	private final long guid;
	private final int maximumTransferUnit;
	private RakNetState state;

	// Networking data
	private final Channel channel;
	private final InetSocketAddress address;

	// Ordering data
	private int splitId;
	private int messageIndex;
	private int nextSequenceNumber;
	private int lastSequenceNumber;
	private final int[] sendOrderIndex;
	private final int[] receiveOrderIndex;
	private final int[] sendSequenceIndex;
	private final int[] receiveSequenceIndex;

	// Handling data
	private final ArrayList<Integer> messageIndexQueue;
	private final IntMap<IntMap<EncapsulatedPacket>> orderedHandleQueue;

	// Network queuing
	private final IntMap<CustomPacket> ackQueue;
	private final ArrayList<EncapsulatedPacket> sendQueue;
	private final ArrayList<Record> nackQueue;
	private final IntMap<SplitPacket> splitQueue;

	public RakNetSession(long guid, int maximumTransferUnit, Channel channel, InetSocketAddress address) {
		// Session data
		this.guid = guid;
		this.maximumTransferUnit = maximumTransferUnit;
		this.state = RakNetState.DISCONNECTED;

		// Networking data
		this.channel = channel;
		this.address = address;

		// Ordering data
		this.sendOrderIndex = new int[RakNet.MAX_CHANNELS];
		this.receiveOrderIndex = new int[RakNet.MAX_CHANNELS];
		this.sendSequenceIndex = new int[RakNet.MAX_CHANNELS];
		this.receiveSequenceIndex = new int[RakNet.MAX_CHANNELS];
		for(int i = 0; i < receiveSequenceIndex.length; i++) {
			this.receiveSequenceIndex[i] = -1; // 0 is greater than -1
		}

		// Handling data
		this.messageIndexQueue = new ArrayList<Integer>();
		this.orderedHandleQueue = new IntMap<IntMap<EncapsulatedPacket>>();
		for (int i = 0; i < receiveOrderIndex.length; i++) {
			orderedHandleQueue.put(i, new IntMap<EncapsulatedPacket>());
		}

		// Networking queuing
		this.ackQueue = new IntMap<CustomPacket>();
		this.sendQueue = new ArrayList<EncapsulatedPacket>();
		this.nackQueue = new ArrayList<Record>();
		this.splitQueue = new IntMap<SplitPacket>();
	}

	public final long getGUID() {
		return this.guid;
	}

	public final InetSocketAddress getAddress() {
		return this.address;
	}

	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	public RakNetState getState() {
		return this.state;
	}

	public void setState(RakNetState state) {
		this.state = state;
	}

	public final void handleCustom0(CustomPacket custom) {
		// Generate NACK queue if needed
		int difference = custom.sequenceNumber - this.lastSequenceNumber;
		if (difference > 0) {
			if (difference > 1) {
				nackQueue.add(new Record(this.lastSequenceNumber, custom.sequenceNumber - 1));
			} else {
				nackQueue.add(new Record(custom.sequenceNumber - 1));
			}
		}
		this.lastSequenceNumber = (custom.sequenceNumber + 1);

		// Handle the messages accordingly
		for (EncapsulatedPacket encapsulated : custom.messages) {
			this.handleEncapsulated0(encapsulated);
		}
	}

	private final void handleEncapsulated0(EncapsulatedPacket encapsulated) {
		Reliability reliability = encapsulated.reliability;

		// Put together split packet
		if (encapsulated.split == true) {
			if (!splitQueue.containsKey(encapsulated.splitId)) {
				SplitPacket split = splitQueue.get(encapsulated.splitId);
				Packet finalPayload = split.update(encapsulated);
				if (finalPayload == null) {
					return; // Do not handle, the split packet is not complete!
				}

				/*
				 * It is safe to set the payload here because the old payload is
				 * no longer needed and split EncapsulatedPackets share the
				 * exact same data except for split data and payload.
				 */
				encapsulated.payload = finalPayload;
			}
		}

		// Make sure we are not handling a duplicate
		if (reliability.isReliable()) {
			if (messageIndexQueue.contains(encapsulated.messageIndex)) {
				return; // Do not handle, it is a duplicate!
			}
			messageIndexQueue.add(encapsulated.messageIndex);
		}

		// Make sure we are handling everything in an ordered/sequenced fashion
		int orderIndex = encapsulated.orderIndex;
		int orderChannel = encapsulated.orderChannel;
		if (reliability.isOrdered()) {
			orderedHandleQueue.get(orderChannel).put(orderIndex, encapsulated);
			while (orderedHandleQueue.get(orderChannel).containsKey(receiveOrderIndex[orderChannel])) {
				EncapsulatedPacket orderedEncapsulated = orderedHandleQueue.get(orderChannel)
						.get(receiveOrderIndex[orderChannel]++);
				this.handlePacket(orderedEncapsulated.payload, orderedEncapsulated.orderChannel);
			}
		} else if (reliability.isSequenced()) {
			if (orderIndex > receiveSequenceIndex[orderChannel]) {
				receiveSequenceIndex[orderChannel] = orderIndex;
				this.handlePacket(encapsulated.payload, encapsulated.orderChannel);
			}
		} else {
			this.handlePacket(encapsulated.payload, encapsulated.orderChannel);
		}
	}

	public final void handleAcknowledge(Acknowledge acknowledge) {
		// Make sure the ranged records were converted to single records
		acknowledge.simplifyRecords();

		// Handle Acknowledged based on it's type
		if (acknowledge.getType() == AcknowledgeType.ACKNOWLEDGED) {
			for (Record record : acknowledge.records) {
				// Notify API that it the receiving-side has received the packet
				for (EncapsulatedPacket encapsulated : ackQueue.get(record.getIndex()).messages) {
					if (encapsulated.reliability.requiresAck()) {
						this.onAcknowledge(record, encapsulated.reliability, encapsulated.orderChannel,
								encapsulated.payload);
					}
				}

				// The packet successfully sent, no need to store it anymore
				ackQueue.remove(record.getIndex());
			}
		} else if (acknowledge.getType() == AcknowledgeType.NOT_ACKNOWLEDGED) {
			for (Record record : acknowledge.records) {
				// Remove all unreliable packets from the queue
				CustomPacket custom = ackQueue.get(record.getIndex());
				custom.removeUnreliables();

				// Resend the modified version
				this.sendRawPacket(custom);
			}
		}
	}

	public final void sendNacknowledge() {
		Acknowledge nack = new Acknowledge(AcknowledgeType.NOT_ACKNOWLEDGED);
		nack.records = this.nackQueue;
		nack.encode();
		this.sendRawPacket(nack);
	}

	public final void sendPacket(Reliability reliability, int channel, Packet packet) {
		// Make sure channel doesn't exceed RakNet limit
		if (channel > RakNet.MAX_CHANNELS) {
			throw new IllegalArgumentException("Channel number can be no larger than " + RakNet.MAX_CHANNELS + "!");
		}

		// Make sure the EncapsulatedPacket fits inside a CustomPacket
		if (EncapsulatedPacket.calculateDummy(reliability, false, packet)
				+ CustomPacket.calculateDummy() <= this.maximumTransferUnit) {
			// Set the parameters
			EncapsulatedPacket encapsulated = new EncapsulatedPacket();
			encapsulated.reliability = reliability;
			encapsulated.orderChannel = (byte) channel;
			encapsulated.payload = packet;

			// Set reliability specific parameters
			if (reliability.isReliable()) {
				encapsulated.messageIndex = this.messageIndex++;
			}
			if (reliability.isOrdered() || reliability.isSequenced()) {
				encapsulated.orderIndex = (reliability.isOrdered() ? this.sendOrderIndex[channel]++
						: this.sendSequenceIndex[channel]++);
			}

			// Add to buffer, CustomPacket encodes it
			sendQueue.add(encapsulated);
		} else {
			byte[][] split = ArrayUtils.splitArray(packet.array(), this.maximumTransferUnit
					- CustomPacket.calculateDummy() - EncapsulatedPacket.calculateDummy(reliability, true));
			int splitMessageIndex = this.messageIndex;
			int splitOrderIndex = (reliability.isOrdered() ? this.sendOrderIndex[channel]++
					: this.sendSequenceIndex[channel]++);

			for (int i = 0; i < split.length; i++) {
				// Set the normal parameters
				EncapsulatedPacket encapsulatedSplit = new EncapsulatedPacket();
				encapsulatedSplit.reliability = reliability;
				encapsulatedSplit.orderChannel = (byte) channel;
				encapsulatedSplit.payload = new Packet(Unpooled.copiedBuffer(split[i]));

				// Set reliability specific parameters
				if (reliability.isReliable()) {
					encapsulatedSplit.messageIndex = splitMessageIndex;
				}
				if (reliability.isOrdered() || reliability.isSequenced()) {
					encapsulatedSplit.orderIndex = splitOrderIndex;
				}

				// Set the split related parameters
				encapsulatedSplit.split = true;
				encapsulatedSplit.splitCount = split.length;
				encapsulatedSplit.splitId = this.splitId++;
				encapsulatedSplit.splitIndex = i;

				// Add to buffer, CustomPacket encodes it
				sendQueue.add(encapsulatedSplit);
			}
		}
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

	public final void sendRawPacket(Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	public final void update() {
		if (sendQueue.isEmpty() == false) {
			CustomPacket custom = new CustomPacket();

			// Add packets to the CustomPacket until it's full or there's none
			ArrayList<EncapsulatedPacket> sent = new ArrayList<EncapsulatedPacket>();
			for (EncapsulatedPacket encapsulated : this.sendQueue) {
				if (custom.calculateSize() + encapsulated.calculateSize() > this.maximumTransferUnit) {
					break; // The packet is full, break out of the loop!
				}
				sent.add(encapsulated);
				custom.messages.add(encapsulated);
			}
			sendQueue.removeAll(sent); // We no longer need these, remove them

			// Only send if we have something to send
			if (custom.messages.size() > 0) {
				custom.sequenceNumber = this.nextSequenceNumber++;
				custom.encode();
				this.sendRawPacket(custom);
				ackQueue.put(custom.sequenceNumber, custom);
			}
		}
	}

	public abstract void onAcknowledge(Record record, Reliability reliability, int channel, Packet packet);

	public abstract void handlePacket(Packet packet, int channel);

}
