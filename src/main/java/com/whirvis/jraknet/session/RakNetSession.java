/*
 *       _   _____            _      _   _          _
 *      | | |  __ \          | |    | \ | |        | |
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
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
package com.whirvis.jraknet.session;

import static com.whirvis.jraknet.protocol.MessageIdentifier.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.map.concurrent.ConcurrentIntMap;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.MessageIdentifier;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.message.CustomPacket;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Acknowledge;
import com.whirvis.jraknet.protocol.message.acknowledge.AcknowledgeType;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;
import com.whirvis.jraknet.protocol.status.ConnectedPing;
import com.whirvis.jraknet.protocol.status.ConnectedPong;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

/**
 * This class is used to easily manage connections in RakNet.
 *
 * @author Trent "Whirvis" Summerlin
 */
public abstract class RakNetSession implements UnumRakNetPeer, GeminusRakNetPeer {

	private static final Logger log = LoggerFactory.getLogger(RakNetSession.class);

	// Session data
	private String loggerName;
	private final ConnectionType connectionType;
	private final long guid;
	private final int maximumTransferUnit;
	private final Channel channel;
	private final InetSocketAddress address;
	private RakNetState state;

	// Timing
	private int packetsSentThisSecond;
	private int packetsReceivedThisSecond;
	private long lastPacketCounterResetTime;
	private long lastPacketSendTime;
	private long lastPacketReceiveTime;
	private long lastRecoverySendTime;
	private long lastKeepAliveSendTime;
	private long lastPingSendTime;

	// Packet data
	private int messageIndex;
	private int splitId;
	private final ConcurrentLinkedQueue<Integer> reliablePackets;
	private final ConcurrentIntMap<SplitPacket> splitQueue;
	/**
	 * Cleared by <code>RakNetServerSession</code> to help make sure the
	 * <code>ID_DISCONNECTION_NOTIFICATION</code> packet is sent out
	 */
	protected final ConcurrentLinkedQueue<EncapsulatedPacket> sendQueue;
	private final ConcurrentIntMap<EncapsulatedPacket[]> recoveryQueue;
	private final ConcurrentHashMap<EncapsulatedPacket, Integer> ackReceiptPackets;

	// Ordering and sequencing
	private int sendSequenceNumber;
	private int receiveSequenceNumber;
	private final int[] orderSendIndex;
	private final int[] orderReceiveIndex;
	private final int[] sequenceSendIndex;
	private final int[] sequenceReceiveIndex;
	private final ConcurrentIntMap<ConcurrentIntMap<EncapsulatedPacket>> handleQueue;

	// Latency detection
	private boolean latencyEnabled;
	private int pongsReceived;
	private long totalLatency;
	private long latency;
	private long lastLatency;
	private long lowestLatency;
	private long highestLatency;
	private final ArrayList<Long> latencyTimestamps;

	/**
	 * Constructs a <code>RakNetSession</code> with the specified globally
	 * unique ID, maximum transfer unit, <code>Channel</code>, and address.
	 * 
	 * @param connectionType
	 *            the connection type of the session.
	 * @param guid
	 *            the globally unique ID.
	 * @param maximumTransferUnit
	 *            the maximum transfer unit.
	 * @param channel
	 *            the <code>Channel</code>.
	 * @param address
	 *            the address.
	 */
	public RakNetSession(ConnectionType connectionType, long guid, int maximumTransferUnit, Channel channel,
			InetSocketAddress address) {
		// Session data
		this.loggerName = "(session #" + Long.toHexString(guid).toUpperCase() + ") ";
		this.connectionType = connectionType;
		this.guid = guid;
		this.maximumTransferUnit = maximumTransferUnit;
		this.channel = channel;
		this.address = address;
		this.state = RakNetState.DISCONNECTED;

		// Timing
		this.lastPacketReceiveTime = System.currentTimeMillis();

		// Packet data
		this.reliablePackets = new ConcurrentLinkedQueue<Integer>();
		this.splitQueue = new ConcurrentIntMap<SplitPacket>();
		this.sendQueue = new ConcurrentLinkedQueue<EncapsulatedPacket>();
		this.recoveryQueue = new ConcurrentIntMap<EncapsulatedPacket[]>();
		this.ackReceiptPackets = new ConcurrentHashMap<EncapsulatedPacket, Integer>();

		// Ordering and sequencing
		this.orderSendIndex = new int[RakNet.MAX_CHANNELS];
		this.orderReceiveIndex = new int[RakNet.MAX_CHANNELS];
		this.sequenceSendIndex = new int[RakNet.MAX_CHANNELS];
		this.sequenceReceiveIndex = new int[RakNet.MAX_CHANNELS];
		this.handleQueue = new ConcurrentIntMap<ConcurrentIntMap<EncapsulatedPacket>>();
		for (int i = 0; i < RakNet.MAX_CHANNELS; i++) {
			sequenceReceiveIndex[i] = -1;
			handleQueue.put(i, new ConcurrentIntMap<EncapsulatedPacket>());
		}

		// Latency detection
		this.latencyEnabled = true;
		this.latency = -1; // We can't predict a player's latency
		this.lastLatency = -1;
		this.lowestLatency = -1;
		this.highestLatency = -1;
		this.latencyTimestamps = new ArrayList<Long>();
	}

	/**
	 * @return the connection type of the session.
	 */
	public final ConnectionType getConnectionType() {
		return this.connectionType;
	}

	/**
	 * @return the session's globally unique ID.
	 */
	public final long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * @return the session's timestamp.
	 */
	public abstract long getTimestamp();

	/**
	 * @return the session's address.
	 */
	public final InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * @return the session's <code>InetAddress</code>.
	 */
	public final InetAddress getInetAddress() {
		return address.getAddress();
	}

	/**
	 * @return the session's port.
	 */
	public final int getInetPort() {
		return address.getPort();
	}

	/**
	 * @return the session's maximum transfer unit.
	 */
	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * @return the session's current state.
	 */
	public RakNetState getState() {
		return this.state;
	}

	/**
	 * Sets the session's current state.
	 * 
	 * @param state
	 *            the new state.
	 */
	public void setState(RakNetState state) {
		this.state = state;
		log.debug(loggerName + "set state to " + state);
	}

	/**
	 * @return the amount of packets sent this second.
	 */
	public int getPacketsSentThisSecond() {
		return this.packetsSentThisSecond;
	}

	/**
	 * @return the amount of packets received this second.
	 */
	public int getPacketsReceivedThisSecond() {
		return this.packetsReceivedThisSecond;
	}

	/**
	 * @return the last time a packet was sent by the session.
	 */
	public long getLastPacketSendTime() {
		return this.lastPacketSendTime;
	}

	/**
	 * @return the last time a packet was received from the session.
	 */
	public long getLastPacketReceiveTime() {
		return this.lastPacketReceiveTime;
	}

	/**
	 * Bumps the message index and returns the new one, this should only be
	 * called by the <code>SplitPacket</code> and <code>RakNetSession</code>
	 * classes.
	 * 
	 * @return the new message index.
	 */
	protected int bumpMessageIndex() {
		log.debug(loggerName + "Bumped message index from " + messageIndex + " to " + (messageIndex + 1));
		return this.messageIndex++;
	}

	/**
	 * Enables/disables latency detection, when disabled the latency will always
	 * return -1. If the session is not yet in the keep alive state then the
	 * packets needed to detect the latency will not be sent until then.
	 * 
	 * @param enabled
	 *            whether or not latency detection is enabled
	 */
	public void enableLatencyDetection(boolean enabled) {
		boolean wasEnabled = this.latencyEnabled;
		this.latencyEnabled = enabled;
		this.latency = (enabled ? this.latency : -1);
		this.pongsReceived = (enabled ? this.pongsReceived : 0);
		if (wasEnabled != enabled) {
			log.info(loggerName + (enabled ? "Enabled" : "Disabled") + " latency detection.");
		}
	}

	/**
	 * @return whether or not latency detection is enabled.
	 */
	public boolean latencyDetectionEnabled() {
		return this.latencyEnabled;
	}

	/**
	 * @return the average latency for the session.
	 */
	public long getLatency() {
		return this.latency;
	}

	/**
	 * @return the last latency for the session.
	 */
	public long getLastLatency() {
		return this.lastLatency;
	}

	/**
	 * @return the lowest recorded latency for the session.
	 */
	public long getLowestLatency() {
		return this.lowestLatency;
	}

	/**
	 * @return the highest recorded latency for the session.
	 */
	public long getHighestLatency() {
		return this.highestLatency;
	}

	@Override
	public final EncapsulatedPacket sendMessage(Reliability reliability, int channel, Packet packet)
			throws InvalidChannelException {
		// Make sure channel doesn't exceed RakNet limit
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}

		// Set packet properties
		EncapsulatedPacket encapsulated = new EncapsulatedPacket();
		encapsulated.reliability = reliability;
		encapsulated.orderChannel = (byte) channel;
		encapsulated.payload = packet;
		if (reliability.isReliable()) {
			encapsulated.messageIndex = this.bumpMessageIndex();
		}
		if (reliability.isOrdered() || reliability.isSequenced()) {
			encapsulated.orderIndex = (reliability.isOrdered() ? this.orderSendIndex[channel]++
					: this.sequenceSendIndex[channel]++);
			log.debug(loggerName + "Bumped " + (reliability.isOrdered() ? "order" : "sequence") + " index from "
					+ ((reliability.isOrdered() ? this.orderSendIndex[channel] : this.sequenceSendIndex[channel]) - 1)
					+ " to "
					+ (reliability.isOrdered() ? this.orderSendIndex[channel] : this.sequenceSendIndex[channel])
					+ " on channel " + channel);
		}

		// Do we need to split the packet?
		if (SplitPacket.needsSplit(reliability, packet, this.maximumTransferUnit)) {
			encapsulated.splitId = ++this.splitId % 65536;
			for (EncapsulatedPacket split : SplitPacket.splitPacket(this, encapsulated)) {
				sendQueue.add(split);
			}
			log.debug(loggerName + "Split encapsulated packet " + encapsulated.splitId
					+ " and added it to the send queue");
		} else {
			sendQueue.add(encapsulated);
			log.debug(loggerName + "Added encapsulated packet to the send queue");
		}
		log.debug(loggerName + "Sent packet with size of " + packet.size() + " bytes (" + (packet.size() * 8)
				+ " bits) with reliability " + reliability + " on channel " + channel);

		/*
		 * We return a copy of the encapsulated packet because if a single
		 * variable is modified in the encapsulated packet before it is sent,
		 * the whole API could break.
		 */
		return encapsulated.clone();
	}

	@Override
	public final EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, Packet packet)
			throws InvalidChannelException {
		if (this.guid == guid) {
			return this.sendMessage(reliability, channel, packet);
		} else {
			throw new IllegalArgumentException("Invalid GUID");
		}
	}

	/**
	 * Used to tell the session to assign the given packets to an ACK receipt to
	 * be used for when an ACK or NACK arrives.
	 * 
	 * @param packets
	 *            the packets.
	 */
	public final void setAckReceiptPackets(EncapsulatedPacket[] packets) {
		for (EncapsulatedPacket packet : packets) {
			EncapsulatedPacket clone = packet.getClone();
			if (!clone.reliability.requiresAck()) {
				throw new IllegalArgumentException("Invalid reliability " + packet.reliability);
			}
			clone.ackRecord = packet.ackRecord;
			ackReceiptPackets.put(clone, clone.ackRecord.getIndex());
		}
	}

	/**
	 * Sends a raw message.
	 * 
	 * @param packet
	 *            The packet to send.
	 */
	public final void sendRawMessage(Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), this.address));
		log.debug(loggerName + "Sent raw packet with size of " + packet.size() + " bytes (" + (packet.size() * 8)
				+ " bits)");
	}

	/**
	 * Sends a raw message
	 * 
	 * @param buf
	 *            the buffer to send.
	 */
	public final void sendRawMessage(ByteBuf buf) {
		channel.writeAndFlush(new DatagramPacket(buf, this.address));
		log.debug(loggerName + "Sent raw buffer with size of " + buf.capacity() + " bytes (" + (buf.capacity() * 8)
				+ " bits)");
	}

	/**
	 * Sends a <code>CustomPacket</code> with the specified
	 * <code>EncapsulatedPacket</code>'s.
	 * 
	 * @param encapsulated
	 *            the encapsulated packets to send.
	 * @param updateRecoveryQueue
	 *            whether or not to store the encapsulated packets in the
	 *            recovery queue for later, only set this to <code>true</code>
	 *            if you are sending new data and not resending old data.
	 * @return the sequence number of the <code>CustomPacket</code>.
	 */
	private final int sendCustomPacket(ArrayList<EncapsulatedPacket> encapsulated, boolean updateRecoveryQueue) {
		// Create CustomPacket
		CustomPacket custom = new CustomPacket();
		custom.sequenceNumber = this.sendSequenceNumber++;
		custom.messages = encapsulated;
		custom.session = this;
		custom.encode();

		// Send packet
		this.sendRawMessage(custom);

		// Do we need to store it for recovery?
		if (updateRecoveryQueue == true) {
			// Make sure unreliable data is discarded
			custom.removeUnreliables();
			if (custom.messages.size() > 0) {
				recoveryQueue.put(custom.sequenceNumber,
						custom.messages.toArray(new EncapsulatedPacket[custom.messages.size()]));
			}
		}

		// Update packet data
		this.packetsSentThisSecond++;
		this.lastPacketSendTime = System.currentTimeMillis();
		log.debug(loggerName + "Sent custom packet with sequence number " + custom.sequenceNumber);
		return custom.sequenceNumber;
	}

	/**
	 * Sends a <code>CustomPacket</code> with the specified
	 * <code>EncapsulatedPacket</code>'s
	 * 
	 * @param encapsulated
	 *            The encapsulated packets to send
	 * @param updateRecoveryQueue
	 *            Whether or not to store the encapsulated packets in the
	 *            recovery queue for later, only set this to <code>true</code>
	 *            if you are sending new data and not resending old data
	 * @return The sequence number of the <code>CustomPacket</code>
	 */
	private final int sendCustomPacket(EncapsulatedPacket[] encapsulated, boolean updateRecoveryQueue) {
		ArrayList<EncapsulatedPacket> encapsulatedArray = new ArrayList<EncapsulatedPacket>();
		for (EncapsulatedPacket message : encapsulated) {
			encapsulatedArray.add(message);
		}
		return this.sendCustomPacket(encapsulatedArray, updateRecoveryQueue);
	}

	/**
	 * Sends an <code>Acknowledge</code> packet with the specified type and
	 * <code>Record</code>s.
	 * 
	 * @param type
	 *            the type of the <code>Acknowledge</code> packet.
	 * @param records
	 *            the <code>Record</code>s to send.
	 */
	private final void sendAcknowledge(AcknowledgeType type, Record... records) {
		// Create Acknowledge packet
		Acknowledge acknowledge = new Acknowledge(type);
		for (Record record : records) {
			acknowledge.records.add(record);
		}
		acknowledge.encode();
		this.sendRawMessage(acknowledge);

		// Update packet data
		this.lastPacketSendTime = System.currentTimeMillis();
		log.debug(loggerName + "Sent " + acknowledge.records.size() + " records in "
				+ (type == AcknowledgeType.ACKNOWLEDGED ? "ACK" : "NACK") + " packet");
	}

	/**
	 * Handles a <code>CustomPacket</code>.
	 * 
	 * @param custom
	 *            the <code>CustomPacket</code> to handle.
	 */
	public final void handleCustom(CustomPacket custom) {
		// Update packet data
		this.packetsReceivedThisSecond++;

		/*
		 * There are three important things to note here:
		 */

		/*
		 * 1. The reason we subtract one from the difference is because the last
		 * sequence number we received should always be one less than the next
		 * one
		 */

		/*
		 * 2. The reason we add one to the last sequence number to the record
		 * when the difference is bigger than one is because we have already
		 * received that record, this is also the same reason we subtract one
		 * from the CustomPacket's sequence number even when the difference is
		 * not greater than one
		 */

		/*
		 * 3. We always generate the NACK response first because the previous
		 * sequence number data would be destroyed, making it impossible to
		 * generate it
		 */

		// Generate NACK queue if needed
		int difference = custom.sequenceNumber - this.receiveSequenceNumber - 1;
		if (difference > 0) {
			if (difference > 1) {
				this.sendAcknowledge(AcknowledgeType.NOT_ACKNOWLEDGED,
						new Record(this.receiveSequenceNumber + 1, custom.sequenceNumber - 1));
			} else {
				this.sendAcknowledge(AcknowledgeType.NOT_ACKNOWLEDGED, new Record(custom.sequenceNumber - 1));
			}
		}

		// Only handle if it is a newer packet
		if (custom.sequenceNumber > this.receiveSequenceNumber - 1) {
			this.receiveSequenceNumber = custom.sequenceNumber;
			for (EncapsulatedPacket encapsulated : custom.messages) {
				this.handleEncapsulated(encapsulated);
			}

			// Update packet data
			this.lastPacketReceiveTime = System.currentTimeMillis();
		}

		// Send ACK
		this.sendAcknowledge(AcknowledgeType.ACKNOWLEDGED, new Record(custom.sequenceNumber));
		log.debug(loggerName + "Handled custom packet with sequence number " + custom.sequenceNumber);
	}

	/**
	 * Handles an <code>Acknowledge</code> packet and responds accordingly.
	 * 
	 * @param acknowledge
	 *            the <code>Acknowledge</code> packet to handle.
	 */
	public final void handleAcknowledge(Acknowledge acknowledge) {
		if (acknowledge.getType().equals(AcknowledgeType.ACKNOWLEDGED)) {
			for (Record record : acknowledge.records) {
				// Get record data
				int recordIndex = record.getIndex();

				// Are any packets associated with an ACK receipt tied to
				// this record?
				Iterator<EncapsulatedPacket> ackReceiptPacketsI = ackReceiptPackets.keySet().iterator();
				while (ackReceiptPacketsI.hasNext()) {
					EncapsulatedPacket packet = ackReceiptPacketsI.next();
					int packetRecordIndex = ackReceiptPackets.get(packet).intValue();
					if (recordIndex == packetRecordIndex) {
						this.onAcknowledge(record, packet);
						packet.ackRecord = null;
						ackReceiptPacketsI.remove();
					}
				}

				// Remove acknowledged packet from the recovery queue
				recoveryQueue.remove(recordIndex);
			}
		} else if (acknowledge.getType().equals(AcknowledgeType.NOT_ACKNOWLEDGED)) {
			// Track old sequence numbers so they can be properly renamed
			int[] oldSequenceNumbers = new int[acknowledge.records.size()];
			int[] newSequenceNumbers = new int[oldSequenceNumbers.length];

			for (int i = 0; i < acknowledge.records.size(); i++) {
				// Get record data
				Record record = acknowledge.records.get(i);
				int recordIndex = record.getIndex();

				// Are any packets associated with an ACK receipt tied to
				// this record?
				Iterator<EncapsulatedPacket> ackReceiptPacketsI = ackReceiptPackets.keySet().iterator();
				while (ackReceiptPacketsI.hasNext()) {
					EncapsulatedPacket packet = ackReceiptPacketsI.next();
					int packetRecordIndex = ackReceiptPackets.get(packet).intValue();

					/*
					 * We only call onNotAcknowledge() for unreliable packets,
					 * as they can be lost. However, reliable packets will
					 * always eventually be received.
					 */
					if (recordIndex == packetRecordIndex && !packet.reliability.isReliable()) {
						this.onNotAcknowledge(record, packet);
						packet.ackRecord = null;
						ackReceiptPackets.remove(packet);
					}
				}

				// Update records and resend lost packets
				if (recoveryQueue.containsKey(recordIndex)) {
					oldSequenceNumbers[i] = recordIndex;
					newSequenceNumbers[i] = this.sendCustomPacket(recoveryQueue.get(oldSequenceNumbers[i]), false);
				} else {
					oldSequenceNumbers[i] = -1;
					newSequenceNumbers[i] = -1;
				}
			}

			// Rename lost packets
			for (int i = 0; i < oldSequenceNumbers.length; i++) {
				if (oldSequenceNumbers[i] != -1) {
					recoveryQueue.renameKey(oldSequenceNumbers[i], newSequenceNumbers[i]);
				}
			}
		}

		// Update packet data
		this.lastPacketReceiveTime = System.currentTimeMillis();
		log.debug(loggerName + "Handled " + (acknowledge.getType() == AcknowledgeType.ACKNOWLEDGED ? "ACK" : "NACK")
				+ " packet with " + acknowledge.records.size() + " records");
	}

	/**
	 * Handles an <code>EncapsulatedPacket</code> and makes sure all the data is
	 * handled correctly.
	 * 
	 * @param encapsulated
	 *            the <code>EncapsualtedPacket</code> to handle.
	 */
	private final void handleEncapsulated(EncapsulatedPacket encapsulated) {
		Reliability reliability = encapsulated.reliability;

		// Put together split packet
		if (encapsulated.split == true) {
			if (!splitQueue.containsKey(encapsulated.splitId)) {
				// Prevent queue from overflowing
				if (splitQueue.size() + 1 > RakNet.MAX_SPLITS_PER_QUEUE) {
					// Remove unreliable packets from the queue
					Iterator<SplitPacket> splitQueueI = splitQueue.values().iterator();
					while (splitQueueI.hasNext()) {
						SplitPacket splitPacket = splitQueueI.next();
						if (!splitPacket.getReliability().isReliable()) {
							splitQueueI.remove();
						}
					}

					// The queue is filled with reliable packets
					if (splitQueue.size() + 1 > RakNet.MAX_SPLITS_PER_QUEUE) {
						throw new SplitQueueOverloadException();
					}
				}
				splitQueue.put(encapsulated.splitId,
						new SplitPacket(encapsulated.splitId, encapsulated.splitCount, encapsulated.reliability));
			}

			SplitPacket splitPacket = splitQueue.get(encapsulated.splitId);
			Packet finalPayload = splitPacket.update(encapsulated);
			if (finalPayload == null) {
				return; // Do not handle, the split packet is not complete
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
			if (reliablePackets.contains(encapsulated.messageIndex)) {
				return; // Do not handle, it is a duplicate
			}
			reliablePackets.add(encapsulated.messageIndex);
		}

		// Make sure we are handling everything in an ordered/sequenced fashion
		int orderIndex = encapsulated.orderIndex;
		int orderChannel = encapsulated.orderChannel;
		if (orderChannel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		} else {
			// Channel is valid, it is safe to handle
			if (reliability.isOrdered()) {
				handleQueue.get(orderChannel).put(orderIndex, encapsulated);
				while (handleQueue.get(orderChannel).containsKey(orderReceiveIndex[orderChannel])) {
					EncapsulatedPacket orderedEncapsulated = handleQueue.get(orderChannel)
							.get(orderReceiveIndex[orderChannel]++);
					handleQueue.get(orderChannel).remove(orderReceiveIndex[orderChannel] - 1);
					this.handleMessage0(encapsulated.orderChannel, new RakNetPacket(orderedEncapsulated.payload));
				}
			} else if (reliability.isSequenced()) {
				if (orderIndex > sequenceReceiveIndex[orderChannel]) {
					sequenceReceiveIndex[orderChannel] = orderIndex;
					this.handleMessage0(encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
				}
			} else {
				this.handleMessage0(encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
			}
		}
		log.debug(loggerName + "Handled encapsulated packet with " + encapsulated.reliability + " reliability");
	}

	/**
	 * Handles an internal packet related to RakNet, if the ID is unrecognized
	 * it is passed on to the underlying session class.
	 * 
	 * @param channel
	 *            the channel the packet was sent on.
	 * @param packet
	 *            the packet.
	 */
	private final void handleMessage0(int channel, RakNetPacket packet) {
		short packetId = packet.getId();

		if (packetId == ID_CONNECTED_PING) {
			ConnectedPing ping = new ConnectedPing(packet);
			ping.decode();

			ConnectedPong pong = new ConnectedPong();
			pong.timestamp = ping.timestamp;
			pong.timestampPong = this.getTimestamp();
			pong.encode();
			this.sendMessage(Reliability.UNRELIABLE, pong);
		} else if (packetId == ID_CONNECTED_PONG) {
			ConnectedPong pong = new ConnectedPong(packet);
			pong.decode();

			// Calculate latency
			if (latencyEnabled == true && latencyTimestamps.contains(Long.valueOf(pong.timestamp))) {
				latencyTimestamps.remove(Long.valueOf(pong.timestamp));
				long latencyRaw = (this.lastPacketReceiveTime - this.lastPingSendTime);

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
				this.totalLatency += latencyRaw;
				this.latency = (totalLatency / ++pongsReceived);
			}

			// Clear overdue ping responses
			long currentTimestamp = this.getTimestamp();
			Iterator<Long> timestampI = latencyTimestamps.iterator();
			while (timestampI.hasNext()) {
				long timestamp = timestampI.next().longValue();
				if (currentTimestamp - timestamp >= RakNet.SESSION_TIMEOUT || latencyTimestamps.size() > 10) {
					timestampI.remove();
				}
			}
		} else {
			this.handleMessage(packet, channel);
		}

		if (MessageIdentifier.hasPacket(packet.getId())) {
			log.debug(loggerName + "Handled internal packet with ID " + MessageIdentifier.getName(packet.getId()) + " ("
					+ packet.getId() + ")");
		} else {
			log.debug(loggerName + "Sent packet with ID " + packet.getId() + " to session handler");
		}
	}

	/**
	 * Updates the session.
	 */
	public final void update() {
		long currentTime = System.currentTimeMillis();

		// Send next packets in the send queue
		if (!sendQueue.isEmpty() && this.packetsSentThisSecond < RakNet.getMaxPacketsPerSecond()) {
			ArrayList<EncapsulatedPacket> send = new ArrayList<EncapsulatedPacket>();
			int sendLength = CustomPacket.calculateDummy();

			// Add packets
			Iterator<EncapsulatedPacket> sendQueueI = sendQueue.iterator();
			while (sendQueueI.hasNext()) {
				// Make sure the packet will not cause an overflow
				EncapsulatedPacket encapsulated = sendQueueI.next();
				if (encapsulated == null) {
					sendQueueI.remove();
					continue;
				}
				sendLength += encapsulated.calculateSize();
				if (sendLength > this.maximumTransferUnit) {
					break;
				}

				// Add the packet and remove it from the queue
				send.add(encapsulated);
				sendQueueI.remove();
			}

			// Send packet
			if (send.size() > 0) {
				this.sendCustomPacket(send, true);
			}
		}

		// Resend lost packets
		Iterator<EncapsulatedPacket[]> recoveryQueueI = recoveryQueue.values().iterator();
		if (currentTime - this.lastRecoverySendTime >= RakNet.RECOVERY_SEND_INTERVAL && recoveryQueueI.hasNext()) {
			this.sendCustomPacket(recoveryQueueI.next(), false);
			this.lastRecoverySendTime = currentTime;
		}

		// Send ping to detect latency if it is enabled
		if (this.latencyEnabled == true && currentTime - this.lastPingSendTime >= RakNet.PING_SEND_INTERVAL
				&& state.equals(RakNetState.CONNECTED)) {
			ConnectedPing ping = new ConnectedPing();
			ping.timestamp = this.getTimestamp();
			ping.encode();

			this.sendMessage(Reliability.UNRELIABLE, ping);
			this.lastPingSendTime = currentTime;
			latencyTimestamps.add(Long.valueOf(ping.timestamp));
			log.debug(loggerName + "Sent ping to session with timestamp " + ping.timestamp);
		}

		// Make sure the client is still connected
		if (currentTime - this.lastPacketReceiveTime >= RakNet.DETECTION_SEND_INTERVAL
				&& currentTime - this.lastKeepAliveSendTime >= RakNet.DETECTION_SEND_INTERVAL
				&& state.equals(RakNetState.CONNECTED)) {
			this.sendMessage(Reliability.UNRELIABLE, ID_DETECT_LOST_CONNECTIONS);
			this.lastKeepAliveSendTime = currentTime;
			log.debug(loggerName + "Sent " + MessageIdentifier.getName(ID_DETECT_LOST_CONNECTIONS)
					+ " packet to session");
		}

		// Client timed out
		if (currentTime - this.lastPacketReceiveTime >= RakNet.SESSION_TIMEOUT) {
			throw new TimeoutException();
		}

		// Reset packet data
		if (currentTime - this.lastPacketCounterResetTime >= 1000L) {
			this.packetsSentThisSecond = 0;
			this.packetsReceivedThisSecond = 0;
			this.lastPacketCounterResetTime = currentTime;
		}
	}

	/**
	 * This function is called when a acknowledge receipt is received for the
	 * packet.
	 * 
	 * @param record
	 *            the received record.
	 * @param packet
	 *            the received packet.
	 */
	public abstract void onAcknowledge(Record record, EncapsulatedPacket packet);

	/**
	 * This function is called when a not acknowledged receipt is received for
	 * the packet.
	 * 
	 * @param record
	 *            the lost record.
	 * @param packet
	 *            the lost packet.
	 */
	public abstract void onNotAcknowledge(Record record, EncapsulatedPacket packet);

	/**
	 * This function is called when a packet is received by the session.
	 * 
	 * @param packet
	 *            the packet to handle.
	 * @param channel
	 *            the packet the channel was sent on.
	 */
	public abstract void handleMessage(RakNetPacket packet, int channel);

}
