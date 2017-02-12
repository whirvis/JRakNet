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
package net.marfgamer.jraknet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;

/**
 * A generic RakNet packet that has the ability to get the ID of the packet
 * along with encoding and decoding
 *
 * @author MarfGamer
 */
public class RakNetPacket extends Packet {

	private short id;

	public RakNetPacket(int id) {
		super();
		if (id < 0) {
			throw new IllegalArgumentException("The packet ID is unsigned, it must be at least 0");
		}
		this.writeUByte(this.id = (short) id);
	}

	public RakNetPacket(ByteBuf buffer) {
		super(buffer);
		if (this.remaining() < 1) {
			throw new IllegalArgumentException("The packet contains no data, it has no ID to be read");
		}
		this.id = this.readUByte();
	}

	public RakNetPacket(DatagramPacket datagram) {
		this(datagram.content());
	}

	public RakNetPacket(byte[] data) {
		this(Unpooled.copiedBuffer(data));
	}

	public RakNetPacket(Packet packet) {
		super(packet);

		// Make sure this isn't an existing RakNetPacket!
		if (packet instanceof RakNetPacket) {
			this.id = ((RakNetPacket) packet).id;
		} else {
			this.id = this.readUByte();
		}
	}

	/**
	 * Returns the ID of the packet
	 * 
	 * @return The ID of the packet
	 */
	public final short getId() {
		return this.id;
	}

	/**
	 * Encodes the packet
	 */
	public void encode() {
	}

	/**
	 * Decodes the packet
	 */
	public void decode() {
	}

	/**
	 * Sets the buffer and updates the ID if specified
	 * 
	 * @param buffer
	 *            The new buffer
	 * @param updateId
	 *            Whether or not to update the ID
	 */
	public void setBuffer(byte[] buffer, boolean updateId) {
		super.setBuffer(buffer);
		if (updateId == true) {
			this.id = this.readUByte();
		}
	}

	@Override
	public void flip() {
		super.flip();
		this.id = this.readUByte();
	}

}
