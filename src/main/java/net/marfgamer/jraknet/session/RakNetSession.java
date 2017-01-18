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
 * Copyright (c) 2016, 2017 MarfGamer
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
package net.marfgamer.jraknet.session;

import static net.marfgamer.jraknet.protocol.MessageIdentifier.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.Reliability;
import net.marfgamer.jraknet.protocol.SplitPacket;
import net.marfgamer.jraknet.protocol.message.CustomPacket;
import net.marfgamer.jraknet.protocol.message.EncapsulatedPacket;
import net.marfgamer.jraknet.protocol.message.acknowledge.Acknowledge;
import net.marfgamer.jraknet.protocol.message.acknowledge.AcknowledgeType;
import net.marfgamer.jraknet.protocol.message.acknowledge.Record;
import net.marfgamer.jraknet.protocol.status.ConnectedPing;
import net.marfgamer.jraknet.protocol.status.ConnectedPong;
import net.marfgamer.jraknet.util.map.IntMap;

/**
 * This class is used to easily manage connections in RakNet
 *
 * @author MarfGamer
 */
public abstract class RakNetSession implements UnumRakNetPeer, GeminusRakNetPeer {

    // Session data
    private final long guid;
    private final int maximumTransferUnit;
    private final Channel channel;
    private final InetSocketAddress address;
    private RakNetState state;
    private int keepAliveState;

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
    private final ArrayList<Integer> reliables;
    private final IntMap<SplitPacket> splitQueue;
    private final ArrayList<EncapsulatedPacket> sendQueue;
    private final IntMap<EncapsulatedPacket[]> recoveryQueue;

    // Ordering and sequencing
    private int sendSequenceNumber;
    private int receiveSequenceNumber;
    private final int[] orderSendIndex;
    private final int[] orderReceiveIndex;
    private final int[] sequenceSendIndex;
    private final int[] sequenceReceiveIndex;
    private final IntMap<IntMap<EncapsulatedPacket>> handleQueue;

    // Latency detection
    private boolean latencyEnabled;
    private int pongsReceived;
    private long latencyIdentifier;
    private long totalLatency;
    private long latency;
    private long lastLatency;
    private long lowestLatency;
    private long highestLatency;

    public RakNetSession(long guid, int maximumTransferUnit, Channel channel, InetSocketAddress address) {
	// Session data
	this.guid = guid;
	this.maximumTransferUnit = maximumTransferUnit;
	this.channel = channel;
	this.address = address;
	this.state = RakNetState.DISCONNECTED;
	this.keepAliveState = RakNetState.CONNECTED.getOrder();

	// Timing
	this.lastPacketReceiveTime = System.currentTimeMillis();

	// Packet data
	this.reliables = new ArrayList<Integer>();
	this.splitQueue = new IntMap<SplitPacket>();
	this.sendQueue = new ArrayList<EncapsulatedPacket>();
	this.recoveryQueue = new IntMap<EncapsulatedPacket[]>();

	// Ordering and sequencing
	this.orderSendIndex = new int[RakNet.MAX_CHANNELS];
	this.orderReceiveIndex = new int[RakNet.MAX_CHANNELS];
	this.sequenceSendIndex = new int[RakNet.MAX_CHANNELS];
	this.sequenceReceiveIndex = new int[RakNet.MAX_CHANNELS];
	this.handleQueue = new IntMap<IntMap<EncapsulatedPacket>>();
	for (int i = 0; i < RakNet.MAX_CHANNELS; i++) {
	    handleQueue.put(i, new IntMap<EncapsulatedPacket>());
	}

	// Latency detection
	this.latencyEnabled = true;
	this.latency = -1; // We can't predict them
	this.lastLatency = -1;
	this.lowestLatency = -1;
	this.highestLatency = -1;
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
     * Returns the session's <code>InetAddress</code>
     * 
     * @return The session's <code>InetAddress</code>
     */
    public final InetAddress getInetAddress() {
	return address.getAddress();
    }

    /**
     * Returns the session's port
     * 
     * @return The session's port
     */
    public final int getInetPort() {
	return address.getPort();
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
     * Returns the session's keep alive state
     * 
     * @return The session's keep alive state
     */
    public int getKeepAliveState() {
	return this.keepAliveState;
    }

    /**
     * Sets the session's keep alive state
     * 
     * @param keepAliveState
     *            The new keep alive state
     */
    public void setKeepAliveState(int keepAliveState) {
	this.keepAliveState = keepAliveState;
    }

    /**
     * Sets the session's keep alive state
     * 
     * @param keepAliveState
     *            The new keep alive state
     */
    public void setKeepAliveState(RakNetState keepAliveState) {
	this.setKeepAliveState(keepAliveState.getOrder());
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
     * Returns the last time a packet (<code>CustomPacket</code> or
     * <code>Acknowledgement</code>) was sent by the session
     * 
     * @return The last time a packet was sent by the session
     */
    public long getLastPacketSendTime() {
	return this.lastPacketSendTime;
    }

    /**
     * Returns the last time a packet (<code>CustomPacket</code> or
     * <code>Acknowledgement</code>) was received from the session
     * 
     * @return The last time a packet was received from the session
     */
    public long getLastPacketReceiveTime() {
	return this.lastPacketReceiveTime;
    }

    /**
     * Enables/disables latency detection, when disabled the latency will always
     * return -1<br>
     * <br>
     * Note: If the session is not yet in the keep alive state then the packets
     * needed to detect the latency will not be sent until then
     * 
     * @param enabled
     *            Whether or not latency detection is enabled
     */
    public void enableLatencyDetection(boolean enabled) {
	this.latencyEnabled = enabled;
	this.latency = (enabled ? this.latency : -1);
	this.pongsReceived = (enabled ? this.pongsReceived : 0);
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

    @Override
    public final void sendMessage(Reliability reliability, int channel, Packet packet) throws InvalidChannelException {
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
	    encapsulated.messageIndex = this.messageIndex++;
	}
	if (reliability.isOrdered() || reliability.isSequenced()) {
	    encapsulated.orderIndex = (reliability.isOrdered() ? this.orderSendIndex[channel]++
		    : this.sequenceSendIndex[channel]++);
	}

	// Do we need to split the packet?
	if (SplitPacket.needsSplit(reliability, packet, this.maximumTransferUnit)) {
	    encapsulated.splitId = this.splitId++;
	    for (EncapsulatedPacket split : SplitPacket.splitPacket(encapsulated, this.maximumTransferUnit)) {
		sendQueue.add(split);
	    }
	} else {
	    sendQueue.add(encapsulated);
	}
    }

    @Override
    public final void sendMessage(long guid, Reliability reliability, int channel, Packet packet)
	    throws InvalidChannelException {
	if (this.guid == guid) {
	    this.sendMessage(reliability, channel, packet);
	} else {
	    throw new IllegalArgumentException("Invalid GUID");
	}
    }

    /**
     * Sends a raw message
     * 
     * @param packet
     *            The packet to send
     */
    public final void sendRawMessage(Packet packet) {
	channel.writeAndFlush(new DatagramPacket(packet.buffer(), this.address));
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
    private final synchronized int sendCustomPacket(ArrayList<EncapsulatedPacket> encapsulated,
	    boolean updateRecoveryQueue) {
	// Create CustomPacket
	CustomPacket custom = new CustomPacket();
	custom.sequenceNumber = this.sendSequenceNumber++;
	custom.messages = encapsulated;
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
    private final synchronized int sendCustomPacket(EncapsulatedPacket[] encapsulated, boolean updateRecoveryQueue) {
	ArrayList<EncapsulatedPacket> encapsulatedArray = new ArrayList<EncapsulatedPacket>();
	for (EncapsulatedPacket message : encapsulated) {
	    encapsulatedArray.add(message);
	}
	return this.sendCustomPacket(encapsulatedArray, updateRecoveryQueue);
    }

    /**
     * Sends an <code>Acknowledge</code> packet with the specified type and
     * records
     * 
     * @param type
     *            The type of the <code>Acknowledge</code> packet
     * @param records
     *            The records to send
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
    }

    /**
     * Handles a <code>CustomPacket</code>
     * 
     * @param custom
     *            The <code>CustomPacket</code> to handle
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
    }

    /**
     * Handles an <code>Acknowledge</code> packet and responds accordingly
     * 
     * @param acknowledge
     *            The <code>Acknowledge</code> packet to handle
     */
    public final synchronized void handleAcknowledge(Acknowledge acknowledge) {
	if (acknowledge.getType().equals(AcknowledgeType.ACKNOWLEDGED)) {
	    // Remove acknowledged packets from the recovery queue
	    for (Record record : acknowledge.records) {
		// TODO: Implement onAcknowledge
		recoveryQueue.remove(record.getIndex());
	    }
	} else if (acknowledge.getType().equals(AcknowledgeType.NOT_ACKNOWLEDGED)) {
	    // Track old sequence numbers so they can be properly renamed
	    int[] oldSequenceNumbers = new int[acknowledge.records.size()];
	    int[] newSequenceNumbers = new int[oldSequenceNumbers.length];

	    for (int i = 0; i < acknowledge.records.size(); i++) {
		// TODO: Implement onNotAcknowledge

		// Update records and resend lost packets
		Record record = acknowledge.records.get(i);
		if (recoveryQueue.containsKey(record.getIndex())) {
		    oldSequenceNumbers[i] = record.getIndex();
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
    }

    /**
     * Handles an <code>EncapsulatedPacket</code> and makes sure all the data is
     * handled correctly
     * 
     * @param encapsulated
     *            The <code>EncapsualtedPacket</code> to handle
     */
    private final void handleEncapsulated(EncapsulatedPacket encapsulated) {
	Reliability reliability = encapsulated.reliability;

	// Put together split packet
	if (encapsulated.split == true) {
	    if (!splitQueue.containsKey(encapsulated.splitId)) {
		// We remove unreliables here incase the new split packet is
		// unreliable
		if (splitQueue.size() + 1 > RakNet.MAX_SPLITS_PER_QUEUE) {
		    // Remove unreliable packets from the queue
		    Iterator<SplitPacket> splitPackets = splitQueue.values().iterator();
		    while (splitPackets.hasNext()) {
			SplitPacket splitPacket = splitPackets.next();
			if (!splitPacket.getReliability().isReliable()) {
			    splitPackets.remove();
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
	    if (reliables.contains(encapsulated.messageIndex)) {
		return; // Do not handle, it is a duplicate
	    }
	    reliables.add(encapsulated.messageIndex);
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
		    this.handlePacket0(encapsulated.orderChannel, new RakNetPacket(orderedEncapsulated.payload));
		}
	    } else if (reliability.isSequenced()) {
		if (orderIndex > sequenceReceiveIndex[orderChannel]) {
		    sequenceReceiveIndex[orderChannel] = orderIndex + 1;
		    this.handlePacket0(encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
		}
	    } else {
		this.handlePacket0(encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
	    }
	}
    }

    /**
     * Handles an internal packet related to RakNet, if the ID is unrecognized
     * it is passed on to the underlying session class
     * 
     * @param channel
     *            The channel the packet was sent on
     * @param packet
     *            The packet
     */
    private final void handlePacket0(int channel, RakNetPacket packet) {
	short packetId = packet.getId();

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
		if (latencyIdentifier - pong.identifier == 1) {
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
		    this.pongsReceived++;
		    this.totalLatency += latencyRaw;
		    this.latency = (totalLatency / pongsReceived);
		}
	    }

	    this.latencyIdentifier = (pong.identifier + 1);
	} else {
	    this.handlePacket(packet, channel);
	}
    }

    /**
     * Updates the session
     */
    public final synchronized void update() {
	long currentTime = System.currentTimeMillis();

	// Send packets in the send queue
	if (!sendQueue.isEmpty() && this.packetsSentThisSecond < RakNet.MAX_PACKETS_PER_SECOND) {
	    ArrayList<EncapsulatedPacket> send = new ArrayList<EncapsulatedPacket>();
	    int sendLength = CustomPacket.calculateDummy();

	    // Add packets
	    Iterator<EncapsulatedPacket> queue = sendQueue.iterator();
	    while (queue.hasNext()) {
		// Make sure the packet will not cause an overflow
		EncapsulatedPacket encapsulated = queue.next();
		sendLength += encapsulated.calculateSize();
		if (sendLength > this.maximumTransferUnit) {
		    break;
		}

		// Add the packet and remove it from the queue
		send.add(encapsulated);
		queue.remove();
	    }

	    // Send packet
	    if (send.size() > 0) {
		this.sendCustomPacket(send, true);
	    }
	}

	// Resend lost packets
	Iterator<EncapsulatedPacket[]> recovering = recoveryQueue.values().iterator();
	if (currentTime - this.lastRecoverySendTime >= RakNet.RECOVERY_SEND_INTERVAL && recovering.hasNext()) {
	    this.sendCustomPacket(recovering.next(), false);
	    this.lastRecoverySendTime = currentTime;
	}

	// Send ping to detect latency if it is enabled
	if (currentTime - this.lastPingSendTime >= RakNet.PING_SEND_INTERVAL && state.getOrder() >= this.keepAliveState
		&& this.keepAliveState >= 0) {
	    ConnectedPing ping = new ConnectedPing();
	    ping.identifier = this.latencyIdentifier++;
	    ping.encode();

	    this.sendMessage(Reliability.UNRELIABLE, ping);
	    this.lastPingSendTime = currentTime;
	}

	// Make sure the client is still connected
	if (currentTime - this.lastPacketReceiveTime >= RakNet.DETECTION_SEND_INTERVAL
		&& currentTime - this.lastKeepAliveSendTime >= RakNet.DETECTION_SEND_INTERVAL
		&& state.getOrder() >= this.keepAliveState && this.keepAliveState >= 0) {
	    this.sendMessage(Reliability.UNRELIABLE, ID_DETECT_LOST_CONNECTIONS);
	    this.lastKeepAliveSendTime = currentTime;
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
