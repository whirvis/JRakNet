/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
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
package com.whirvis.jraknet.protocol.message;

import java.util.ArrayList;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;

/**
 * A <code>CUSTOM_0</code>, <code>CUSTOM_1</code>, <code>CUSTOM_2</code>,
 * <code>CUSTOM_3</code>, <code>CUSTOM_4</code>, <code>CUSTOM_5</code>,
 * <code>CUSTOM_6</code>, <code>CUSTOM_7</code>, <code>CUSTOM_8</code>,
 * <code>CUSTOM_9</code>, <code>CUSTOM_A</code>, <code>CUSTOM_B</code>,
 * <code>CUSTOM_C</code>, <code>CUSTOM_D</code>, <code>CUSTOM_E</code>, or
 * <code>CUSTOM_F</code> packet.
 * <p>
 * This packet is used to send {@link EncapsulatedPacket encapsulated packets}
 * that are in the send queue. This is where {@link EncapsulatedPacket
 * encapsulated packets} get their name from, as they are encapsulated within
 * another container packet. The way these are used is by storing as many
 * packets in the send queue as possible into one packet before sending them off
 * all at once.
 * 
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public class CustomPacket extends RakNetPacket {

	/**
	 * The minimum size of a custom packet.
	 */
	public static final int MINIMUM_SIZE = size((EncapsulatedPacket[]) null);

	/**
	 * Calculates the size of the packet if it had been encoded.
	 * 
	 * @param packets the packets inside the custom packet.
	 * @return the size of the packet if it had been encoded.
	 */
	public static int size(EncapsulatedPacket... packets) {
		int size = 4;
		if (packets != null) {
			for (EncapsulatedPacket packet : packets) {
				size += packet.size();
			}
		}
		return size;
	}

	/**
	 * The sequence ID of the packet.
	 */
	public int sequenceId;

	/**
	 * If encoding, these are the packets that will be encoded into the packet. <br>
	 * If decoding, these are the packets decoded from the packet.
	 */
	public EncapsulatedPacket[] messages;

	/**
	 * The encapsulated packets that require acknowledgement.
	 */
	public EncapsulatedPacket[] ackMessages;

	/**
	 * Creates a custom packet to be encoded.
	 * 
	 * @param type the type of custom packet being in between
	 *             <code>ID_CUSTOM_0</code> and <code>ID_CUSTOM_F</code>.
	 * @throws IllegalArgumentException if the <code>type</code> is not in between
	 *                                  code <code>ID_CUSTOM_0</code> and
	 *                                  <code>ID_CUSTOM_F</code>.
	 * @see #encode()
	 */
	protected CustomPacket(int type) throws IllegalArgumentException {
		super(type);
		if (type < ID_CUSTOM_0 || type > ID_CUSTOM_F) {
			throw new IllegalArgumentException("Custom packet ID must be in between ID_CUSTOM_0 and ID_CUSTOM_F");
		}
	}

	/**
	 * Creates a <code>CUSTOM</code> packet to be decoded.
	 * 
	 * @param packet the original packet whose data will be read from in the
	 *               {@link #decode()} method.
	 */
	public CustomPacket(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeTriadLE(sequenceId);
		if (messages != null) {
			ArrayList<EncapsulatedPacket> ackMessages = new ArrayList<EncapsulatedPacket>();
			for (EncapsulatedPacket packet : messages) {
				if (packet.reliability.requiresAck()) {
					packet.ackRecord = new Record(sequenceId);
					ackMessages.add(packet);
				}
				packet.encode(this);
			}
			this.ackMessages = ackMessages.toArray(new EncapsulatedPacket[ackMessages.size()]);
		}
	}

	@Override
	public void decode() {
		this.sequenceId = this.readTriadLE();
		ArrayList<EncapsulatedPacket> messages = new ArrayList<EncapsulatedPacket>();
		ArrayList<EncapsulatedPacket> ackMessages = new ArrayList<EncapsulatedPacket>();
		while (this.remaining() >= EncapsulatedPacket.MINIMUM_SIZE) {
			EncapsulatedPacket packet = new EncapsulatedPacket();
			packet.decode(this);
			if (packet.reliability.requiresAck()) {
				packet.ackRecord = new Record(sequenceId);
				ackMessages.add(packet);
			}
			messages.add(packet);
		}
		this.ackMessages = ackMessages.toArray(new EncapsulatedPacket[ackMessages.size()]);
		this.messages = messages.toArray(new EncapsulatedPacket[messages.size()]);
	}

}
