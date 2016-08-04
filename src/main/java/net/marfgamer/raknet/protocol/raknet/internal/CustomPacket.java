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
 * Copyright (c) 2016 Whirvis T. Wheatley
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

import java.util.ArrayList;

import net.marfgamer.raknet.protocol.Message;

public class CustomPacket extends Message {
	
	public static final int HEADER_LENGTH = 4;

	/**
	 * This is handled by the ClientSession class
	 */
	public int seqNumber;
	public ArrayList<EncapsulatedPacket> packets;

	public CustomPacket(Message packet) {
		super(packet);
		this.packets = new ArrayList<EncapsulatedPacket>();
	}

	public CustomPacket() {
		super(ID_CUSTOM_4);
		this.packets = new ArrayList<EncapsulatedPacket>();
	}

	@Override
	public void encode() {
		this.putLTriad(seqNumber);
		for (EncapsulatedPacket packet : packets) {
			packet.encode(this);
		}
	}

	@Override
	public void decode() {
		this.seqNumber = this.getLTriad();
		while (this.remaining() >= EncapsulatedPacket.HEADER_LENGTH) {
			EncapsulatedPacket encapsulated = new EncapsulatedPacket();
			encapsulated.decode(this);
			packets.add(encapsulated);
		}
	}

}
