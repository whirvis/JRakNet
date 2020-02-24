/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 "Whirvis" Trent Summerlin
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
package com.whirvis.jraknet.protocol.message;

import java.util.Arrays;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.map.IntMap;
import com.whirvis.jraknet.peer.RakNetPeer;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;

/**
 * An encapsulated packet.
 * <p>
 * These packets are sent within {@link CustomPacket CUSTOM} packets after
 * initial connection has succeeded between the server and client.
 *
 * @author "Whirvis" Trent Summerlin
 * @since JRakNet v1.0.0
 */
public final class EncapsulatedPacket implements Cloneable {

	/**
	 * Used to easily split and reassemble {@link EncapsulatedPacket
	 * encapsulated packets}.
	 * 
	 * @author "Whirvis" Trent Summerlin
	 * @since JRakNet v1.0.0
	 */
	public static final class Split {

		/**
		 * Returns whether or not the packet needs to be split.
		 * 
		 * @param peer
		 *            the peer the packet is being sent to.
		 * @param encapsulated
		 *            the encapsulated packet.
		 * @return <code>true</code> if the packet needs to be split,
		 *         <code>false</code> otherwise.
		 * @throws NullPointerException
		 *             if the <code>peer</code> or <code>encapsulated</code> is
		 *             <code>null</code>.
		 * @throws IllegalArgumentException
		 *             if the <code>encapsulated</code> is already split.
		 */
		public static boolean needsSplit(RakNetPeer peer, EncapsulatedPacket encapsulated)
				throws NullPointerException, IllegalArgumentException {
			if (peer == null) {
				throw new NullPointerException("Peer cannot be null");
			} else if (encapsulated == null) {
				throw new NullPointerException("Encapsulated packet cannot be null");
			} else if (encapsulated.split == true) {
				throw new IllegalArgumentException("Encapsulated packet is already split");
			}
			return CustomPacket.MINIMUM_SIZE + encapsulated.size() > peer.getMaximumTransferUnit();
		}

		/**
		 * Splits the packet.
		 * 
		 * @param peer
		 *            the peer.
		 * @param encapsulated
		 *            the packet to split.
		 * @return the split up encapsulated packet.
		 * 
		 * @throws NullPointerException
		 *             if the <code>peer</code> or <code>encapsulated</code> is
		 *             <code>null</code>.
		 * @throws IllegalArgumentException
		 *             if the <code>encapsulated</code> is already split or if
		 *             the packet is too small to be split according to
		 *             {@link #needsSplit(RakNetPeer, EncapsulatedPacket)}.
		 */
		public static EncapsulatedPacket[] split(RakNetPeer peer, EncapsulatedPacket encapsulated)
				throws NullPointerException, IllegalArgumentException {
			if (peer == null) {
				throw new NullPointerException("Peer cannot be null");
			} else if (encapsulated == null) {
				throw new NullPointerException("Encapsulated packet cannot be null");
			} else if (encapsulated.split == true) {
				throw new NullPointerException("Encapsulated packet is already split");
			} else if (!needsSplit(peer, encapsulated)) {
				throw new IllegalArgumentException("Encapsulated packet is too small to be split");
			}

			// Split packet payload
			int size = peer.getMaximumTransferUnit() - CustomPacket.MINIMUM_SIZE
					- EncapsulatedPacket.size(encapsulated.reliability, true);
			byte[] src = encapsulated.payload.array();
			int payloadIndex = 0;
			int splitIndex = 0;
			byte[][] split = new byte[(int) Math.ceil((float) src.length / (float) size)][size];
			while (payloadIndex < src.length) {
				if (payloadIndex + size <= src.length) {
					split[splitIndex++] = Arrays.copyOfRange(src, payloadIndex, payloadIndex + size);
					payloadIndex += size;
				} else {
					split[splitIndex++] = Arrays.copyOfRange(src, payloadIndex, src.length);
					payloadIndex = src.length;
				}
			}

			// Generate split encapsulated packets
			EncapsulatedPacket[] splitPackets = new EncapsulatedPacket[split.length];
			for (int i = 0; i < split.length; i++) {
				EncapsulatedPacket encapsulatedSplit = new EncapsulatedPacket();
				encapsulatedSplit.reliability = encapsulated.reliability;
				encapsulatedSplit.payload = new Packet(split[i]);
				encapsulatedSplit.messageIndex = encapsulated.reliability.isReliable() ? peer.bumpMessageIndex() : 0;
				if (encapsulated.reliability.isOrdered() || encapsulated.reliability.isSequenced()) {
					encapsulatedSplit.orderChannel = encapsulated.orderChannel;
					encapsulatedSplit.orderIndex = encapsulated.orderIndex;
				}
				encapsulatedSplit.split = true;
				encapsulatedSplit.splitCount = split.length;
				encapsulatedSplit.splitId = encapsulated.splitId;
				encapsulatedSplit.splitIndex = i;
				splitPackets[i] = encapsulatedSplit;
			}
			return splitPackets;
		}

		private final int splitId;
		private final int splitCount;
		private final Reliability reliability;
		private final IntMap<Packet> payloads;

		/**
		 * Creates a split packet container.
		 * 
		 * @param splitId
		 *            the split ID.
		 * @param splitCount
		 *            the split count.
		 * @param reliability
		 *            the reliability.
		 * @throws IllegalArgumentException
		 *             if the <code>splitId</code> is negative or if the
		 *             <code>splitCount</code> is greater than
		 *             {@value RakNetPeer#MAX_SPLIT_COUNT}.
		 * @throws NullPointerException
		 *             if the <code>reliability</code> is <code>null</code>.
		 */
		public Split(int splitId, int splitCount, Reliability reliability) {
			if (splitId < 0) {
				throw new IllegalArgumentException("Split ID cannot be negative");
			} else if (splitCount > RakNetPeer.MAX_SPLIT_COUNT) {
				throw new IllegalArgumentException("Split count can be no greater than " + RakNetPeer.MAX_SPLIT_COUNT);
			} else if (reliability == null) {
				throw new NullPointerException("Reliability cannot be null");
			}
			this.splitId = splitId;
			this.splitCount = splitCount;
			this.reliability = reliability;
			this.payloads = new IntMap<Packet>();
		}

		/**
		 * Returns the reliability of the split packet.
		 * 
		 * @return the reliability of the split packet.
		 */
		public Reliability getReliability() {
			return this.reliability;
		}

		/**
		 * Updates the data for the split packet while also verifying that the
		 * <code>EncapsulatedPacket</code> belongs to this split packet.
		 * 
		 * @param encapsulated
		 *            the encapsulated packet chunk.
		 * @return the packet if finished, <code>null</code> if data is still
		 *         missing.
		 * @throws NullPointerException
		 *             if the <code>encapsulated</code> is <code>null</code>.
		 * @throws IllegalArgumentException
		 *             if the <code>encapsulated</code> is not part of the
		 *             bigger split packet based on whether or not it is split,
		 *             its <code>splitId</code>, <code>splitCount</code>, or
		 *             <code>reliability</code>, or if the
		 *             <code>splitIndex</code> is less than <code>0</code> or
		 *             greater than or equal to the <code>splitCount</code> of
		 *             the <code>encapsulated</code> packet, or another
		 *             <code>encapsulated</code> packet has been registered with
		 *             the same <code>splitIndex</code>.
		 */
		public EncapsulatedPacket update(EncapsulatedPacket encapsulated)
				throws NullPointerException, IllegalArgumentException {
			if (encapsulated == null) {
				throw new NullPointerException("Encapsulated packet cannot be null");
			} else if (encapsulated.split != true || encapsulated.splitId != splitId
					|| encapsulated.splitCount != splitCount || encapsulated.reliability != reliability) {
				throw new IllegalArgumentException("This split packet does not belong to this one");
			} else if (encapsulated.splitIndex < 0 || encapsulated.splitIndex >= encapsulated.splitCount) {
				throw new IllegalArgumentException("Encapsulated packet split index out of range");
			} else if (payloads.containsKey(encapsulated.splitIndex)) {
				throw new IllegalArgumentException("Encapsulated packet with split index has already been registered");
			}
			payloads.put(encapsulated.splitIndex, encapsulated.payload);
			if (payloads.size() >= splitCount) {
				// Stitch payload
				Packet payload = new Packet();
				for (int i = 0; i < payloads.size(); i++) {
					payload.write(payloads.get(i).array());
				}
				payloads.clear();

				// Create stitched encapsulated packet
				EncapsulatedPacket stitched = new EncapsulatedPacket();
				stitched.ackRecord = null;
				stitched.reliability = encapsulated.reliability;
				stitched.split = false; // No longer split
				stitched.messageIndex = encapsulated.messageIndex;
				stitched.orderIndex = encapsulated.orderIndex;
				stitched.orderChannel = encapsulated.orderChannel;
				stitched.splitCount = encapsulated.splitCount;
				stitched.splitId = encapsulated.splitId;
				stitched.splitIndex = -1; // No longer split
				stitched.payload = payload;
				return stitched;
			}
			return null;
		}

		@Override
		public String toString() {
			return "Split [splitId=" + splitId + ", splitCount=" + splitCount + ", reliability=" + reliability + "]";
		}

	}

	private static final int FLAG_RELIABILITY_INDEX = 5;
	private static final byte FLAG_RELIABILITY = (byte) 0b11100000;
	private static final byte FLAG_SPLIT = (byte) 0b00010000;

	/**
	 * The minimum size of an encapsulated packet.
	 */
	public static final int MINIMUM_SIZE = size(null, false);

	/**
	 * Calculates the size of an encapsulated packet if it had been encoded.
	 * 
	 * @param reliability
	 *            the reliability.
	 * @param split
	 *            <code>true</code> if the packet is split, <code>false</code>
	 *            otherwise.
	 * @param payload
	 *            the payload, <code>null</code> if there is no payload.
	 * @return the size if it had been encoded.
	 */
	public static int size(Reliability reliability, boolean split, Packet payload) {
		int size = 3;
		if (reliability != null) {
			size += reliability.isReliable() ? 3 : 0;
			size += reliability.isOrdered() || reliability.isSequenced() ? 4 : 0;
		}
		size += split == true ? 10 : 0;
		size += payload != null ? payload.size() : 0;
		return size;
	}

	/**
	 * Calculates the size of an encapsulated packet if it had been encoded.
	 * 
	 * @param reliability
	 *            the reliability.
	 * @param split
	 *            <code>true</code> if the packet is split, <code>false</code>
	 *            otherwise.
	 * @return the size.
	 */
	public static int size(Reliability reliability, boolean split) {
		return size(reliability, split, null);
	}

	private boolean isClone;
	private EncapsulatedPacket clone;

	/**
	 * The acknowledgement record. This is only used if the reliability is of
	 * the {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 * type.
	 * <p>
	 * This is <i>not</i> used for packet encoding. Rather, when a sender gets
	 * an
	 * {@link com.whirvis.jraknet.protocol.message.acknowledge.AcknowledgedPacket
	 * ACK} packet, the event method
	 * {@link com.whirvis.jraknet.client.RakNetClientListener#onAcknowledge(com.whirvis.jraknet.client.RakNetClient, com.whirvis.jraknet.peer.RakNetServerPeer, Record, EncapsulatedPacket)
	 * onAcknowledge(RakNetClient, RakNetServerPeer, Record,
	 * EncapsulatedPacket)} is called. Likewise, the same occurs when a
	 * {@link com.whirvis.jraknet.protocol.message.acknowledge.NotAcknowledgedPacket
	 * NACK} packet is received with the exception of
	 * {@link com.whirvis.jraknet.client.RakNetClientListener#onLoss(com.whirvis.jraknet.client.RakNetClient, com.whirvis.jraknet.peer.RakNetServerPeer, Record, EncapsulatedPacket)
	 * onLoss(RakNetClient, RakNetServerPeer, Record, EncapsulatedPacket)} being
	 * called instead.
	 */
	public Record ackRecord;

	/**
	 * The packet reliability.
	 */
	public Reliability reliability;

	/**
	 * Whether or not the packet is part of a bigger split up packet.
	 */
	public boolean split;

	/**
	 * The message index. This is only ever used if the reliability is of the
	 * {@link Reliability#RELIABLE RELIABLE} type. This should always be one
	 * higher than the message index of the last reliable packet that was sent,
	 * as it is used to let the receiver know whether or not they have missed a
	 * reliable packet in transmission. This crucial in order for the
	 * {@link Reliability#RELIABLE_ORDERED RELIABLE_ORDERED} reliability to
	 * function.
	 * <p>
	 * It works like this:
	 * <ul>
	 * <li>1. Sender sends reliable packet one to receiver.</li>
	 * <li>2. Receiver receives reliable packet one.</li>
	 * <li>3. Sender sends reliable packet two to receiver.</li>
	 * <li>4. Receiver does not receive reliable packet two as it is lost in
	 * transmission.</li>
	 * <li>5. Sender sends reliable packet three to receiver.</li>
	 * <li>6. Receiver receives reliable packet three, and realizes that it has
	 * lost reliable packet two, causing it to send a
	 * {@link com.whirvis.jraknet.protocol.message.acknowledge.NotAcknowledgedPacket
	 * NACK} packet.</li>
	 * <li>7. Sender sends reliable packet two again.</li>
	 * <li>8. Receiver receives reliable packet two and communication continues
	 * as normal.</li>
	 * </ul>
	 * <p>
	 * When a receiver receives a reliable packet, it is also always supposed to
	 * send an
	 * {@link com.whirvis.jraknet.protocol.message.acknowledge.AcknowledgedPacket
	 * ACK} packet back to the sender. This lets the sender know that the packet
	 * has been received so it can be removed from its cache. It is assumed that
	 * the receiver sent an
	 * {@link com.whirvis.jraknet.protocol.message.acknowledge.AcknowledgedPacket
	 * ACK} packet in each step describing the fact that it received a reliable
	 * packet.
	 */
	public int messageIndex;

	/**
	 * The order index. This is only ever used if the reliability if of the
	 * {@link Reliability#RELIABLE_ORDERED ORDERED} or
	 * {@link Reliability#UNRELIABLE_SEQUENCED SEQUENCED} type.
	 * <p>
	 * This is similar to the <code>messageIndex</code>, however this is used to
	 * determine when ordered packets are ordered and if a sequenced packet is
	 * the newest one or not. On the other hand, <code>messageIndex</code> is
	 * used to determine when reliable packets have been lost in transmission.
	 */
	public int orderIndex;

	/**
	 * The order index. This is only ever used if the reliability if of the
	 * {@link Reliability#RELIABLE_ORDERED ORDERED} or
	 * {@link Reliability#UNRELIABLE_SEQUENCED SEQUENCED} type.
	 * <p>
	 * In total, there are a total of
	 * {@value com.whirvis.jraknet.RakNet#CHANNEL_COUNT} channels that can be
	 * used to send {@link Reliability#RELIABLE_ORDERED ORDERED} and
	 * {@link Reliability#UNRELIABLE_SEQUENCED SEQUENCED} packets on. Both have
	 * their own set of these channels. It is good to make use of this if there
	 * are many different operations that must be ordered or sequenced happening
	 * at the same time, as it can help prevent clogging.
	 */
	public byte orderChannel;

	/**
	 * The amount of packets the original packet is split up into.
	 */
	public int splitCount;

	/**
	 * The ID the split packet.
	 */
	public int splitId;

	/**
	 * The index of this split part of the packet in the overall packet. This is
	 * used to determine the order in which to put the split packet back
	 * together when each and every part has been received.
	 */
	public int splitIndex;

	/**
	 * The payload.
	 */
	public Packet payload;

	/**
	 * Encodes the packet.
	 * 
	 * @param buffer
	 *            the buffer to write to.
	 * @throws NullPointerException
	 *             if the <code>reliability</code>, <code>payload</code>, or
	 *             <code>buffer</code> are <code>null</code>, or if the
	 *             reliability is reliable and the <code>ackRecord</code> is
	 *             <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>ackRecord</code> is ranged.
	 */
	public void encode(Packet buffer) throws NullPointerException, IllegalArgumentException {
		if (buffer == null) {
			throw new NullPointerException("Buffer cannot be null");
		} else if (reliability == null) {
			throw new NullPointerException("Reliability cannot be null");
		} else if (payload == null) {
			throw new NullPointerException("Payload cannot be null");
		}
		byte flags = 0x00;
		flags |= reliability.getId() << FLAG_RELIABILITY_INDEX;
		flags |= split == true ? FLAG_SPLIT : 0;
		buffer.writeByte(flags);
		buffer.writeUnsignedShort(payload.size() * Byte.SIZE);
		if (ackRecord == null && reliability.requiresAck()) {
			throw new NullPointerException("No ACK record set for encapsulated packet with reliability " + reliability);
		} else if (ackRecord != null) {
			if (ackRecord.isRanged()) {
				throw new IllegalArgumentException("ACK record cannot be ranged");
			}
		}
		if (reliability.isReliable()) {
			buffer.writeTriadLE(messageIndex);
		}
		if (reliability.isOrdered() || reliability.isSequenced()) {
			buffer.writeTriadLE(orderIndex);
			buffer.writeUnsignedByte(orderChannel);
		}
		if (split == true) {
			buffer.writeInt(splitCount);
			buffer.writeUnsignedShort(splitId);
			buffer.writeInt(splitIndex);
		}
		buffer.write(payload.array());
	}

	/**
	 * Decodes the packet.
	 * 
	 * @param buffer
	 *            the buffer to read from.
	 * @throws NullPointerException
	 *             if the <code>buffer</code> is <code>null</code>, or if the
	 *             <code>reliability</code> failed to lookup (normally due to an
	 *             invalid ID).
	 */
	public void decode(Packet buffer) throws NullPointerException {
		if (buffer == null) {
			throw new NullPointerException("Buffer cannot be null");
		}
		short flags = buffer.readUnsignedByte();
		this.reliability = Reliability.lookup((flags & FLAG_RELIABILITY) >> FLAG_RELIABILITY_INDEX);
		if (reliability == null) {
			throw new NullPointerException(
					"Failed to lookup reliability with ID " + ((flags & FLAG_RELIABILITY) >> FLAG_RELIABILITY_INDEX));
		}
		this.split = (flags & FLAG_SPLIT) > 0;
		int length = buffer.readUnsignedShort() / Byte.SIZE;
		if (reliability.isReliable()) {
			this.messageIndex = buffer.readTriadLE();
		}
		if (reliability.isOrdered() || reliability.isSequenced()) {
			this.orderIndex = buffer.readTriadLE();
			this.orderChannel = buffer.readByte();
		}
		if (split == true) {
			this.splitCount = buffer.readInt();
			this.splitId = buffer.readUnsignedShort();
			this.splitIndex = buffer.readInt();
		}
		this.payload = new Packet(buffer.read(length));
	}

	/**
	 * Calculates the size of the packet if it had been encoded.
	 * 
	 * @return the size of the packet if it had been encoded.
	 */
	public int size() {
		return size(reliability, split, payload);
	}

	/**
	 * Returns whether or not the packet needs to be split.
	 * 
	 * @param peer
	 *            the peer the packet is being sent to.
	 * @return <code>true</code> if the packet needs to be split,
	 *         <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>peer</code> is <code>null</code>.
	 */
	public boolean needsSplit(RakNetPeer peer) throws NullPointerException {
		return Split.needsSplit(peer, this);
	}

	/**
	 * Splits the packet.
	 * 
	 * @param peer
	 *            the peer.
	 * @return the split up encapsulated packet.
	 * @throws IllegalStateException
	 *             if the <code>encapsulated</code> is already split or if the
	 *             packet is too small to be split according to
	 *             {@link #needsSplit(RakNetPeer)}.
	 * @throws NullPointerException
	 *             if the <code>peer</code> is <code>null</code>.
	 */
	public EncapsulatedPacket[] split(RakNetPeer peer) throws IllegalStateException, NullPointerException {
		if (split == true) {
			throw new IllegalStateException("Already split");
		} else if (!needsSplit(peer)) {
			throw new IllegalStateException("Too small to be split");
		}
		return Split.split(peer, this);
	}

	/**
	 * Returns the cloned packet. If the packet has not yet been cloned, the
	 * {@link #clone()} method will be called automatically. This method is
	 * recommended for use over the {@link #clone()} method as it has checks put
	 * in place to prevent a <code>CloneNotSupporteException</code> from being
	 * thrown.
	 * 
	 * @return the cloned packet, or the packet itself if it is the clone
	 *         already.
	 * @throws RuntimeException
	 *             if a <code>CloneNotSupportedException</code> is caught
	 *             despite the safe checks put in place.
	 */
	public EncapsulatedPacket getClone() throws RuntimeException {
		if (isClone == true) {
			return this;
		} else if (clone == null) {
			try {
				this.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}
		return this.clone;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws CloneNotSupportedException
	 *             if the packet has already been cloned or if the packet is a
	 *             clone.
	 * @see #getClone()
	 */
	@Override
	public EncapsulatedPacket clone() throws CloneNotSupportedException {
		if (clone != null) {
			throw new CloneNotSupportedException("Encapsulated packets can only be cloned once");
		} else if (isClone == true) {
			throw new CloneNotSupportedException("Clones of encapsulated packets cannot be cloned");
		}
		this.clone = (EncapsulatedPacket) super.clone();
		clone.isClone = true;
		return this.clone;
	}

	@Override
	public String toString() {
		return "EncapsulatedPacket [isClone=" + isClone + ", ackRecord=" + ackRecord + ", reliability=" + reliability
				+ ", split=" + split + ", messageIndex=" + messageIndex + ", orderIndex=" + orderIndex
				+ ", orderChannel=" + orderChannel + ", splitCount=" + splitCount + ", splitId=" + splitId
				+ ", splitIndex=" + splitIndex + ", calculateSize()=" + size() + "]";
	}

}
