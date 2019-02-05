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
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;

import io.netty.buffer.Unpooled;

/**
 * Used by <code>RakNetSession</code> to properly send data to connected clients
 * and servers.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class EncapsulatedPacket implements Sizable, Cloneable {

	private static final Logger log = LoggerFactory.getLogger(EncapsulatedPacket.class);

	// Length constants
	public static final int MINIMUM_BUFFER_LENGTH = 3;
	public static final int BITFLAG_LENGTH = 1;
	public static final int PAYLOAD_LENGTH_LENGTH = 2;
	public static final int MESSAGE_INDEX_LENGTH = 3;
	public static final int ORDER_INDEX_ORDER_CHANNEL_LENGTH = 4;
	public static final int SPLIT_COUNT_SPLIT_ID_SPLIT_INDEX_LENGTH = 10;

	// Bitflags
	public static final byte RELIABILITY_POSITION = (byte) 0b00000101;
	public static final byte FLAG_RELIABILITY = (byte) 0b11100000;
	public static final byte FLAG_SPLIT = (byte) 0b00010000;

	/*
	 * Used for encoding and decoding, should be modified by CustomPacket and
	 * RakNetSession only
	 */
	protected Packet buffer = new Packet();
	private EncapsulatedPacket clone = null;
	private boolean isClone = false;

	/**
	 * If the reliability requires an ACK receipt (The name ends with
	 * <code>_WITH_ACK_RECEIPT</code>) then this can be used to determine if
	 * this is the packet that was received or lost once you are notified
	 * through <code>onAcknowledge()</code> or
	 * <code>onNotAcknowledge()</code>.<br>
	 * <br>
	 * This will <i>always</i> be <code>null</code> before and after the two
	 * notifier methods stated above are called. This is due to the fact that
	 * the ACK record for an encapsulated packet can change as the custom packet
	 * that sends it might not ever arrive, causing it to be resent in another
	 * custom packet with another sequence ID (ACK record), causing it to be
	 * changed. Because of this, it is recommended to only read data from the
	 * packet when either the <code>onAcknowledge()</code> or
	 * <code>onNotAcknowledge()</code> methods are called.
	 */
	public Record ackRecord = null;

	// Encapsulation data
	public Reliability reliability;
	public boolean split;
	public int messageIndex;
	public int orderIndex;
	public byte orderChannel;
	public int splitCount;
	public int splitId;
	public int splitIndex;
	public Packet payload;

	/**
	 * @return the cloned object of the packet.
	 */
	public EncapsulatedPacket getClone() {
		return this.clone;
	}

	/**
	 * Encodes the packet.
	 */
	public void encode() {
		buffer.writeByte((byte) ((reliability.getId() << RELIABILITY_POSITION) | (split ? FLAG_SPLIT : 0)));
		buffer.writeUnsignedShort(payload.size() * 8); // Size is in bits

		if (reliability.requiresAck() && ackRecord == null) {
			log.error("No ACK record ID set for encapsulated packet with reliability " + reliability);
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
	 */
	public void decode() {
		short flags = buffer.readUnsignedByte();
		this.reliability = Reliability.lookup((byte) ((flags & FLAG_RELIABILITY) >> RELIABILITY_POSITION));
		this.split = (flags & FLAG_SPLIT) > 0;
		int length = buffer.readUnsignedShort() / 8; // Size is in bits

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

		this.payload = new Packet(Unpooled.copiedBuffer(buffer.read(length)));
	}

	@Override
	public int calculateSize() {
		int packetSize = 0;
		packetSize += BITFLAG_LENGTH;
		packetSize += PAYLOAD_LENGTH_LENGTH;

		if (reliability.isReliable()) {
			packetSize += MESSAGE_INDEX_LENGTH;
		}

		if (reliability.isOrdered() || reliability.isSequenced()) {
			packetSize += ORDER_INDEX_ORDER_CHANNEL_LENGTH;
		}

		if (split == true) {
			packetSize += SPLIT_COUNT_SPLIT_ID_SPLIT_INDEX_LENGTH;
		}

		packetSize += payload.array().length;
		return packetSize;
	}

	@Override
	public EncapsulatedPacket clone() {
		try {
			// Make sure clone is valid
			if (this.clone != null) {
				throw new CloneNotSupportedException("EncapsulatedPackets can only be cloned once");
			} else if (this.isClone == true) {
				throw new CloneNotSupportedException("Clones of EncapsulatedPackets cannot be cloned");
			}

			// Create clone and return it
			this.clone = (EncapsulatedPacket) super.clone();
			clone.isClone = true;
			return this.clone;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param reliability
	 *            the reliability of the packet.
	 * @param split
	 *            whether or not the packet is split.
	 * @param payload
	 *            the payload of the packet
	 * @return the size of an <code>EncapsulatedPacket</code> based on the
	 *         specified reliability, whether or not it is split, and the size
	 *         of the specified payload without any extra data written to it.
	 */
	public static int calculateDummy(Reliability reliability, boolean split, Packet payload) {
		EncapsulatedPacket dummy = new EncapsulatedPacket();
		dummy.ackRecord = new Record(0);
		dummy.reliability = reliability;
		dummy.payload = payload;
		dummy.split = true;
		dummy.encode();
		return dummy.buffer.size();
	}

	/**
	 * @param reliability
	 *            the reliability of the packet.
	 * @param split
	 *            whether or not the packet is split.
	 * @return the size of an <code>EncapsulatedPacket</code> based on the
	 *         specified reliability and whether or not it is split without any
	 *         extra data written to it.
	 */
	public static int calculateDummy(Reliability reliability, boolean split) {
		return EncapsulatedPacket.calculateDummy(reliability, split, new Packet());
	}

	@Override
	public String toString() {
		return "EncapsulatedPacket [isClone=" + isClone + ", ackRecord=" + ackRecord + ", reliability=" + reliability
				+ ", split=" + split + ", messageIndex=" + messageIndex + ", orderIndex=" + orderIndex
				+ ", orderChannel=" + orderChannel + ", splitCount=" + splitCount + ", splitId=" + splitId
				+ ", splitIndex=" + splitIndex + ", calculateSize()=" + calculateSize() + "]";
	}

}
