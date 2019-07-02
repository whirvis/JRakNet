/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
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
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.whirvis.jraknet.Packet;

/**
 * Used as a way for a {@link Packet} to be used where an {@link InputStream} is
 * required.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.1.0
 * @see Packet#getInputStream()
 */
public final class PacketDataInputStream extends InputStream implements DataInput {

	/**
	 * Signals that the end of a file has been reached.
	 */
	private static final byte EOF = -1;

	private final Packet packet;
	private final DataInputStream dataIn;

	/**
	 * Creates a packet input stream to read data from the specified underlying
	 * packet.
	 * 
	 * @param packet
	 *            the underlying packet.
	 */
	public PacketDataInputStream(Packet packet) {
		this.packet = packet;
		this.dataIn = new DataInputStream(this);
	}

	@Override
	public int read() throws IOException, EOFException {
		if (packet.remaining() > 0) {
			return packet.readUnsignedByte();
		}
		return EOF;
	}

	@Override
	public void readFully(byte[] b) throws IOException, EOFException {
		for (int i = 0; i < b.length; i++) {
			b[i] = packet.readByte();
			if (b[i] == EOF) {
				throw new EOFException();
			}
		}
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException, EOFException {
		for (int i = off; i < len; i++) {
			b[i] = packet.readByte();
			if (b[i] == EOF) {
				throw new EOFException();
			}
		}
	}

	@Override
	public int skipBytes(int n) throws IOException, EOFException {
		int skipped = 0;
		while (skipped < n && packet.remaining() > 0) {
			packet.readByte();
			skipped++;
		}
		return skipped;
	}

	@Override
	public boolean readBoolean() throws IOException, EOFException {
		try {
			return packet.readBoolean();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public byte readByte() throws IOException, EOFException {
		try {
			return packet.readByte();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public int readUnsignedByte() throws IOException, EOFException {
		try {
			return packet.readUnsignedByte();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public short readShort() throws IOException, EOFException {
		try {
			return packet.readShort();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public int readUnsignedShort() throws IOException, EOFException {
		try {
			return packet.readUnsignedShort();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public char readChar() throws IOException, EOFException {
		try {
			return (char) packet.readUnsignedShort();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public int readInt() throws IOException, EOFException {
		try {
			return packet.readInt();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public long readLong() throws IOException, EOFException {
		try {
			return packet.readLong();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public float readFloat() throws IOException, EOFException {
		try {
			return packet.readFloat();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	@Override
	public double readDouble() throws IOException, EOFException {
		try {
			return packet.readDouble();
		} catch (IndexOutOfBoundsException e) {
			throw new EOFException();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is implemented via a {@link DataInputStream} which refers
	 * back to this original stream to execute the {@link #readLine()} method.
	 */
	@Deprecated
	@Override
	public String readLine() throws IOException {
		return dataIn.readLine();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is implemented via a {@link DataInputStream} which refers
	 * back to this original stream to execute the {@link #readLine()} method.
	 */
	@Override
	public String readUTF() throws IOException, EOFException {
		return dataIn.readUTF();
	}

}
