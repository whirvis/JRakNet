/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 MarfGamer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 */
package net.marfgamer.raknet.session;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.exception.session.InvalidChannelException;
import net.marfgamer.raknet.exception.session.SplitQueueOverloadException;
import net.marfgamer.raknet.exception.session.TimeoutException;
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

/**
 * This class is used to easily manage connections in RakNet
 *
 * @author MarfGamer
 */
public abstract class RakNetSession implements UnumRakNetPeer, GeminusRakNetPeer {

	public static final long MAX_PACKETS_PER_SECOND_BLOCK = 1000L * 300L;
	public static final long RECOVERY_SEND_WAIT_TIME_MILLIS = 50L;
	public static final long ACK_SEND_WAIT_TIME_MILLIS = 3000L;
	public static final long PING_SEND_WAIT_TIME_MILLIS = 3000L;
	public static final long DETECTION_SEND_WAIT_TIME_MILLIS = 5000L;
	public static final long SESSION_TIMEOUT = PING_SEND_WAIT_TIME_MILLIS * 5L;

	// Session data
	private final long guid;
	private final int maximumTransferUnit;
	private RakNetState state;
	private int packetsSentThisSecond;
	private int packetsReceivedThisSecond;
	private long packetSentReceiveCounter;
	private long lastPacketReceiveTime;
	private long latency;
	private long lastLatency;
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
	private long lastRecoverySend;
	private long lastAckSend;

	// Latency detection
	private boolean latencyEnabled;
	private long lastPingSend;
	private long pongsReceived;
	private long pongsTotalLatency;
	private long pingIdentifier;

	// Connection loss prevention
	private long lastDetectionSend;

	public RakNetSession(long guid, int maximumTransferUnit, Channel channel, InetSocketAddress address) {
		// Session data
		this.guid = guid;
		this.maximumTransferUnit = maximumTransferUnit;
		this.state = RakNetState.DISCONNECTED;
		this.packetSentReceiveCounter = System.currentTimeMillis();
		this.lastPacketReceiveTime = System.currentTimeMillis();

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

		// Network queuing
		this.recoveryQueue = new IntMap<CustomPacket>();
		this.acknowledgeQueue = new ArrayList<Record>();
		this.nacknowledgeQueue = new ArrayList<Record>();
		this.requireAcknowledgeQueue = new IntMap<EncapsulatedPacket[]>();
		this.splitQueue = new IntMap<SplitPacket>();
		this.sendQueue = new ArrayList<EncapsulatedPacket>();
		this.lastAckSend = System.currentTimeMillis();

		// Latency detection
		this.latencyEnabled = true;
		this.latency = -1; // We can't predict them
		this.lastLatency = -1;
		this.lowestLatency = -1;
		this.highestLatency = -1;

		// Connection loss prevention
		this.lastDetectionSend = System.currentTimeMillis();
	}

	/**
	 * Returns the session's globally unique ID (GUID)
	 * 
	 * @return The session's globally unique ID
	 */
	public final long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * Returns the session's address
	 * 
	 * @return The session's address
	 */
	public final InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * Returns the session's maximum transfer unit
	 * 
	 * @return The session's maximum transfer unit
	 */
	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Returns the session's current state
	 * 
	 * @return The session's current state
	 */
	public RakNetState getState() {
		return this.state;
	}

	/**
	 * Sets the session's current state
	 * 
	 * @param state
	 *            The new state
	 */
	public void setState(RakNetState state) {
		this.state = state;
	}

	/**
	 * Returns the amount of packets sent this second
	 * 
	 * @return The amount of packets sent this second
	 */
	public int getPacketsSentThisSecond() {
		return this.packetsSentThisSecond;
	}

	/**
	 * Returns the amount of packet received this second
	 * 
	 * @return The amount of packets received this second
	 */
	public int getPacketsReceivedThisSecond() {
		return this.packetsReceivedThisSecond;
	}

	/**
	 * Returns the last time a packet(CustomPacket, Acknowledgement, or
	 * AcknowledgementReceipt) was received by the session
	 * 
	 * @return The last time a packet was received by the session
	 */
	public long getLastPacketReceiveTime() {
		return this.lastPacketReceiveTime;
	}

	/**
	 * Enables/disables latency detection, when disabled the latency will always
	 * return -1
	 * 
	 * @param enabled
	 *            Whether or not latency detection is enabled
	 */
	public void enableLatencyDetection(boolean enabled) {
		this.latencyEnabled = enabled;
		this.latency = (enabled ? this.latency : -1);
	}

	/**
	 * Returns whether or not latency detection is enabled
	 * 
	 * @return Whether or not latency detection is enabled
	 */
	public boolean latencyDetectionEnabled() {
		return this.latencyEnabled;
	}

	/**
	 * Returns the average latency for the session
	 * 
	 * @return The average latency for the session
	 */
	public long getLatency() {
		return this.latency;
	}

	/**
	 * Returns the last latency for the session
	 * 
	 * @return The last latency for the session
	 */
	public long getLastLatency() {
		return this.lastLatency;
	}

	/**
	 * Returns the lowest recorded latency for the session
	 * 
	 * @return The lowest recorded latency for the session
	 */
	public long getLowestLatency() {
		return this.lowestLatency;
	}

	/**
	 * Returns the highest recorded latency for the session
	 * 
	 * @return The highest recorded latency for the session
	 */
	public long getHighestLatency() {
		return this.highestLatency;
	}

	/**
	 * Handles a CustomPacket and updates the session's data accordingly
	 * 
	 * @param custom
	 *            The CustomPacket to handle
	 */
	public final void handleCustom0(CustomPacket custom) {
		this.lastPacketReceiveTime = System.currentTimeMillis();

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

		// Make sure we don't set an old packet to the new one
		if (difference >= 0) {
			this.lastSequenceNumber = (custom.sequenceNumber + 1);
		}

		// Update acknowledgement queues
		acknowledgeQueue.add(new Record(custom.sequenceNumber));
		this.updateAcknowledge(true);

		// Handle the messages accordingly
		this.packetsReceivedThisSecond++;
		for (EncapsulatedPacket encapsulated : custom.messages) {
			this.handleEncapsulated0(encapsulated);
		}
	}

	/**
	 * Handles an EncapsulatedPacket retrieved from a CustomPacket and updates
	 * the session's data accordingly
	 * 
	 * @param encapsulated
	 *            The EncapsulatedPacket to handle
	 * @throws SplitQueueOverloadException
	 *             Thrown if the split queue has been overloaded
	 * @throws InvalidChannelException
	 *             Thrown if the channel is higher than the maximum
	 */
	private final void handleEncapsulated0(EncapsulatedPacket encapsulated)
			throws SplitQueueOverloadException, InvalidChannelException {
		Reliability reliability = encapsulated.reliability;

		// Put together split packet
		if (encapsulated.split == true) {
			if (!splitQueue.containsKey(encapsulated.splitId)) {
				splitQueue.put(encapsulated.splitId,
						new SplitPacket(encapsulated.splitId, encapsulated.splitCount, encapsulated.reliability));
				if (splitQueue.size() > RakNet.MAX_SPLITS_PER_QUEUE) {
					throw new SplitQueueOverloadException();
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
		if (orderChannel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		} else {
			// Channel is valid, it is safe to handle
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
	}

	/**
	 * This method is method is called before
	 * <code>handlePacket(RakNetPacket, int)</code> so we can handle packets
	 * that are sent by the server and the client
	 * 
	 * @param packet
	 *            The packet to handle
	 * @param channel
	 *            The channel the packet was sent on
	 */
	private final void handlePacket0(RakNetPacket packet, int channel) {
		int packetId = packet.getId();

		if (packetId == ID_CONNECTED_PING) {
			ConnectedPing ping = new ConnectedPing(packet);
			ping.decode();

			ConnectedPong pong = new ConnectedPong();
			pong.identifier = ping.identifier;
			pong.encode();
			this.sendMessage(Reliability.UNRELIABLE, pong);
		} else if (packetId == ID_CONNECTED_PONG) {
			ConnectedPong pong = new ConnectedPong(packet);
			pong.decode();

			if (latencyEnabled == true) {
				if (this.pingIdentifier - pong.identifier == 1) {
					long latencyRaw = (this.lastPacketReceiveTime - this.lastPingSend);

					// Get last latency result
					this.lastLatency = latencyRaw;

					// Get lowest and highest latency
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

					// Get average latency
					pongsReceived++;
					pongsTotalLatency += latencyRaw;
					this.latency = (pongsTotalLatency / pongsReceived);
				}
			}

			this.pingIdentifier = pong.identifier + 1;
		} else {
			this.handlePacket(packet, channel);
		}
	}

	/**
	 * Handles an Acknowledge packet and updates the session's data accordingly
	 * 
	 * @param acknowledge
	 *            The acknowledge packet to handle
	 */
	public final void handleAcknowledge(Acknowledge acknowledge) {
		this.lastPacketReceiveTime = System.currentTimeMillis();

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
				// Resend the lost packets
				CustomPacket custom = recoveryQueue.get(record.getIndex());
				this.sendRawMessage(custom);
			}
		}
	}

	/**
	 * Handles an acknowledge receipt and notifies the API accordingly
	 * 
	 * @param acknowledgeReceipt
	 *            The acknowledge receipt to handle
	 */
	public final void handleAcknowledgeReceipt(AcknowledgeReceipt acknowledgeReceipt) {
		this.lastPacketReceiveTime = System.currentTimeMillis();
		for (EncapsulatedPacket encapsulated : requireAcknowledgeQueue.get(acknowledgeReceipt.record)) {
			// Do we have this packet registered?
			if (encapsulated == null) {
				continue; // We never sent a packet requiring an acknowledge
							// receipt with this ID!
			}

			// Was the packet received or was it lost?
			if (acknowledgeReceipt.getType() == AcknowledgeReceiptType.ACKNOWLEDGED) {
				this.onAcknowledge(new Record(acknowledgeReceipt.record), encapsulated.reliability,
						encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
			} else if (acknowledgeReceipt.getType() == AcknowledgeReceiptType.LOSS) {
				this.onNotAcknowledge(new Record(acknowledgeReceipt.record), encapsulated.reliability,
						encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
			}
		}
	}

	@Override
	public final synchronized void sendMessage(Reliability reliability, int channel, Packet packet)
			throws InvalidChannelException {
		// Make sure channel doesn't exceed RakNet limit
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
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
			// Get split packet data
			byte[][] split = ArrayUtils.splitArray(packet.array(), this.maximumTransferUnit
					- CustomPacket.calculateDummy() - EncapsulatedPacket.calculateDummy(reliability, true));
			int splitMessageIndex = this.messageIndex;
			int splitOrderIndex = (reliability.isOrdered() ? this.sendOrderIndex[channel]++
					: this.sendSequenceIndex[channel]++);

			// Encode encapsulated packets
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
				encapsulatedSplit.splitId = this.splitId;
				encapsulatedSplit.splitIndex = i;

				// Add to buffer, CustomPacket encodes it
				sendQueue.add(encapsulatedSplit);
			}

			// Bump split ID for next split packet
			this.splitId++;
		}
	}

	@Override
	public void sendMessage(long guid, Reliability reliability, int channel, Packet packet) {
		if (this.guid == guid) {
			this.sendMessage(reliability, channel, packet);
		}
	}

	/**
	 * Sends a raw message
	 * 
	 * @param packet
	 *            The packet to send
	 */
	public final void sendRawMessage(Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	/**
	 * Updates acknowledgement data by resending unacknowledged packets along
	 * with sending ACK and NACK packets
	 * 
	 * @param forceSend
	 *            Determines whether or not the functions will be done
	 *            immediately or be handled based on the timer system
	 */
	private final void updateAcknowledge(boolean forceSend) {
		long currentTime = System.currentTimeMillis();

		// Resend unacknowledged packets
		if (currentTime - this.lastRecoverySend >= RECOVERY_SEND_WAIT_TIME_MILLIS && !recoveryQueue.isEmpty()) {
			for (CustomPacket custom : recoveryQueue.values()) {
				this.sendRawMessage(custom);
				break; // Only send one at a time
			}
			this.lastRecoverySend = currentTime;
		}

		// Check for missing packets
		if (currentTime - lastAckSend >= ACK_SEND_WAIT_TIME_MILLIS || forceSend == true) {
			// Have we not acknowledge some packets?
			if (acknowledgeQueue.isEmpty() == false) {
				Acknowledge ack = new Acknowledge(AcknowledgeType.ACKNOWLEDGED);
				ack.records = this.acknowledgeQueue;
				ack.encode();
				this.sendRawMessage(ack);

				acknowledgeQueue.clear(); // No longer needed
			}

			// Are we missing any packets?
			if (nacknowledgeQueue.isEmpty() == false) {
				Acknowledge nack = new Acknowledge(AcknowledgeType.NOT_ACKNOWLEDGED);
				nack.records = this.nacknowledgeQueue;
				nack.encode();
				this.sendRawMessage(nack);
			}

			// Update timing
			this.lastAckSend = currentTime;
		}
	}

	/**
	 * Updates the session
	 * 
	 * @throws RakNetException
	 *             Thrown if an error occurs during the updating process
	 */
	public final synchronized void update() throws RakNetException {
		long currentTime = System.currentTimeMillis();

		// Are we missing any packets?
		this.updateAcknowledge(false);

		// Are there any packets to send?
		if (sendQueue.isEmpty() == false) {
			CustomPacket custom = new CustomPacket();

			// Add packets to the CustomPacket until it's full or there's none
			ArrayList<EncapsulatedPacket> sent = new ArrayList<EncapsulatedPacket>();
			for (EncapsulatedPacket encapsulated : this.sendQueue) {
				if (custom.calculateSize() + encapsulated.calculateSize() > this.maximumTransferUnit) {
					if (sent.isEmpty()) {
						throw new RakNetException(
								"Payload is too big to fit in a single packet! Was it split correctly?");
					}
					break; // If we add this next packet we'll exceed the MTU!
				}
				sent.add(encapsulated);
				custom.messages.add(encapsulated);
			}
			sendQueue.removeAll(sent); // We no longer need these, remove them

			// Only send if we have something to send
			if (custom.messages.size() > 0) {
				custom.sequenceNumber = this.nextSequenceNumber++;
				custom.encode();

				// Let all unacknowledged packets be handled first and don't DOS
				if (recoveryQueue.isEmpty() == true && this.packetsSentThisSecond < RakNet.MAX_PACKETS_PER_SECOND) {
					this.sendRawMessage(custom);
				}
				custom.removeUnreliables();
				recoveryQueue.put(custom.sequenceNumber, custom);
			}
		}

		// Reset the amount of packets sent and received in this second
		if (currentTime - packetSentReceiveCounter >= RakNet.MAX_PACKETS_PER_SECOND) {
			this.packetsSentThisSecond = 0;
			this.packetsReceivedThisSecond = 0;
			this.packetSentReceiveCounter = currentTime;
		}

		// Send a CONNECTED_PING to get the latency
		if (currentTime - lastPingSend >= PING_SEND_WAIT_TIME_MILLIS && latencyEnabled == true) {
			ConnectedPing ping = new ConnectedPing();
			ping.identifier = this.pingIdentifier++;
			ping.encode();
			this.sendMessage(Reliability.UNRELIABLE, ping);
			this.lastPingSend = currentTime;
		}

		// Send a DETECT_LOST_CONNECTIONS to make sure the client is still there
		if (currentTime - lastDetectionSend >= DETECTION_SEND_WAIT_TIME_MILLIS
				&& currentTime - this.lastPacketReceiveTime >= DETECTION_SEND_WAIT_TIME_MILLIS) {
			this.sendMessage(Reliability.RELIABLE, ID_DETECT_LOST_CONNECTIONS);
			this.lastDetectionSend = currentTime;
		}

		// The client timed out
		if (currentTime - lastPacketReceiveTime >= SESSION_TIMEOUT) {
			throw new TimeoutException();
		}
	}

	/**
	 * This function is called when a acknowledge receipt is received for the
	 * packet
	 * 
	 * @param record
	 *            The record of the packet
	 * @param reliability
	 *            The reliability of the packet
	 * @param channel
	 *            The channel of the packet
	 * @param packet
	 *            The acknowledged packet
	 */
	public abstract void onAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet);

	/**
	 * This function is called when a not acknowledged receipt is received for
	 * the packet
	 * 
	 * @param record
	 *            The record of the packet
	 * @param reliability
	 *            The reliability of the packet
	 * @param channel
	 *            The channel of the packet
	 * @param packet
	 *            The not acknowledged packet
	 */
	public abstract void onNotAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet);

	/**
	 * This function is called when a packet is received by the session
	 * 
	 * @param packet
	 *            The packet to handle
	 * @param channel
	 *            The packet the channel was sent on
	 */
	public abstract void handlePacket(RakNetPacket packet, int channel);

}
