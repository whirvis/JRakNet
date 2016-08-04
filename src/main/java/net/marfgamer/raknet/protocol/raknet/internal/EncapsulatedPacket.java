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
package net.marfgamer.raknet.protocol.raknet.internal;

import io.netty.buffer.Unpooled;
import net.marfgamer.raknet.protocol.Message;
import net.marfgamer.raknet.protocol.Reliability;

public class EncapsulatedPacket {
	
	public static final int HEADER_LENGTH = 4;
	
	// Binary flag data
	public static final byte FLAG_RELIABILITY = (byte) 0xF4;
	public static final byte FLAG_SPLIT = (byte) 0x10;

	// Encapsulation data
	public Reliability reliability;
	public boolean split;

	// Reliability data
	public int messageIndex;

	// Order data
	public int orderIndex;
	public int orderChannel;

	// Split data
	public int splitCount;
	public int splitId;
	public int splitIndex;

	// Packet payload
	public byte[] payload;

	public void encode(CustomPacket packet) {
		packet.putByte((byte) ((reliability.asByte() << 5) | (split ? FLAG_SPLIT : 0)));
		packet.putShort((payload.length * 8) & 0xFFFF);

		if (reliability.isReliable()) {
			packet.putLTriad(messageIndex);
		}

		if (reliability.isOrdered() || reliability.isSequenced()) {
			packet.putLTriad(orderIndex);
			packet.putByte(orderChannel);
		}

		if (split) {
			packet.putInt(splitCount);
			packet.putShort(splitId & 0xFFFF);
			packet.putInt(splitIndex);
		}

		packet.put(payload);
	}

	public void decode(CustomPacket packet) {
		short flags = packet.getUByte();
		this.reliability = Reliability.lookup((byte) ((flags & FLAG_RELIABILITY) >> 5));
		this.split = (flags & FLAG_SPLIT) > 0;
		int length = (packet.getUShort() / 8);

		if (reliability.isReliable()) {
			this.messageIndex = packet.getLTriad();
		}

		if (reliability.isOrdered() || reliability.isSequenced()) {
			this.orderIndex = packet.getLTriad();
			this.orderChannel = packet.getUByte();
		}

		if (split) {
			this.splitCount = packet.getInt();
			this.splitId = packet.getShort();
			this.splitIndex = packet.getInt();
		}

		this.payload = packet.get(length);
	}

	public Message message() {
		return new Message(Unpooled.copiedBuffer(payload));
	}

}
