package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.SplitPacket;
import net.marfgamer.raknet.protocol.message.CustomPacket;
import net.marfgamer.raknet.protocol.message.EncapsulatedPacket;
import net.marfgamer.raknet.protocol.message.acknowledge.Acknowledge;
import net.marfgamer.raknet.protocol.message.acknowledge.AcknowledgeReceipt;
import net.marfgamer.raknet.protocol.message.acknowledge.AcknowledgeReceiptType;
import net.marfgamer.raknet.protocol.message.acknowledge.AcknowledgeType;
import net.marfgamer.raknet.protocol.message.acknowledge.Record;
import net.marfgamer.raknet.protocol.status.ConnectedPing;
import net.marfgamer.raknet.protocol.status.ConnectedPong;
import net.marfgamer.raknet.util.ArrayUtils;
import net.marfgamer.raknet.util.map.IntMap;

public abstract class RakNetSession implements Reliability.INTERFACE {

	public static final int DEFAULT_ORDER_CHANNEL = 0x00;
	public static final int ACK_SEND_WAIT_TIME_MILLIS = 3000;
	public static final int PING_SEND_WAIT_TIME_MILLIS = 3000;
	public static final int SESSION_TIMEOUT = PING_SEND_WAIT_TIME_MILLIS * 5;

	// Session data
	private final long guid;
	private final int maximumTransferUnit;
	private RakNetState state;
	private long lastPacketReceiveTime;
	private long latency;
	private long lowestLatency;
	private long highestLatency;

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
	private final ArrayList<Integer> customIndexQueue;
	private final ArrayList<Integer> messageIndexQueue;
	private final IntMap<IntMap<EncapsulatedPacket>> orderedHandleQueue;

	// Network queuing
	private final IntMap<CustomPacket> recoveryQueue;
	private final ArrayList<Record> acknowledgeQueue;
	private final ArrayList<Record> nacknowledgeQueue;
	private final IntMap<EncapsulatedPacket[]> requireAcknowledgeQueue;
	private final IntMap<SplitPacket> splitQueue;
	private final ArrayList<EncapsulatedPacket> sendQueue;
	private long lastAckSend;

	// Latency detection
	private long lastPingSend;
	private long pongsReceived;
	private long pongsTotalLatency;
	private long pingIdentifier;

	public RakNetSession(long guid, int maximumTransferUnit, Channel channel, InetSocketAddress address) {
		// Session data
		this.guid = guid;
		this.maximumTransferUnit = maximumTransferUnit;
		this.state = RakNetState.DISCONNECTED;
		this.lastPacketReceiveTime = System.currentTimeMillis();
		this.latency = -1; // We can't predict them

		// Networking data
		this.channel = channel;
		this.address = address;

		// Ordering data
		this.sendOrderIndex = new int[RakNet.MAX_CHANNELS];
		this.receiveOrderIndex = new int[RakNet.MAX_CHANNELS];
		this.sendSequenceIndex = new int[RakNet.MAX_CHANNELS];
		this.receiveSequenceIndex = new int[RakNet.MAX_CHANNELS];

		// Handling data
		this.customIndexQueue = new ArrayList<Integer>();
		this.messageIndexQueue = new ArrayList<Integer>();
		this.orderedHandleQueue = new IntMap<IntMap<EncapsulatedPacket>>();
		for (int i = 0; i < receiveOrderIndex.length; i++) {
			orderedHandleQueue.put(i, new IntMap<EncapsulatedPacket>());
		}

		// Networking queuing
		this.recoveryQueue = new IntMap<CustomPacket>();
		this.acknowledgeQueue = new ArrayList<Record>();
		this.nacknowledgeQueue = new ArrayList<Record>();
		this.requireAcknowledgeQueue = new IntMap<EncapsulatedPacket[]>();
		this.splitQueue = new IntMap<SplitPacket>();
		this.sendQueue = new ArrayList<EncapsulatedPacket>();
		this.lastAckSend = System.currentTimeMillis();
		this.lastPingSend = System.currentTimeMillis();
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

	public long getLastPacketReceiveTime() {
		return this.lastPacketReceiveTime;
	}

	public void bumpLastPacketReceiveTime() {
		this.lastPacketReceiveTime = System.currentTimeMillis();
	}

	public long getLatency() {
		return this.latency;
	}

	public long getLowestLatency() {
		return this.lowestLatency;
	}

	public long getHighestLatency() {
		return this.highestLatency;
	}

	public final void handleCustom0(CustomPacket custom) throws Exception {
		// Only handle if we haven't handled it before
		if (customIndexQueue.contains(custom.sequenceNumber)) {
			return; // We have handle it before!
		}
		customIndexQueue.add(custom.sequenceNumber);

		// Generate NACK queue if needed
		int difference = custom.sequenceNumber - this.lastSequenceNumber;
		synchronized (this.nacknowledgeQueue) {
			Record record = new Record(custom.sequenceNumber);
			if (nacknowledgeQueue.contains(record)) {
				nacknowledgeQueue.remove(record);
			}

			if (difference > 0) {
				if (difference > 1) {
					nacknowledgeQueue.add(new Record(this.lastSequenceNumber, custom.sequenceNumber - 1));
				} else {
					nacknowledgeQueue.add(new Record(custom.sequenceNumber - 1));
				}
			}
		}

		// Make sure we don't set an old packet to the knew one
		if (difference >= 0) {
			this.lastSequenceNumber = (custom.sequenceNumber + 1);
		}

		// Update acknowledgement queues
		acknowledgeQueue.add(new Record(custom.sequenceNumber));
		this.updateAcknowledge(true);

		// Handle the messages accordingly
		for (EncapsulatedPacket encapsulated : custom.messages) {
			this.handleEncapsulated0(encapsulated);
		}
	}

	private final void handleEncapsulated0(EncapsulatedPacket encapsulated) throws Exception {
		Reliability reliability = encapsulated.reliability;

		// Put together split packet
		if (encapsulated.split == true) {
			if (!splitQueue.containsKey(encapsulated.splitId)) {
				splitQueue.put(encapsulated.splitId,
						new SplitPacket(encapsulated.splitId, encapsulated.splitCount, encapsulated.reliability));
				if (splitQueue.size() > RakNet.MAX_SPLITS_PER_QUEUE) {
					throw new IllegalArgumentException("Too many split packets in the queue!");
				}
			}

			SplitPacket split = splitQueue.get(encapsulated.splitId);
			Packet finalPayload = split.update(encapsulated);
			if (finalPayload == null) {
				return; // Do not handle, the split packet is not complete!
			}

			/*
			 * It is safe to set the payload here because the old payload is no
			 * longer needed and split EncapsulatedPackets share the exact same
			 * data except for split data and payload.
			 */
			encapsulated.payload = finalPayload;
			splitQueue.remove(encapsulated.splitId);
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
				this.handlePacket0(new RakNetPacket(orderedEncapsulated.payload), orderedEncapsulated.orderChannel);
			}
		} else if (reliability.isSequenced()) {
			if (orderIndex > receiveSequenceIndex[orderChannel]) {
				receiveSequenceIndex[orderChannel] = orderIndex + 1;
				this.handlePacket0(new RakNetPacket(encapsulated.payload), encapsulated.orderChannel);
			}
		} else {
			this.handlePacket0(new RakNetPacket(encapsulated.payload), encapsulated.orderChannel);
		}
	}

	/**
	 * This method is called to see if the packet is meant for the server and
	 * the client before passing it on to a specific type
	 * 
	 * @param packet
	 * @param channel
	 * @throws Exception
	 */
	private final void handlePacket0(RakNetPacket packet, int channel) throws Exception {
		int id = packet.getId();

		if (id == MessageIdentifier.ID_CONNECTED_PING) {
			ConnectedPing ping = new ConnectedPing(packet);
			ping.decode();

			ConnectedPong pong = new ConnectedPong();
			pong.identifier = ping.identifier;
			pong.encode();
			this.sendPacket(UNRELIABLE, pong);
		} else if (id == MessageIdentifier.ID_CONNECTED_PONG) {
			ConnectedPong pong = new ConnectedPong(packet);
			pong.decode();

			if (this.pingIdentifier - pong.identifier == 1) {
				long latencyRaw = (this.lastPacketReceiveTime - this.lastPingSend);

				// Get lowest and highest latency for users who like that more
				if (this.pongsReceived == 0) {
					this.lowestLatency = latencyRaw;
					this.highestLatency = latencyRaw;
				} else {
					if (latencyRaw < lowestLatency) {
						this.lowestLatency = latencyRaw;
					} else if (latencyRaw > highestLatency) {
						this.highestLatency = latencyRaw;
					}
				}

				// Get average latency to provide more stable results
				pongsReceived++;
				pongsTotalLatency += latencyRaw;
				this.latency = (pongsTotalLatency / pongsReceived);
			}
		} else {
			this.handlePacket(packet, channel);
		}
	}

	public final void handleAcknowledge(Acknowledge acknowledge) throws Exception {
		// Make sure the ranged records were converted to single records
		acknowledge.simplifyRecords();

		// Handle Acknowledged based on it's type
		if (acknowledge.getType() == AcknowledgeType.ACKNOWLEDGED) {
			for (Record record : acknowledge.records) {
				// The packet successfully sent, no need to store it anymore
				recoveryQueue.remove(record.getIndex());
			}
		} else if (acknowledge.getType() == AcknowledgeType.NOT_ACKNOWLEDGED) {
			for (Record record : acknowledge.records) {
				// Remove all unreliable packets from the queue
				CustomPacket custom = recoveryQueue.get(record.getIndex());

				// We already sent it, we will need to send a fake
				if (custom == null) {
					custom = new CustomPacket();
					custom.sequenceNumber = record.getIndex();
				} else {
					custom.removeUnreliables();
				}

				// Resend the modified version
				this.sendRawPacket(custom);
			}
		}
	}

	public final void handleAcknowledgeReceipt(AcknowledgeReceipt acknowledgeReceipt) throws Exception {
		if (acknowledgeReceipt.getType() == AcknowledgeReceiptType.ACKNOWLEDGED) {
			for (EncapsulatedPacket encapsulated : requireAcknowledgeQueue.get(acknowledgeReceipt.record)) {
				this.onAcknowledge(new Record(acknowledgeReceipt.record), encapsulated.reliability,
						encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
			}
		}
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

	private final void updateAcknowledge(boolean forceSend) throws Exception {
		long currentTime = System.currentTimeMillis();

		// Check for missing packets
		if (currentTime - lastAckSend >= ACK_SEND_WAIT_TIME_MILLIS || forceSend == true) {
			// Have we not acknowledge some packets?
			if (acknowledgeQueue.isEmpty() == false) {
				Acknowledge ack = new Acknowledge(AcknowledgeType.ACKNOWLEDGED);
				ack.records = this.acknowledgeQueue;
				ack.encode();
				this.sendRawPacket(ack);

				acknowledgeQueue.clear(); // No longer needed
			}

			// Are we missing any packets?
			if (nacknowledgeQueue.isEmpty() == false) {
				Acknowledge nack = new Acknowledge(AcknowledgeType.NOT_ACKNOWLEDGED);
				nack.records = this.nacknowledgeQueue;
				nack.encode();
				this.sendRawPacket(nack);
			}

			// Only do this naturally
			if (forceSend == false) {
				for (CustomPacket custom : recoveryQueue.values()) {
					this.sendRawPacket(custom);
					break; // Only send one at a time
				}
			}

			// Update timing
			this.lastAckSend = currentTime;
		}
	}

	public final void update() throws Exception {
		long currentTime = System.currentTimeMillis();

		// Are we missing any packets?
		this.updateAcknowledge(false);

		// Are there any packets to send?
		if (sendQueue.isEmpty() == false) {
			CustomPacket custom = new CustomPacket();

			// Add packets to the CustomPacket until it's full or there's none
			ArrayList<EncapsulatedPacket> sent = new ArrayList<EncapsulatedPacket>();
			for (EncapsulatedPacket encapsulated : this.sendQueue) {
				if (custom.calculateSize() + encapsulated.calculateSize() >= this.maximumTransferUnit) {
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

				// Let all unacknowledged packets be handled first
				if (recoveryQueue.isEmpty() == true) {
					this.sendRawPacket(custom);
				}
				recoveryQueue.put(custom.sequenceNumber, custom);
			}
		}

		// Send a ping to try and wake up the receiving side
		if (currentTime - lastPingSend >= PING_SEND_WAIT_TIME_MILLIS
				&& currentTime - this.lastPacketReceiveTime >= PING_SEND_WAIT_TIME_MILLIS) {
			ConnectedPing ping = new ConnectedPing();
			ping.identifier = this.pingIdentifier++;
			ping.encode();
			this.sendPacket(UNRELIABLE, ping);
			this.lastPingSend = currentTime;
		}

		// The client timed out
		if (currentTime - lastPacketReceiveTime >= SESSION_TIMEOUT) {
			this.closeConnection("Timeout");
		}

	}

	public abstract void onAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet)
			throws Exception;

	public abstract void handlePacket(RakNetPacket packet, int channel) throws Exception;

	public abstract void closeConnection(String reason);

}
