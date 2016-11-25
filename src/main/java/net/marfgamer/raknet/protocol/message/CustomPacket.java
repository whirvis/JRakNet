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
package net.marfgamer.raknet.protocol.message;

import java.util.ArrayList;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class CustomPacket extends RakNetPacket {

	public static final short ID_CUSTOM_0 = 0x80;
	public static final short ID_CUSTOM_1 = 0x81;
	public static final short ID_CUSTOM_2 = 0x82;
	public static final short ID_CUSTOM_3 = 0x83;
	public static final short ID_CUSTOM_4 = 0x84;
	public static final short ID_CUSTOM_5 = 0x85;
	public static final short ID_CUSTOM_6 = 0x86;
	public static final short ID_CUSTOM_7 = 0x87;
	public static final short ID_CUSTOM_8 = 0x88;
	public static final short ID_CUSTOM_9 = 0x89;
	public static final short ID_CUSTOM_A = 0x8A;
	public static final short ID_CUSTOM_B = 0x8B;
	public static final short ID_CUSTOM_C = 0x8C;
	public static final short ID_CUSTOM_D = 0x8D;
	public static final short ID_CUSTOM_E = 0x8E;
	public static final short ID_CUSTOM_F = 0x8F;

	public static final int SEQUENCE_NUMBER_LENGTH = 0x03;

	public int sequenceNumber;
	public ArrayList<EncapsulatedPacket> messages;

	public CustomPacket() {
		super(MessageIdentifier.ID_RESERVED_4);
		this.messages = new ArrayList<EncapsulatedPacket>();
	}

	public CustomPacket(Packet packet) {
		super(packet);
		this.messages = new ArrayList<EncapsulatedPacket>();
	}

	@Override
	public void encode() {
		this.writeTriadLE(sequenceNumber);
		for (EncapsulatedPacket packet : messages) {
			// Encode packet and write to buffer
			packet.buffer = this;
			packet.encode();

			// Buffer is no longer needed, proceed
			packet.buffer = null;
		}
	}

	@Override
	public void decode() {
		this.sequenceNumber = this.readTriadLE();
		while (this.remaining() >= EncapsulatedPacket.MINIMUM_BUFFER_LENGTH) {
			// Decode packet
			EncapsulatedPacket packet = new EncapsulatedPacket();
			packet.buffer = new Packet(this.buffer());
			packet.decode();

			// Buffer is no longer needed, add the packet to the list
			packet.buffer = null;
			messages.add(packet);
		}
	}

	public int calculateSize() {
		int packetSize = 1; // Packet ID
		packetSize += SEQUENCE_NUMBER_LENGTH;
		for (EncapsulatedPacket message : this.messages) {
			packetSize += message.calculateSize();
		}
		return packetSize;
	}

	public boolean containsUnreliables() {
		if (messages.size() <= 0) {
			return false; // Nothing to check
		}

		for (EncapsulatedPacket encapsulated : this.messages) {
			if (!encapsulated.reliability.isReliable()) {
				return true;
			}
		}
		return false;
	}

	public void removeUnreliables() {
		if (messages.size() <= 0) {
			return; // Nothing to remove
		}

		ArrayList<EncapsulatedPacket> unreliables = new ArrayList<EncapsulatedPacket>();
		for (EncapsulatedPacket encapsulated : this.messages) {
			if (!encapsulated.reliability.isReliable()) {
				unreliables.add(encapsulated);
			}
		}
		messages.removeAll(unreliables);
	}

	public static int calculateDummy() {
		CustomPacket custom = new CustomPacket();
		custom.encode();
		return custom.size();
	}

}
