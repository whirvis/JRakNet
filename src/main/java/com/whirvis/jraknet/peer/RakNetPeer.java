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
 * Copyright (c) 2016-2019 Trent Summerlin
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
package com.whirvis.jraknet.peer;

import static com.whirvis.jraknet.RakNetPacket.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.map.concurrent.ConcurrentIntMap;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.message.CustomFourPacket;
import com.whirvis.jraknet.protocol.message.CustomPacket;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.AcknowledgedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.NotAcknowledgedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;
import com.whirvis.jraknet.protocol.status.ConnectedPing;
import com.whirvis.jraknet.protocol.status.ConnectedPong;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

/**
 * Represents a connection to another machine, be it a server or a client.
 * 
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public abstract class RakNetPeer implements RakNetPeerMessenger {

	/**
	 * Used to store the message index for received reliable packets in a
	 * condensed fashion.
	 * 
	 * @author Trent Summerlin
	 * @since JRakNet v2.11.0
	 */
	private static class ConcurrentMessageIndexList {

		private List<Record> indexes;

		/**
		 * Constructs a <code>ConcurrentMesageIndexList</code>.
		 */
		public ConcurrentMessageIndexList() {
			this.indexes = new ArrayList<Record>();
		}

		/**
		 * Adds the specified message index to the list.
		 * 
		 * @param index
		 *            the index to add.
		 */
		public synchronized void add(int index) {
			indexes.add(new Record(index));
			this.indexes = Arrays.asList(Record.condense(indexes));
		}

		/**
		 * Returns whether or not the list contains the specified message index.
		 * 
		 * @param index
		 *            the index.
		 * @return <code>true</code> if the list contains the
		 *         <code>index</code>.
		 */
		public synchronized boolean contains(int index) {
			for (Record record : indexes) {
				if ((record.isRanged() && record.getIndex() >= index && record.getIndex() <= index)
						|| record.getIndex() == index) {
					return true;
				}
			}
			return false;
		}

	}

	private final Logger log;
	private final InetSocketAddress address;
	private final long guid;
	private final int maximumTransferUnit;
	private final ConnectionType connectionType;
	private final Channel channel;
	private RakNetState state;
	private int packetsSentThisSecond;
	private int packetsReceivedThisSecond;
	private long lastPacketsSentThisSecondResetTime;
	private long lastPacketsReceivedThisSecondResetTime;
	private long lastPacketSendTime;
	private long lastPacketReceiveTime;
	private long lastRecoverySendTime;
	private long lastKeepAliveSendTime;
	private long lastPingSendTime;
	private int messageIndex;
	private int splitId;
	private final ConcurrentMessageIndexList reliablePackets;
	private final ConcurrentIntMap<EncapsulatedPacket.Split> splitQueue;
	protected final ConcurrentLinkedQueue<EncapsulatedPacket> sendQueue;
	private final ConcurrentIntMap<EncapsulatedPacket[]> recoveryQueue;
	private final ConcurrentHashMap<EncapsulatedPacket, Integer> ackReceiptPackets;
	private int sendSequenceNumber;
	private int receiveSequenceNumber;
	private final int[] orderSendIndex;
	private final int[] orderReceiveIndex;
	private final int[] sequenceSendIndex;
	private final int[] sequenceReceiveIndex;
	private final ConcurrentIntMap<ConcurrentIntMap<EncapsulatedPacket>> handleQueue;
	private boolean latencyEnabled;
	private int pongsReceived;
	private long totalLatency;
	private long latency;
	private long lastLatency;
	private long lowestLatency;
	private long highestLatency;
	private final ArrayList<Long> latencyTimestamps;

	/**
	 * Creates a RakNet peer.
	 * 
	 * @param address
	 *            the address of the peer.
	 * @param guid
	 *            the globally unique ID of the peer.
	 * @param maximumTransferUnit
	 *            the maximum transfer unit of the peer.
	 * @param connectionType
	 *            the connection type of the peer.
	 * @param channel
	 *            the channel to communicate to the peer with.
	 */
	public RakNetPeer(InetSocketAddress address, long guid, int maximumTransferUnit, ConnectionType connectionType,
			Channel channel) {
		this.log = LogManager.getLogger("jraknet-peer-" + Long.toHexString(guid));
		this.address = address;
		this.guid = guid;
		this.maximumTransferUnit = maximumTransferUnit;
		this.connectionType = connectionType;
		this.channel = channel;
		this.state = RakNetState.DISCONNECTED;
		this.reliablePackets = new ConcurrentMessageIndexList();
		this.splitQueue = new ConcurrentIntMap<EncapsulatedPacket.Split>();
		this.sendQueue = new ConcurrentLinkedQueue<EncapsulatedPacket>();
		this.recoveryQueue = new ConcurrentIntMap<EncapsulatedPacket[]>();
		this.ackReceiptPackets = new ConcurrentHashMap<EncapsulatedPacket, Integer>();
		this.orderSendIndex = new int[RakNet.MAX_CHANNELS];
		this.orderReceiveIndex = new int[RakNet.MAX_CHANNELS];
		this.sequenceSendIndex = new int[RakNet.MAX_CHANNELS];
		this.sequenceReceiveIndex = new int[RakNet.MAX_CHANNELS];
		this.handleQueue = new ConcurrentIntMap<ConcurrentIntMap<EncapsulatedPacket>>();
		for (int i = 0; i < RakNet.MAX_CHANNELS; i++) {
			sequenceReceiveIndex[i] = -1;
			handleQueue.put(i, new ConcurrentIntMap<EncapsulatedPacket>());
		}
		this.latencyEnabled = true;
		this.latency = -1;
		this.lastLatency = -1;
		this.lowestLatency = -1;
		this.highestLatency = -1;
		this.latencyTimestamps = new ArrayList<Long>();
	}

	/**
	 * Returns the peer's address.
	 * 
	 * @return the peer's address.
	 */
	public final InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * Returns the peer's IP address.
	 * 
	 * @return the peer's IP address.
	 */
	public final InetAddress getInetAddress() {
		return address.getAddress();
	}

	/**
	 * Returns the peer's port.
	 * 
	 * @return the peer's port.
	 */
	public final int getPort() {
		return address.getPort();
	}

	/**
	 * Returns the peer's globally unique ID.
	 * 
	 * @return the peer's globally unique ID.
	 */
	public final long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * Returns the peer's maximum transfer unit.
	 * 
	 * @return the peer's maximum transfer unit.
	 */
	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Returns the connection type of the peer.
	 * 
	 * @return the connection type of the peer.
	 */
	public final ConnectionType getConnectionType() {
		return this.connectionType;
	}

	/**
	 * Returns the current state of the peer, guaranteed to not be
	 * <code>null</code>.
	 * 
	 * @return the current state of the peer.
	 */
	public RakNetState getState() {
		return this.state;
	}

	/**
	 * Sets the current state of the peer.
	 * 
	 * @param state
	 *            the state.
	 * @throws NullPointerException
	 *             if the <code>state</code> is <code>null</code>.
	 */
	public void setState(RakNetState state) throws NullPointerException {
		if (state == null) {
			throw new NullPointerException("State cannot be null");
		}
		this.state = state;
		log.debug("Set state to " + state.name());
	}

	/**
	 * Returns the peer's timestamp. If login has not yet been completed,
	 * <code>-1</code> will be returned.
	 * 
	 * @return the peer's timestamp, <code>-1</code> if login has not yet been
	 *         completed.
	 */
	public abstract long getTimestamp();

	/**
	 * Returns the amount of packets sent in the last second.
	 * 
	 * @return the amount of packets sent in the last second.
	 */
	public int getPacketsSentThisSecond() {
		return this.packetsSentThisSecond;
	}

	/**
	 * Returns the amount of packets received in the last second.
	 * 
	 * @return the amount of packets received in the last second.
	 */
	public int getPacketsReceivedThisSecond() {
		return this.packetsReceivedThisSecond;
	}

	/**
	 * Returns the last time a packet was sent by the peer.
	 * 
	 * @return the last time a packet was sent by the peer.
	 */
	public long getLastPacketSendTime() {
		return this.lastPacketSendTime;
	}

	/**
	 * Returns the last tiem a packet was received.
	 * 
	 * @return the last time a packet was received.
	 */
	public long getLastPacketReceiveTime() {
		return this.lastPacketReceiveTime;
	}

	/**
	 * Returns the message index and bumps it.
	 * <p>
	 * This method should only ever be called by the
	 * {@link EncapsulatedPacket.Split} class. If it is called by anyone else
	 * and it is set out of sync, all {@link Reliability#RELIABLE RELIABLE}
	 * based reliabilities will break and the connection will have to be closed.
	 * 
	 * @return the message index.
	 */
	public int bumpMessageIndex() {
		log.debug("Bumped message index from " + messageIndex + " to " + (messageIndex + 1));
		return this.messageIndex++;
	}

	/**
	 * Enables/disables latency detection.
	 * <p>
	 * When disabled, all methods relating to latency will report
	 * <code>-1</code>. If the peer is not yet in the keep alive state then the
	 * packets needed to detect the latency will not be sent until then.
	 * 
	 * @param enabled
	 *            <code>true</code> to enable latency detection,
	 *            <code>false</code> to disable it.
	 */
	public void enableLatencyDetection(boolean enabled) {
		boolean wasEnabled = latencyEnabled;
		this.latencyEnabled = enabled;
		this.latency = enabled ? latency : -1;
		this.pongsReceived = enabled ? pongsReceived : 0;
		if (wasEnabled != enabled) {
			log.info((enabled ? "Enabled" : "Disabled") + " latency detection.");
		}
	}

	/**
	 * Returns whether or not latency detection is enabled.
	 * 
	 * @return <code>true</code> if latency detection is enabled,
	 *         <code>false</code> otherwise.
	 */
	public boolean latencyDetectionEnabled() {
		return this.latencyEnabled;
	}

	/**
	 * Returns the average latency for the peer.
	 * 
	 * @return the average latency for the peer.
	 */
	public long getLatency() {
		return this.latency;
	}

	/**
	 * Returns the last calculated latency for the peer.
	 * <p>
	 * This is not the same as {@link #getLatency()}, which is more accurate as
	 * returns the average latency of the peer.
	 * 
	 * @return the last calculated latency for the peer.
	 */
	public long getLastLatency() {
		return this.lastLatency;
	}

	/**
	 * Returns the lowest recorded latency for the peer.
	 * 
	 * @return the lowest recorded latency for the peer.
	 */
	public long getLowestLatency() {
		return this.lowestLatency;
	}

	/**
	 * Returns the highest recorded latency for the peer.
	 * 
	 * @return the highest recorded latency for the peer.
	 */
	public long getHighestLatency() {
		return this.highestLatency;
	}

	/**
	 * Handles the packet.
	 * 
	 * @param packet
	 *            the packet to handle.
	 * @throws NullPointerException
	 *             if the <code>packet</code> is <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the packet is a {@link CustomPacket CUSTOM_PACKET} and the
	 *             channel of an encapsulated packet found inside of it is
	 *             greater than or equal to {@value RakNet#MAX_CHANNELS}.
	 * @throws SplitQueueOverflowException
	 *             if the packet is a {@link CustomPacket CUSTOM_PACKET}, an
	 *             encapsulated packet found inside of it is split, and adding
	 *             it to the split queue would cause it to overflow.
	 */
	public final void handleMessage(RakNetPacket packet)
			throws NullPointerException, InvalidChannelException, SplitQueueOverflowException {
		if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		}
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastPacketsReceivedThisSecondResetTime >= 1000L) {
			this.packetsReceivedThisSecond = 0;
			this.lastPacketsReceivedThisSecondResetTime = currentTime;
		}
		this.packetsReceivedThisSecond++;
		int packetId = packet.getId();
		if (packetId >= ID_CUSTOM_0 && packetId <= ID_CUSTOM_F) {
			CustomPacket custom = new CustomPacket(packet);
			custom.decode();

			/*
			 * NACK must be generated first before the session data is updated,
			 * otherwise the data needed to know which packets have been lost
			 * will have been overwriten.
			 */
			int skipped = custom.sequenceId - receiveSequenceNumber - 1;
			if (skipped > 0) {
				this.sendAcknowledge(false, skipped == 1 ? new Record(custom.sequenceId - 1)
						: new Record(receiveSequenceNumber + 1, custom.sequenceId - 1));
			}

			if (custom.sequenceId > receiveSequenceNumber - 1) {
				this.receiveSequenceNumber = custom.sequenceId;
				for (EncapsulatedPacket encapsulated : custom.messages) {
					this.handleEncapsulated(encapsulated);
				}
				this.lastPacketReceiveTime = System.currentTimeMillis();
			}
			this.sendAcknowledge(true, new Record(custom.sequenceId));
			log.debug("Handled custom packet with sequence number " + custom.sequenceId);
		} else if (packetId == ID_NACK) {
			NotAcknowledgedPacket notAcknowledged = new NotAcknowledgedPacket(packet);
			notAcknowledged.decode();

			/*
			 * When a peer realizes they have lost a packet in transmission,
			 * they only send a NACK packet once. This makes implementation
			 * easier. However, this can make this code look a bit whacky.
			 * 
			 * What's happening here is we are getting the old sequence numbers
			 * of the lost packets and then creating an array to store the new
			 * ones in. The new sequence number is found when sending the custom
			 * packet containing the lost data without updating the recovery
			 * queue.
			 * 
			 * As a final step, we rename the keys in the recovery queue from
			 * the old sequence number to the new sequence number after all the
			 * packets that were lost in transmission were resent. We do not
			 * remove them from the recovery queue until the peer has responded
			 * with an ACK packet.
			 */
			int[] oldSequenceNumbers = new int[notAcknowledged.records.length];
			int[] newSequenceNumbers = new int[oldSequenceNumbers.length];
			for (int i = 0; i < notAcknowledged.records.length; i++) {
				Record record = notAcknowledged.records[i];
				int recordIndex = record.getIndex();

				// Fire onNotAcknowledge() event for packets lost in tranmission
				Iterator<EncapsulatedPacket> ackReceiptPacketsI = ackReceiptPackets.keySet().iterator();
				while (ackReceiptPacketsI.hasNext()) {
					EncapsulatedPacket encapsulated = ackReceiptPacketsI.next();
					int encapsulatedRecordIndex = ackReceiptPackets.get(encapsulated).intValue();
					if (recordIndex == encapsulatedRecordIndex) {
						this.onNotAcknowledge(record, encapsulated);
						encapsulated.ackRecord = null;
						ackReceiptPackets.remove(encapsulated);
					}
				}

				// Resend packets lost in transmission
				if (recoveryQueue.containsKey(recordIndex)) {
					oldSequenceNumbers[i] = recordIndex;
					newSequenceNumbers[i] = this.sendCustomPacket(false, recoveryQueue.get(oldSequenceNumbers[i]));
				} else {
					oldSequenceNumbers[i] = -1;
					newSequenceNumbers[i] = -1;
				}
			}

			/*
			 * Rename recovery queue keys in case these packets are lost in
			 * transmission again. Packets are not removed from the recovery
			 * queue until an ACK packet is received.
			 */
			for (int i = 0; i < oldSequenceNumbers.length; i++) {
				if (oldSequenceNumbers[i] >= 0) {
					recoveryQueue.renameKey(oldSequenceNumbers[i], newSequenceNumbers[i]);
				}
			}
		} else if (packetId == ID_ACK) {
			AcknowledgedPacket acknowledged = new AcknowledgedPacket(packet);
			acknowledged.decode();
			for (Record record : acknowledged.records) {
				int recordIndex = record.getIndex();
				Iterator<EncapsulatedPacket> ackReceiptPacketsI = ackReceiptPackets.keySet().iterator();
				while (ackReceiptPacketsI.hasNext()) {
					EncapsulatedPacket encapsulated = ackReceiptPacketsI.next();
					int encapsulatedRecordIndex = ackReceiptPackets.get(encapsulated).intValue();
					if (recordIndex == encapsulatedRecordIndex) {
						this.onAcknowledge(record, encapsulated);
						encapsulated.ackRecord = null;
						ackReceiptPacketsI.remove();
					}
				}
				recoveryQueue.remove(recordIndex);
			}
			log.debug("Handled ACK packet with " + acknowledged.records.length + " record"
					+ (acknowledged.records.length == 1 ? "" : "s"));
		}
	}

	/**
	 * Handles an {@link EncapsulatedPacket}.
	 * 
	 * @param encapsulated
	 *            the encapsulated packet.
	 * @throws NullPointerException
	 *             if the <code>encapsulated</code> packet is <code>null</code>.
	 * @throws InvalidChannelException
	 *             if the channel of the <code>encapsulated</code> packet is
	 *             greater than or equal to {@value RakNet#MAX_CHANNELS}.
	 * @throws SplitQueueOverflowException
	 *             if the <code>encapsulated</code> packet is split, and adding
	 *             it to the split queue would cause it to overflow.
	 */
	private final void handleEncapsulated(EncapsulatedPacket encapsulated)
			throws InvalidChannelException, SplitQueueOverflowException {
		if (encapsulated == null) {
			throw new NullPointerException("Encapsulated packet cannot be null");
		} else if (encapsulated.orderChannel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		} else if (encapsulated.split == true) {
			if (!splitQueue.containsKey(encapsulated.splitId)) {
				splitQueue.put(encapsulated.splitId, new EncapsulatedPacket.Split(encapsulated.splitId,
						encapsulated.splitCount, encapsulated.reliability));

				/**
				 * If the split queue is greater than the maximum amount of
				 * packets that can be split, remove all unreliable split
				 * packets. If the split queue is still too big, then the queue
				 * has been overloaded.
				 */
				if (splitQueue.size() > RakNet.MAX_SPLITS_PER_QUEUE) {
					Iterator<EncapsulatedPacket.Split> splitQueueI = splitQueue.values().iterator();
					int removeCount = 0;
					while (splitQueueI.hasNext()) {
						EncapsulatedPacket.Split splitPacket = splitQueueI.next();
						if (!splitPacket.getReliability().isReliable()) {
							splitQueueI.remove();
							removeCount++;
						}
					}
					if (removeCount > 0) {
						log.warn("Removed " + removeCount
								+ " unreliable packets from the split queue due to an overflowing split queue");
					}
					if (splitQueue.size() > RakNet.MAX_SPLITS_PER_QUEUE) {
						throw new SplitQueueOverflowException();
					}
				}
			}
			EncapsulatedPacket stitched = splitQueue.get(encapsulated.splitId).update(encapsulated);
			if (stitched != null) {
				splitQueue.remove(encapsulated.splitId);
				this.handleEncapsulated(stitched);
			}
		} else if (encapsulated.reliability.isReliable() && !reliablePackets.contains(encapsulated.messageIndex)) {
			/**
			 * Determine if the message should be handled based on its
			 * reliability.
			 * 
			 * If the message is ordered, only handle it when all the messages
			 * before it on the channel have also been received and are ready to
			 * be handled.
			 * 
			 * If the message is sequenced, only handle it if it is the newest
			 * packet on the channel.
			 * 
			 * If the message is neither ordered nor sequenced, then it is
			 * handled regardless.
			 */
			reliablePackets.add(encapsulated.messageIndex);
			if (encapsulated.reliability.isOrdered()) {
				handleQueue.get(encapsulated.orderChannel).put(encapsulated.orderIndex, encapsulated);
				while (handleQueue.get(encapsulated.orderChannel)
						.containsKey(orderReceiveIndex[encapsulated.orderChannel])) {
					this.handleMessage0(encapsulated.orderChannel,
							new RakNetPacket(handleQueue.get(encapsulated.orderChannel)
									.remove(orderReceiveIndex[encapsulated.orderChannel]++).payload));
				}
			} else if (encapsulated.reliability.isSequenced()
					&& encapsulated.orderIndex > sequenceReceiveIndex[encapsulated.orderChannel]) {
				sequenceReceiveIndex[encapsulated.orderChannel] = encapsulated.orderIndex;
				this.handleMessage0(encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
			} else {
				this.handleMessage0(encapsulated.orderChannel, new RakNetPacket(encapsulated.payload));
			}
		}
		log.debug("Handled " + (encapsulated.split ? "split " : "") + "encapsulated packet with "
				+ encapsulated.reliability + " reliability on channel " + encapsulated.orderChannel);
	}

	/**
	 * Handles an internal packet.
	 * <p>
	 * If the ID is unrecognized it is passed on to the extending peer class via
	 * the {@link handleMessage(int, RakNetPacket)} method.
	 * 
	 * @param channel
	 *            the channel the packet was sent on.
	 * @param packet
	 *            the packet.
	 * @throws InvalidChannelException
	 *             if the <code>channel</code> is greater than or equal to
	 *             {@value RakNet#MAX_CHANNELS}.
	 * @throws NullPointerException
	 *             if the <code>packet</code> is <code>null</code>.
	 */
	private final void handleMessage0(int channel, RakNetPacket packet)
			throws InvalidChannelException, NullPointerException {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		} else if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		}
		if (packet.getId() == ID_CONNECTED_PING) {
			ConnectedPing ping = new ConnectedPing(packet);
			ping.decode();

			ConnectedPong pong = new ConnectedPong();
			pong.timestamp = ping.timestamp;
			pong.timestampPong = this.getTimestamp();
			pong.encode();
			this.sendMessage(Reliability.UNRELIABLE, pong);
		} else if (packet.getId() == ID_CONNECTED_PONG) {
			ConnectedPong pong = new ConnectedPong(packet);
			pong.decode();

			// Calculate latency
			if (latencyEnabled == true && latencyTimestamps.contains(pong.timestamp)) {
				latencyTimestamps.remove(pong.timestamp);
				long responseTime = lastPacketReceiveTime - lastPingSendTime;
				this.lastLatency = responseTime;
				if (this.pongsReceived == 0) {
					this.lowestLatency = responseTime;
					this.highestLatency = responseTime;
				} else if (responseTime < lowestLatency) {
					this.lowestLatency = responseTime;
				} else if (responseTime > highestLatency) {
					this.highestLatency = responseTime;
				}
				this.totalLatency += responseTime;
				this.latency = totalLatency / ++pongsReceived;
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
		log.debug("Handled packet with ID " + RakNetPacket.getName(packet.getId()));
	}

	/**
	 * Sends a message over the channel raw.
	 * <p>
	 * This will automatically update the <code>lastPacketSendTime</code> and
	 * <code>packetsSentThisSecond</code> variable.
	 * 
	 * @param buf
	 *            the buffer.
	 * @throws NullPointerException
	 *             if the <code>buf</code> is <code>null</code>.
	 */
	public final void sendNettyMessage(ByteBuf buf) throws NullPointerException {
		if (buf == null) {
			throw new NullPointerException("Buffer cannot be null");
		}
		channel.writeAndFlush(new DatagramPacket(buf, address));
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastPacketsSentThisSecondResetTime >= 1000L) {
			this.packetsSentThisSecond = 0;
			this.lastPacketsSentThisSecondResetTime = currentTime;
		}
		this.lastPacketSendTime = currentTime;
		this.packetsSentThisSecond++;
		log.debug("Sent netty message with size of " + buf.capacity() + " bytes (" + (buf.capacity() * 8) + " bits) to "
				+ address);
	}

	/**
	 * Sends a message over the channel raw.
	 * <p>
	 * This will automatically update the <code>lastPacketSendTime</code> and
	 * <code>packettSentThisSecond</code> variable.
	 * 
	 * @param packet
	 *            the packet.
	 * @throws NullPointerException
	 *             if the <code>packet</code> is <code>null</code>.
	 */
	public final void sendNettyMessage(Packet packet) throws NullPointerException {
		if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		}
		this.sendNettyMessage(packet.buffer());
	}

	/**
	 * Sends a {@link CustomFourPacket} to the peer with the specified
	 * {@link EncapsulatedPacket encapsulated packets}.
	 * 
	 * @param updateRecoveryQueue
	 *            <code>true</code> if the the encapsulated packets should be
	 *            stored in the recovery queue for later, <code>false</code>
	 *            otherwise. This should only be <code>true</code> when sending
	 *            a group of packets for the first time, rather than resending
	 *            old data that the peer has reported to be lost in transmision.
	 * @param encapsulated
	 *            the packets to send.
	 * @return the sequence number of the {@link CustomFourPacket}.
	 * @throws NullPointerException
	 *             if the <code>messages</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>messages</code> array is empty.
	 */
	private final int sendCustomPacket(boolean updateRecoveryQueue, EncapsulatedPacket... messages)
			throws NullPointerException, IllegalArgumentException {
		if (messages == null) {
			throw new NullPointerException("Messages cannot be null");
		} else if (messages.length <= 0) {
			throw new IllegalArgumentException("There must be a message to send");
		}

		// Encode custom packet
		CustomFourPacket custom = new CustomFourPacket();
		custom.sequenceId = sendSequenceNumber++;
		custom.messages = messages;
		custom.encode();

		// Save packets that require acknowledgement receipts for later
		for (EncapsulatedPacket packet : custom.ackMessages) {
			EncapsulatedPacket clone = packet.getClone();
			if (!clone.reliability.requiresAck()) {
				throw new IllegalArgumentException("Invalid reliability " + packet.reliability);
			}
			clone.ackRecord = packet.ackRecord;
			ackReceiptPackets.put(clone, clone.ackRecord.getIndex());
		}

		// Send packet
		this.sendNettyMessage(custom);
		if (updateRecoveryQueue == true) {
			ArrayList<EncapsulatedPacket> reliable = new ArrayList<EncapsulatedPacket>();
			for (EncapsulatedPacket packet : custom.messages) {
				if (packet.reliability.isReliable()) {
					reliable.add(packet);
				}
			}
			recoveryQueue.put(custom.sequenceId, reliable.toArray(new EncapsulatedPacket[reliable.size()]));
		}
		log.debug("Sent custom packet containing " + custom.messages.length
				+ " encapsulated packets with sequence number " + custom.sequenceId);
		return custom.sequenceId;
	}

	/**
	 * Sends an {@link Acknowledge ACK} with the specified {@link Record
	 * records}.
	 * 
	 * @param acknowledge
	 *            <code>true</code> if the records inside the packet are
	 *            acknowledged, <code>false</code> if the records are not
	 *            acknowledged.
	 * @param record
	 *            the records to send.
	 */
	private final void sendAcknowledge(boolean acknowledge, Record... records) throws NullPointerException, IllegalArgumentException {
		if(records == null) {
			throw new NullPointerException("Records cannot be null");
		} else if(records.length <= 0) {
			throw new IllegalArgumentException("There must be a record to send");
		}
		
		AcknowledgedPacket acknowledged = acknowledge ? new AcknowledgedPacket() : new NotAcknowledgedPacket();
		acknowledged.records = records;
		acknowledged.encode();
		this.sendNettyMessage(acknowledged);
		log.debug("Sent " + acknowledged.records.length + " record" + (acknowledged.records.length == 1 ? "" : "s")
				+ " in " + (acknowledged.isAcknowledgement() ? "ACK" : "NACK") + " packet");
	}

	@Override
	public final EncapsulatedPacket sendMessage(Reliability reliability, int channel, Packet packet)
			throws NullPointerException, InvalidChannelException {
		if (reliability == null) {
			throw new NullPointerException("Reliability cannot be null");
		} else if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		} else if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}

		// Generate encapsulated packet
		EncapsulatedPacket encapsulated = new EncapsulatedPacket();
		encapsulated.reliability = reliability;
		encapsulated.orderChannel = (byte) channel;
		encapsulated.payload = packet;
		if (reliability.isReliable()) {
			encapsulated.messageIndex = this.bumpMessageIndex();
			log.debug("Bumped message index from " + encapsulated.messageIndex + " to " + messageIndex);
		}
		if (reliability.isOrdered() || reliability.isSequenced()) {
			encapsulated.orderIndex = (reliability.isOrdered() ? orderSendIndex[channel]++
					: sequenceSendIndex[channel]++);
			log.debug("Bumped " + (reliability.isOrdered() ? "order" : "sequence") + " index from "
					+ ((reliability.isOrdered() ? orderSendIndex[channel] : sequenceSendIndex[channel]) - 1) + " to "
					+ (reliability.isOrdered() ? orderSendIndex[channel] : sequenceSendIndex[channel]) + " on channel "
					+ channel);
		}

		// Add to send queue
		if (encapsulated.needsSplit(this)) {
			encapsulated.splitId = ++this.splitId % 65536;
			for (EncapsulatedPacket split : encapsulated.split(this)) {
				sendQueue.add(split);
			}
			log.debug("Split encapsulated packet and added it to the send queue");
		} else {
			sendQueue.add(encapsulated);
			log.debug("Added encapsulated packet to the send queue");
		}
		log.debug("Sent packet with size of " + packet.size() + " bytes (" + (packet.size() * 8)
				+ " bits) with reliability " + reliability + " on channel " + channel);

		/*
		 * Return a copy of the encapsulated packet as if a single variable is
		 * modified in the encapsulated packet before it is sent, the
		 * communication with the peer could cease to function entirely.
		 */
		return encapsulated.getClone();
	}

	/**
	 * Updates the peer.
	 * 
	 * @throws TimeoutException
	 *             if the peer has timed out.
	 */
	public final void update() throws TimeoutException {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastPacketReceiveTime >= RakNet.SESSION_TIMEOUT) {
			throw new TimeoutException();
		}

		// Send keep alive packet
		if (currentTime - lastPacketReceiveTime >= RakNet.DETECTION_SEND_INTERVAL
				&& currentTime - lastKeepAliveSendTime >= RakNet.DETECTION_SEND_INTERVAL
				&& state.equals(RakNetState.LOGGED_IN)) {
			this.sendMessage(Reliability.UNRELIABLE, ID_DETECT_LOST_CONNECTIONS);
			this.lastKeepAliveSendTime = currentTime;
		}

		// Send ping to detect latency if it is enabled
		if (latencyEnabled == true && currentTime - lastPingSendTime >= RakNet.PING_SEND_INTERVAL
				&& state.equals(RakNetState.LOGGED_IN)) {
			ConnectedPing ping = new ConnectedPing();
			ping.timestamp = this.getTimestamp();
			ping.encode();
			this.sendMessage(Reliability.UNRELIABLE, ping);
			this.lastPingSendTime = currentTime;
			latencyTimestamps.add(ping.timestamp);
		}

		// Send next packets in the send queue
		if (!sendQueue.isEmpty() && packetsSentThisSecond < RakNet.getMaxPacketsPerSecond()) {
			ArrayList<EncapsulatedPacket> send = new ArrayList<EncapsulatedPacket>();
			int sendLength = CustomPacket.MINIMUM_SIZE;
			Iterator<EncapsulatedPacket> sendQueueI = sendQueue.iterator();
			while (sendQueueI.hasNext()) {
				EncapsulatedPacket encapsulated = sendQueueI.next();
				sendLength += encapsulated.size();
				if (sendLength > maximumTransferUnit) {
					break; // Adding this packet would cause an overflow
				}
				send.add(encapsulated);
				sendQueueI.remove();
			}
			if (!send.isEmpty()) {
				this.sendCustomPacket(true, send.toArray(new EncapsulatedPacket[send.size()]));
			}
		}

		// Resend lost packets
		Iterator<EncapsulatedPacket[]> recoveryQueueI = recoveryQueue.values().iterator();
		if (currentTime - lastRecoverySendTime >= RakNet.RECOVERY_SEND_INTERVAL && recoveryQueueI.hasNext()) {
			this.sendCustomPacket(false, recoveryQueueI.next());
			this.lastRecoverySendTime = currentTime;
		}
	}

	/**
	 * Called when a not acknowledged receipt is received for an
	 * {@link EncapsulatedPacket}.
	 * <p>
	 * Keep in mind that an {@link EncapsualtedPacket} is not the same as a
	 * regular message (or simply a packet). These have a lot of extra data in
	 * them other than the payload, including split data if the packet is split
	 * up.
	 * <p>
	 * This does not mean they will never arrive. It simply means they were lost
	 * in transmission and need to be resent to the peer. The only time this
	 * means that a packet will never be received is if the packet was sent
	 * using an {@link Reliability#UNRELIABLE UNRELIABLE} reliability.
	 * 
	 * @param record
	 *            the lost record.
	 * @param packet
	 *            the lost packet.
	 */
	public abstract void onNotAcknowledge(Record record, EncapsulatedPacket packet);

	/**
	 * Called when a acknowledge receipt is received for an
	 * {@link EncapsulatedPacket}.
	 * <p>
	 * Keep in mind that an {@link EncapsualtedPacket} is not the same as a
	 * regular message (or simply a packet). These have a lot of extra data in
	 * them other than the payload, including split data if the packet is split
	 * up.
	 * 
	 * @param record
	 *            the received record.
	 * @param packet
	 *            the received packet.
	 */
	public abstract void onAcknowledge(Record record, EncapsulatedPacket packet);

	/**
	 * Called when a packet is received.
	 * 
	 * @param packet
	 *            the packet to handle.
	 * @param channel
	 *            the packet the channel was sent on.
	 */
	public abstract void handleMessage(RakNetPacket packet, int channel);

}
