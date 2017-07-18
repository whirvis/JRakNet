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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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

import java.util.ArrayList;

import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.MessageIdentifier;

public class CustomPacket extends RakNetPacket {

	public static final int SEQUENCE_NUMBER_LENGTH = 0x03;

	public int sequenceNumber;
	public ArrayList<EncapsulatedPacket> messages;

	public CustomPacket() {
		super(MessageIdentifier.ID_CUSTOM_4);
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

	/**
	 * @return the size of the packet would be if it had been encoded.
	 */
	public int calculateSize() {
		int packetSize = 1; // Packet ID
		packetSize += SEQUENCE_NUMBER_LENGTH;
		for (EncapsulatedPacket message : this.messages) {
			packetSize += message.calculateSize();
		}
		return packetSize;
	}

	/**
	 * @return true if the packet contains any unreliable messages.
	 */
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

	/**
	 * Removes all the unreliable messages from the packet.
	 */
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

	/**
	 * @return the size of a <code>CustomPacket</code> without any extra data
	 *         written to it.
	 */
	public static int calculateDummy() {
		CustomPacket custom = new CustomPacket();
		custom.encode();
		return custom.size();
	}

}
