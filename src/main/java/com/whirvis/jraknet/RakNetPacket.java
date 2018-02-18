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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;

/**
 * A generic RakNet packet that has the ability to get the ID of the packet
 * along with encoding and decoding.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class RakNetPacket extends Packet {

	// RakNet packet data
	private short id;

	/**
	 * Constructs a <code>RakNetPacket</code> with the specified ID that will be
	 * written to it.
	 * 
	 * @param id
	 *            the ID of the <code>RakNetPacket</code>.
	 */
	public RakNetPacket(int id) {
		super();
		if (id < 0 || id > 255) {
			throw new IllegalArgumentException("Invalid ID, must be in between 0-255");
		}
		this.writeUnsignedByte(this.id = (short) id);
	}

	/**
	 * Constructs a <code>RakNetPacket</code> that reads from and writes to the
	 * specified <code>ByteBuf</code>. On instantiation, the first byte of the
	 * buffer will be read and set as the ID.
	 * 
	 * @param buffer
	 *            the <code>ByteBuf</code> to read from and write to.
	 */
	public RakNetPacket(ByteBuf buffer) {
		super(buffer);
		if (this.remaining() < 1) {
			throw new IllegalArgumentException("The packet contains no data, it has no ID to be read");
		}
		this.id = this.readUnsignedByte();
	}

	/**
	 * Constructs a <code>RakNetPacket</code> that reads from and writes to the
	 * specified <code>DatagramPacket</code>. On instantiation, the first byte of
	 * the datagram will be read and set as the ID.
	 * 
	 * @param datagram
	 *            the <code>DatagramPacket</code> to read from and write to.
	 */
	public RakNetPacket(DatagramPacket datagram) {
		this(datagram.content());
	}

	/**
	 * Constructs a <code>RakNetPacket</code> that reads from and writes to the
	 * specified byte array. On instantiation, the first byte of the byte array will
	 * be read and set as the ID.
	 * 
	 * @param data
	 *            the byte array to read from and write to.
	 */
	public RakNetPacket(byte[] data) {
		this(Unpooled.copiedBuffer(data));
	}

	/**
	 * Constructs a <code>RakNetPacket</code> that reads from and writes to the
	 * specified <code>Packet</code>. On instantiation, the first byte of the buffer
	 * will be read and set as the ID unless the specified packet is a subclass of
	 * <code>RakNetPacket</code>.
	 * 
	 * @param packet
	 *            the <code>Packet</code> to read from and write to.
	 */
	public RakNetPacket(Packet packet) {
		super(packet);

		// Make sure this isn't an existing RakNetPacket!
		if (packet instanceof RakNetPacket) {
			this.id = ((RakNetPacket) packet).id;
		} else {
			this.id = this.readUnsignedByte();
		}
	}

	/**
	 * @return the ID of the packet.
	 */
	public final short getId() {
		return this.id;
	}

	/**
	 * Encodes the packet.
	 */
	public void encode() {
	}

	/**
	 * Decodes the packet.
	 */
	public void decode() {
	}

	/**
	 * Sets the buffer and updates the ID if specified.
	 * 
	 * @param buffer
	 *            the new buffer.
	 * @param updateId
	 *            whether or not to update the ID.
	 */
	public void setBuffer(byte[] buffer, boolean updateId) {
		super.setBuffer(buffer);
		if (updateId == true) {
			this.id = this.readUnsignedByte();
		}
	}

	@Override
	public Packet flip() {
		super.flip();
		this.id = this.readUnsignedByte();
		return this;
	}

}
