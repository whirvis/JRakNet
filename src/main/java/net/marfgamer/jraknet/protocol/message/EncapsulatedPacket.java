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
package net.marfgamer.jraknet.protocol.message;

import io.netty.buffer.Unpooled;
import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.protocol.Reliability;

public class EncapsulatedPacket {

    // Length constants
    public static final int MINIMUM_BUFFER_LENGTH = 0x03;
    public static final int BITFLAG_LENGTH = 0x01;
    public static final int PAYLOAD_LENGTH_LENGTH = 0x02;
    public static final int MESSAGE_INDEX_LENGTH = 0x03;
    public static final int ORDER_INDEX_ORDER_CHANNEL_LENGTH = 0x04;
    public static final int SPLIT_COUNT_SPLIT_ID_SPLIT_INDEX_LENGTH = 0x0A;

    // Bitflags
    public static final byte RELIABILITY_POSITION = (byte) 0b00000101;
    public static final byte FLAG_RELIABILITY = (byte) 0b11100000;
    public static final byte FLAG_SPLIT = (byte) 0b00010000;

    // Used to encode and decode, modified ONLY by CustomPacket
    public Packet buffer = new Packet();

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
     * Encodes the packet
     */
    public void encode() {
	buffer.writeByte((byte) ((reliability.asByte() << RELIABILITY_POSITION) | (split ? FLAG_SPLIT : 0)));
	buffer.writeUShort(payload.size() * 8);

	if (reliability.isReliable()) {
	    buffer.writeTriadLE(messageIndex);
	}

	if (reliability.isOrdered() || reliability.isSequenced()) {
	    buffer.writeTriadLE(orderIndex);
	    buffer.writeUByte(orderChannel);
	}

	if (split == true) {
	    buffer.writeInt(splitCount);
	    buffer.writeUShort(splitId);
	    buffer.writeInt(splitIndex);
	}

	buffer.write(payload.array());
    }

    /**
     * Decodes the packet
     */
    public void decode() {
	byte flags = buffer.readByte();
	this.reliability = Reliability.lookup((byte) (byte) ((flags & 0b11100000) >> 5));
	this.split = (flags & FLAG_SPLIT) > 0;
	int length = buffer.readUShort() / 8;

	if (reliability.isReliable()) {
	    this.messageIndex = buffer.readTriadLE();
	}

	if (reliability.isOrdered() || reliability.isSequenced()) {
	    this.orderIndex = buffer.readTriadLE();
	    this.orderChannel = buffer.readByte();
	}

	if (split == true) {
	    this.splitCount = buffer.readInt();
	    this.splitId = buffer.readUShort();
	    this.splitIndex = buffer.readInt();
	}

	this.payload = new Packet(Unpooled.copiedBuffer(buffer.read(length)));
    }

    /**
     * Calculates what the size of the packet would be if it had been encoded
     * 
     * @return What the size of the packet would be if it had been encoded
     */
    public int calculateSize() {
	int packetSize = 0; // Unlike CustomPacket EncapsulatedPacket has no ID,
			    // so this starts at 0 instead of 1
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

    /**
     * Calculates the size of an <code>EncapsulatedPacket</code> based on the
     * specified reliability, whether or not it is split, and the size of the
     * specified payload without any extra data written to it
     * 
     * @param reliability
     *            The reliability of the packet
     * @param split
     *            Whether or not the packet is split
     * @param payload
     *            The payload of the packet
     * @return The size of an <code>EncapsulatedPacket</code> based on the
     *         specified reliability, whether or not it is split, and the size
     *         of the specified payload without any extra data written to it
     */
    public static int calculateDummy(Reliability reliability, boolean split, Packet payload) {
	EncapsulatedPacket dummy = new EncapsulatedPacket();
	dummy.reliability = reliability;
	dummy.payload = payload;
	dummy.split = true;
	dummy.encode();
	return dummy.buffer.size();
    }

    /**
     * Calculates the size of an <code>EncapsulatedPacket</code> based on the
     * specified reliability and whether or not it is split without any extra
     * data written to it
     * 
     * @param reliability
     *            The reliability of the packet
     * @param split
     *            Whether or not the packet is split
     * @return The size of an <code>EncapsulatedPacket</code> based on the
     *         specified reliability and whether or not it is split without any
     *         extra data written to it
     */
    public static int calculateDummy(Reliability reliability, boolean split) {
	return EncapsulatedPacket.calculateDummy(reliability, split, new Packet());
    }

}
