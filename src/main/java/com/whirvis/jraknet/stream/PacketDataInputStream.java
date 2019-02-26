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
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
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
package com.whirvis.jraknet.stream;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

import com.whirvis.jraknet.Packet;

/**
 * Used as a way for a {@link Packet} to be used where an {@link InputStream} is
 * required.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet vUNKNOWN
 * @see Packet#getDataInput()
 */
public class PacketDataInputStream extends InputStream implements DataInput {

	private final Packet packet;

	/**
	 * Creates a packet input stream to read data from the specified underlying
	 * packet.
	 * 
	 * @param packet
	 *            the underlying packet.
	 */
	public PacketDataInputStream(Packet packet) {
		this.packet = packet;
	}

	@Override
	public int read() throws IOException {
		if (packet.remaining() <= 0) {
			return -1; // No data remaining
		} else {
			return packet.readUnsignedByte();
		}
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		for (int i = 0; i < b.length; i++) {
			b[i] = packet.readByte();
		}
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		for (int i = off; i < len; i++) {
			b[i] = packet.readByte();
		}
	}

	@Override
	public int skipBytes(int n) throws IOException {
		int skipped = 0;
		while (skipped < n && packet.remaining() > 0) {
			packet.readByte();
			skipped++;
		}
		return skipped;
	}

	@Override
	public boolean readBoolean() throws IOException {
		return packet.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return packet.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return packet.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException {
		return packet.readShort();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return packet.readUnsignedShort();
	}

	@Override
	public char readChar() throws IOException {
		return (char) packet.readUnsignedShort();
	}

	@Override
	public int readInt() throws IOException {
		return packet.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return packet.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return packet.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return packet.readDouble();
	}

	@Override
	public String readLine() throws IOException {
		return packet.readString();
	}
	
	@Override
	public String readUTF() throws IOException {
		return packet.readString();
	}

}
