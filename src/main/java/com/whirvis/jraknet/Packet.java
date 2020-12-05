/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 "Whirvis" Trent Summerlin
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
package com.whirvis.jraknet;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

import com.whirvis.jraknet.stream.PacketDataInputStream;
import com.whirvis.jraknet.stream.PacketDataOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;

/**
 * A generic packet that has the ability to read and write data to and from a
 * source buffer.
 *
 * @author "Whirvis" Trent Summerlin
 * @since JRakNet v1.0.0
 */
public class Packet {

	private ByteBuf buffer;
	private PacketDataInputStream input;
	private PacketDataOutputStream output;

	/**
	 * Creates a packet using the specified {@link ByteBuf}
	 * 
	 * @param buffer
	 *            the {@link ByteBuf} to read from and write to, a
	 *            <code>null</code> value will have a new buffer be used
	 *            instead.
	 * @throws IllegalArgumentException
	 *             if the <code>buffer</code> is an {@link EmptyByteBuf}.
	 */
	public Packet(ByteBuf buffer) throws IllegalArgumentException {
		if (buffer instanceof EmptyByteBuf) {
			throw new IllegalArgumentException("No content");
		}
		this.buffer = buffer == null ? Unpooled.buffer() : buffer;
		this.input = new PacketDataInputStream(this);
		this.output = new PacketDataOutputStream(this);
	}

	/**
	 * Creates packet from an existing {@link DatagramPacket}.
	 * 
	 * @param datagram
	 *            the {@link DatagramPacket} to read from.
	 */
	public Packet(DatagramPacket datagram) {
		this(datagram.content());
	}

	/**
	 * Creates a packet from an existing <code>byte[]</code>
	 * 
	 * @param data
	 *            the <code>byte[]</code> to read from.
	 */
	public Packet(byte[] data) {
		this(Unpooled.copiedBuffer(data));
	}

	/**
	 * Creates a packet from an existing packet's buffer.
	 * 
	 * @param packet
	 *            the packet whose buffer to reference and then read from and
	 *            write to.
	 */
	public Packet(Packet packet) {
		this(packet.buffer());
	}

	/**
	 * Creates an empty packet.
	 */
	public Packet() {
		this((ByteBuf) /* Solves ambiguity */ null);
	}

	/**
	 * Reads data into the specified <code>byte[]</code>.
	 * 
	 * @param dest
	 *            the <code>byte[]</code> to read the data into.
	 * @return the packet.
	 * @throws IndexOutOfBoundsException
	 *             if there are less readable bytes than the length of
	 *             <code>dest</code>.
	 */
	public final Packet read(byte[] dest) throws IndexOutOfBoundsException {
		for (int i = 0; i < dest.length; i++) {
			dest[i] = buffer.readByte();
		}
		return this;
	}

	/**
	 * Reads the specified amount of <code>byte</code>s.
	 * 
	 * @param length
	 *            the amount of <code>byte</code>s to read.
	 * @return the read <code>byte</code>s.
	 * @throws IndexOutOfBoundsException
	 *             if there are less readable bytes than the specified
	 *             <code>length</code>.
	 */
	public final byte[] read(int length) throws IndexOutOfBoundsException {
		byte[] data = new byte[length];
		for (int i = 0; i < data.length; i++) {
			data[i] = buffer.readByte();
		}
		return data;
	}

	/**
	 * Skips the specified amount of <code>byte</code>s.
	 * 
	 * @param length
	 *            the amount of <code>byte</code>s to skip.
	 */
	public final void skip(int length) {
		buffer.skipBytes(Math.min(length, this.remaining()));
	}

	/**
	 * Reads a <code>byte</code>.
	 * 
	 * @return a <code>byte</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>1</code> readable byte left in
	 *             the packet.
	 */
	public final byte readByte() throws IndexOutOfBoundsException {
		return buffer.readByte();
	}

	/**
	 * Reads an unsigned <code>byte</code>.
	 * 
	 * @return an unsigned <code>byte</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>1</code> readable byte left in
	 *             the packet.
	 */
	public final short readUnsignedByte() throws IndexOutOfBoundsException {
		return (short) (buffer.readByte() & 0xFF);
	}

	/**
	 * Reads a <code>boolean</code>.
	 * 
	 * @return <code>true</code> if the <code>byte</code> read is anything
	 *         higher than <code>0</code>, <code>false</code> otherwise.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>1</code> readable byte left in
	 *             the packet.
	 */
	public final boolean readBoolean() throws IndexOutOfBoundsException {
		return this.readUnsignedByte() > 0x00;
	}

	/**
	 * Reads a <code>char</code>.
	 * 
	 * @return a <code>char</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet.
	 */
	public final char readChar() throws IndexOutOfBoundsException {
		return (char) buffer.readShort();
	}

	/**
	 * Reads a little-endian <code>char</code>.
	 * 
	 * @return a little-endian <code>char</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet.
	 */
	public final char readCharLE() throws IndexOutOfBoundsException {
		return (char) buffer.readShortLE();
	}

	/**
	 * Reads a <code>short</code>.
	 * 
	 * @return a <code>short</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet.
	 */
	public final short readShort() throws IndexOutOfBoundsException {
		return buffer.readShort();
	}

	/**
	 * Reads a little-endian <code>short</code>.
	 * 
	 * @return a little-endian <code>short</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet.
	 */
	public final short readShortLE() throws IndexOutOfBoundsException {
		return buffer.readShortLE();
	}

	/**
	 * Reads an unsigned <code>short</code>.
	 * 
	 * @return an unsigned <code>short</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet.
	 */
	public final int readUnsignedShort() throws IndexOutOfBoundsException {
		return buffer.readShort() & 0xFFFF;
	}

	/**
	 * Reads an unsigned little-endian <code>short</code>.
	 * 
	 * @return an unsigned little-endian <code>short</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet.
	 */
	public final int readUnsignedShortLE() throws IndexOutOfBoundsException {
		return buffer.readShortLE() & 0xFFFF;
	}

	/**
	 * Reads a <code>triad</code>.
	 * 
	 * @return a <code>triad</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>3</code> readable bytes left in
	 *             the packet.
	 */
	public final int readTriad() throws IndexOutOfBoundsException {
		return buffer.readMedium();
	}

	/**
	 * Reads a little-endian <code>triad</code>.
	 * 
	 * @return a little-endian <code>triad</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>3</code> readable bytes left in
	 *             the packet.
	 */
	public final int readTriadLE() {
		return buffer.readMediumLE();
	}

	/**
	 * Reads an unsigned <code>triad</code>.
	 * 
	 * @return an unsigned <code>triad</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>3</code> readable bytes left in
	 *             the packet.
	 */
	public final int readUnsignedTriad() throws IndexOutOfBoundsException {
		return this.readTriad() & 0xFFFFFF;
	}

	/**
	 * Reads an unsigned little-endian <code>triad</code>.
	 * 
	 * @return an unsigned little-endian <code>triad</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>3</code> readable bytes left in
	 *             the packet.
	 */
	public final int readUnsignedTriadLE() throws IndexOutOfBoundsException {
		return this.readTriad() & 0xFFFFFF;
	}

	/**
	 * Reads an <code>int</code>.
	 * 
	 * @return an <code>int</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>4</code> readable bytes left in
	 *             the packet.
	 */
	public final int readInt() throws IndexOutOfBoundsException {
		return buffer.readInt();
	}

	/**
	 * Reads a little-endian <code>int</code>.
	 * 
	 * @return a little-endian <code>int</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>4</code> readable bytes left in
	 *             the packet.
	 */
	public final int readIntLE() throws IndexOutOfBoundsException {
		return buffer.readIntLE();
	}

	/**
	 * Reads an unsigned <code>int</code>.
	 * 
	 * @return an unsigned <code>int</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>4</code> readable bytes left in
	 *             the packet.
	 */
	public final long readUnsignedInt() throws IndexOutOfBoundsException {
		/*
		 * Don't forget the 'L' at the end of 0xFFFFFFFFL. Without it, the
		 * unsigned operation will fail as it will not be ANDing with a long!
		 */
		return buffer.readInt() & 0xFFFFFFFFL;
	}

	/**
	 * Reads an unsigned little-endian <code>int</code>.
	 * 
	 * @return an unsigned little-endian <code>int</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>4</code> readable bytes left in
	 *             the packet.
	 */
	public final long readUnsignedIntLE() throws IndexOutOfBoundsException {
		/*
		 * Don't forget the 'L' at the end of 0xFFFFFFFFL. Without it, the
		 * unsigned operation will fail as it will not be ANDing with a long!
		 */
		return buffer.readIntLE() & 0xFFFFFFFFL;
	}

	/**
	 * Reads a <code>long</code>.
	 * 
	 * @return a <code>long</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>8</code> readable bytes left in
	 *             the packet.
	 */
	public final long readLong() throws IndexOutOfBoundsException {
		return buffer.readLong();
	}

	/**
	 * Reads a little-endian <code>long</code>.
	 * 
	 * @return a little-endian <code>long</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>8</code> readable bytes left in
	 *             the packet.
	 */
	public final long readLongLE() throws IndexOutOfBoundsException {
		return buffer.readLongLE();
	}

	/**
	 * Reads an unsigned <code>long</code>.
	 * 
	 * @return an unsigned <code>long</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>8</code> readable bytes left in
	 *             the packet.
	 */
	public final BigInteger readUnsignedLong() throws IndexOutOfBoundsException {
		byte[] ulBytes = this.read(Long.BYTES);
		return new BigInteger(ulBytes);
	}

	/**
	 * Reads an unsigned little-endian <code>long</code>.
	 * 
	 * @return an unsigned little-endian <code>long</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>8</code> readable bytes left in
	 *             the packet.
	 */
	public final BigInteger readUnsignedLongLE() throws IndexOutOfBoundsException {
		byte[] ulBytesReversed = this.read(Long.BYTES);
		byte[] ulBytes = new byte[ulBytesReversed.length];
		for (int i = 0; i < ulBytes.length; i++) {
			ulBytes[i] = ulBytesReversed[ulBytesReversed.length - i - 1];
		}
		return new BigInteger(ulBytes);
	}

	/**
	 * Reads a <code>float</code>.
	 * 
	 * @return a <code>float</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>4</code> readable bytes left in
	 *             the packet.
	 */
	public final float readFloat() throws IndexOutOfBoundsException {
		return buffer.readFloat();
	}

	/**
	 * Reads a little-endian <code>float</code>.
	 * 
	 * @return a little-endian <code>float</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>4</code> readable bytes left in
	 *             the packet.
	 */
	public final float readFloatLE() throws IndexOutOfBoundsException {
		return buffer.readFloatLE();
	}

	/**
	 * Reads a <code>double</code>.
	 * 
	 * @return a <code>double</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>8</code> readable bytes left in
	 *             the packet.
	 */
	public final double readDouble() throws IndexOutOfBoundsException {
		return buffer.readDouble();
	}

	/**
	 * Reads a little-endian <code>double</code>.
	 * 
	 * @return a little-endian <code>double</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>8</code> readable bytes left in
	 *             the packet.
	 */
	public final double readDoubleLE() throws IndexOutOfBoundsException {
		return buffer.readDoubleLE();
	}

	/**
	 * Reads a <code>VarInt</code>.
	 * 
	 * @return a <code>VarInt</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are not enough bytes to read a <code>VarInt</code>
	 *             or the <code>VarInt</code> exceeds the size limit.
	 * @throws RuntimeException
	 *             if an I/O error occurs despite the fact it should never
	 *             happen.
	 * @see VarInt#readVarInt(java.io.InputStream)
	 *      VarInt.readVarInt(InputStream)
	 */
	public final int readVarInt() throws IndexOutOfBoundsException, RuntimeException {
		try {
			return VarInt.readVarInt(this.getInputStream());
		} catch (EOFException e) {
			throw new IndexOutOfBoundsException("VarInt underflow");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads an unsigned <code>VarInt</code>.
	 * 
	 * @return an unsigned <code>VarInt</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are not enough bytes to read a <code>VarInt</code>
	 *             or the <code>VarInt</code> exceeds the size limit.
	 * @throws RuntimeException
	 *             if an I/O error occurs despite the fact it should never
	 *             happen.
	 * @see VarInt#readUnsignedVarInt(java.io.InputStream)
	 *      VarInt.readUnsignedVarInt(InputStream)
	 */
	public final long readUnsignedVarInt() throws IndexOutOfBoundsException, RuntimeException {
		try {
			return VarInt.readUnsignedVarInt(this.getInputStream());
		} catch (EOFException e) {
			throw new IndexOutOfBoundsException("VarInt underflow");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads a <code>VarLong</code>.
	 * 
	 * @return a <code>VarLong</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are not enough bytes to read a <code>VarLong</code>
	 *             or the <code>VarLong</code> exceeds the size limit.
	 * @throws RuntimeException
	 *             if an I/O error occurs despite the fact it should never
	 *             happen.
	 * @see VarInt#readVarLong(java.io.InputStream)
	 *      VarInt.readVarLong(InputStream)
	 */
	public final long readVarLong() throws IndexOutOfBoundsException, RuntimeException {
		try {
			return VarInt.readVarLong(this.getInputStream());
		} catch (EOFException e) {
			throw new IndexOutOfBoundsException("VarInt underflow");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads an unsigned <code>VarLong</code>.
	 * 
	 * @return an unsigned <code>VarLong</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are not enough bytes to read a <code>VarLong</code>
	 *             or the <code>VarLong</code> exceeds the size limit.
	 * @throws RuntimeException
	 *             if an I/O error occurs despite the fact it should never
	 *             happen.
	 * @see VarInt#readUnsignedVarLong(java.io.InputStream)
	 *      VarInt.readUnsignedVarLong(InputStream)
	 */
	public final long readUnsignedVarLong() throws IndexOutOfBoundsException, RuntimeException {
		try {
			return VarInt.readUnsignedVarLong(this.getInputStream());
		} catch (EOFException e) {
			throw new IndexOutOfBoundsException("VarLong underflow");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads a UTF-8 string with its length prefixed by an unsigned
	 * <code>short</code>.
	 * 
	 * @return a string.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet to read the length of the string, or if there are
	 *             less readable bytes than are specified by the length.
	 */
	public final String readString() throws IndexOutOfBoundsException {
		int len = this.readUnsignedShort();
		byte[] data = this.read(len);
		return new String(data);
	}

	/**
	 * Reads a UTF-8 string with its length prefixed by a unsigned little
	 * -endian <code>short</code>.
	 * 
	 * @return a string.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet to read the length of the string, or if there are
	 *             less readable bytes than are specified by the length.
	 */
	public final String readStringLE() throws IndexOutOfBoundsException {
		int len = this.readUnsignedShortLE();
		byte[] data = this.read(len);
		return new String(data);
	}

	/**
	 * Reads an IPv4/IPv6 address.
	 * 
	 * @return an IPv4/IPv6 address.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>8</code> readable bytes left in
	 *             the packet when it is an IPv4 address or <code>30</code> when
	 *             it is an IPv6 address.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found,
	 *             the family for an IPv6 address was not
	 *             {@value RakNet#AF_INET6}, a <code>scope_id</code> was
	 *             specified for a global IPv6 address, or the address version
	 *             is an unknown version.
	 */
	public final InetSocketAddress readAddress() throws IndexOutOfBoundsException, UnknownHostException {
		short version = this.readUnsignedByte();
		if (version == RakNet.IPV4) {
			byte[] ipAddress = new byte[RakNet.IPV4_ADDRESS_LENGTH];
			for (int i = 0; i < ipAddress.length; i++) {
				ipAddress[i] = (byte) (~this.readByte() & 0xFF);
			}
			int port = this.readUnsignedShort();
			return new InetSocketAddress(InetAddress.getByAddress(ipAddress), port);
		} else if (version == RakNet.IPV6) {
			this.readShortLE(); // Family
			int port = this.readUnsignedShort();
			this.readInt(); // Flow info
			byte[] ipAddress = new byte[RakNet.IPV6_ADDRESS_LENGTH];
			for (int i = 0; i < ipAddress.length; i++) {
				ipAddress[i] = this.readByte();
			}
			this.readInt(); // Scope ID
			return new InetSocketAddress(InetAddress.getByAddress(ipAddress), port);
		} else {
			throw new UnknownHostException("Unknown protocol IPv" + version);
		}
	}

	/**
	 * Reads a <code>UUID</code>.
	 * 
	 * @return a <code>UUID</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>16</code> readable bytes left in
	 *             the packet.
	 */
	public final UUID readUUID() throws IndexOutOfBoundsException {
		long mostSignificantBits = this.readLong();
		long leastSignificantBits = this.readLong();
		return new UUID(mostSignificantBits, leastSignificantBits);
	}

	/**
	 * Writes the specified <code>byte</code>s to the packet.
	 * 
	 * @param data
	 *            the data to write.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>data</code> is <code>null</code>.
	 */
	public final Packet write(byte... data) throws NullPointerException {
		if (data == null) {
			throw new NullPointerException("Data cannot be null");
		}
		for (byte datum : data) {
			buffer.writeByte(datum);
		}
		return this;
	}

	/**
	 * Writes the specified <code>byte</code>s to the packet.
	 * <p>
	 * This method is simply a shorthand for the {@link #write(byte...)} method,
	 * with all the values being automatically casted back to a
	 * <code>byte</code> before being sent to the original
	 * {@link #write(byte...)} method.
	 * 
	 * @param data
	 *            the data to write.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>data</code> is <code>null</code>.
	 */
	public final Packet write(int... data) {
		if (data == null) {
			throw new NullPointerException("Data cannot be null");
		}
		byte[] bData = new byte[data.length];
		for (int i = 0; i < data.length; i++) {
			bData[i] = (byte) data[i];
		}
		return this.write(bData);
	}

	/**
	 * Writes the specified amount of <code>null</code> (<code>0x00</code>)
	 * bytes to the packet.
	 * 
	 * @param length
	 *            the amount of bytes to write.
	 * @return the packet.
	 */
	public final Packet pad(int length) {
		for (int i = 0; i < length; i++) {
			buffer.writeByte(0x00);
		}
		return this;
	}

	/**
	 * Writes a <code>byte</code> to the packet.
	 * 
	 * @param b
	 *            the <code>byte</code>.
	 * @return the packet.
	 */
	public final Packet writeByte(int b) {
		buffer.writeByte((byte) b);
		return this;
	}

	/**
	 * Writes an unsigned <code>byte</code> to the packet.
	 * 
	 * @param b
	 *            the unsigned <code>byte</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>b</code> is not within the range of
	 *             <code>0-255</code>.
	 */
	public final Packet writeUnsignedByte(int b) throws IllegalArgumentException {
		if (b < 0x00 || b > 0xFF) {
			throw new IllegalArgumentException("Value must be in between 0-255");
		}
		buffer.writeByte(((byte) b) & 0xFF);
		return this;
	}

	/**
	 * Writes a <code>boolean</code> to the packet.
	 * 
	 * @param b
	 *            the <code>boolean</code>.
	 * @return the packet.
	 */
	public final Packet writeBoolean(boolean b) {
		buffer.writeByte(b ? 0x01 : 0x00);
		return this;
	}

	/**
	 * Writes a <code>short</code> to the packet.
	 * 
	 * @param s
	 *            the <code>short</code>.
	 * @return the packet.
	 */
	public final Packet writeShort(int s) {
		buffer.writeShort(s);
		return this;
	}

	/**
	 * Writes a little-endian <code>short</code> to the packet.
	 * 
	 * @param s
	 *            the <code>short</code>.
	 * @return the packet.
	 */
	public final Packet writeShortLE(int s) {
		buffer.writeShortLE(s);
		return this;
	}

	/**
	 * Writes a unsigned <code>short</code> to the packet.
	 * 
	 * @param s
	 *            the <code>short</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>s</code> is not within the range of
	 *             <code>0-65535</code>.
	 */
	public final Packet writeUnsignedShort(int s) throws IllegalArgumentException {
		if (s < 0x0000 || s > 0xFFFF) {
			throw new IllegalArgumentException("Value must be in between 0-65535");
		}
		buffer.writeShort(((short) s) & 0xFFFF);
		return this;
	}

	/**
	 * Writes an unsigned little-endian <code>short</code> to the packet.
	 * 
	 * @param s
	 *            the <code>short</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>s</code> is not in between <code>0-65535</code>.
	 */
	public final Packet writeUnsignedShortLE(int s) throws IllegalArgumentException {
		if (s < 0x0000 || s > 0xFFFF) {
			throw new IllegalArgumentException("Value must be in between 0-65535");
		}
		buffer.writeShortLE(((short) s) & 0xFFFF);
		return this;
	}

	/**
	 * Writes a <code>triad</code> to the packet.
	 * 
	 * @param t
	 *            the <code>triad</code>.
	 * @return the packet.
	 */
	public final Packet writeTriad(int t) {
		buffer.writeMedium(t);
		return this;
	}

	/**
	 * Writes a little-endian <code>triad</code> to the packet.
	 * 
	 * @param t
	 *            the <code>triad</code>.
	 * @return the packet.
	 */
	public final Packet writeTriadLE(int t) {
		buffer.writeMediumLE(t);
		return this;
	}

	/**
	 * Writes an unsigned <code>triad</code> to the packet.
	 * 
	 * @param t
	 *            the <code>triad</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>t</code> is not in between <code>0-16777215</code>.
	 */
	public final Packet writeUnsignedTriad(int t) throws IllegalArgumentException {
		if (t < 0x000000 || t > 0xFFFFFF) {
			throw new IllegalArgumentException("Value must be in between 0-16777215");
		}
		return this.writeTriad(t & 0xFFFFFF);
	}

	/**
	 * Writes an unsigned little-endian <code>triad</code> to the packet.
	 * 
	 * @param t
	 *            the <code>triad</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>t</code> is not in between <code>0-16777215</code>.
	 */
	public final Packet writeUnsignedTriadLE(int t) throws IllegalArgumentException {
		if (t < 0x000000 || t > 0xFFFFFF) {
			throw new IllegalArgumentException("Value must be in between 0-16777215");
		}
		return this.writeTriadLE(t & 0xFFFFFF);
	}

	/**
	 * Writes an <code>int</code> to the packet.
	 * 
	 * @param i
	 *            the <code>int</code>.
	 * @return the packet.
	 */
	public final Packet writeInt(int i) {
		buffer.writeInt(i);
		return this;
	}

	/**
	 * Writes an unsigned <code>int</code> to the packet.
	 * 
	 * @param i
	 *            the <code>int</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>i</code> is not in between <code>0-4294967295</code>
	 */
	public final Packet writeUnsignedInt(long i) throws IllegalArgumentException {
		if (i < 0x00000000 || i > 0xFFFFFFFFL) {
			throw new IllegalArgumentException("Value must be in between 0-4294967295");
		}
		buffer.writeInt(((int) i));
		return this;
	}

	/**
	 * Writes a little-endian <code>int</code> to the packet.
	 * 
	 * @param i
	 *            the <code>int</code>.
	 * @return the packet.
	 */
	public final Packet writeIntLE(int i) {
		buffer.writeIntLE(i);
		return this;
	}

	/**
	 * Writes an unsigned little-endian <code>int</code> to the packet.
	 * 
	 * @param i
	 *            the <code>int</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>i</code> is not in between
	 *             <code>0-4294967295</code>.
	 */
	public final Packet writeUnsignedIntLE(long i) throws IllegalArgumentException {
		if (i < 0x00000000 || i > 0xFFFFFFFFL) {
			throw new IllegalArgumentException("Value must be in between 0-4294967295");
		}
		buffer.writeIntLE(((int) i) & 0xFFFFFFFF);
		return this;
	}

	/**
	 * Writes a <code>long</code> to the packet.
	 * 
	 * @param l
	 *            the <code>long</code>.
	 * @return the packet.
	 */
	public final Packet writeLong(long l) {
		buffer.writeLong(l);
		return this;
	}

	/**
	 * Writes a little-endian <code>long</code> to the packet.
	 * 
	 * @param l
	 *            the <code>long</code>.
	 * @return the packet.
	 */
	public final Packet writeLongLE(long l) {
		buffer.writeLongLE(l);
		return this;
	}

	/**
	 * Writes an unsigned <code>long</code> to the packet.
	 * 
	 * @param bi
	 *            the <code>long</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>bi</code> is bigger than {@value Long#BYTES} bytes
	 *             or is negative.
	 */
	public final Packet writeUnsignedLong(BigInteger bi) throws IllegalArgumentException {
		byte[] ulBytes = bi.toByteArray();
		if (ulBytes.length > Long.BYTES) {
			throw new IllegalArgumentException("Value is too big to fit into a long");
		} else if (bi.longValue() < 0) {
			throw new IllegalArgumentException("Value cannot be negative");
		}
		for (int i = 0; i < Long.BYTES; i++) {
			this.writeByte(i < ulBytes.length ? ulBytes[i] : 0x00);
		}
		return this;
	}

	/**
	 * Writes an unsigned <code>long</code> to the packet.
	 * 
	 * @param l
	 *            the <code>long</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>l</code> is less than <code>0</code>.
	 */
	public final Packet writeUnsignedLong(long l) throws IllegalArgumentException {
		return this.writeUnsignedLong(new BigInteger(Long.toString(l)));
	}

	/**
	 * Writes an unsigned little-endian <code>long</code> to the packet.
	 * 
	 * @param bi
	 *            the <code>long</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if the size of the <code>bi</code> is bigger than
	 *             {@value Long#BYTES} bytes or is negative.
	 */
	public final Packet writeUnsignedLongLE(BigInteger bi) throws IllegalArgumentException {
		byte[] ulBytes = bi.toByteArray();
		if (ulBytes.length > Long.BYTES) {
			throw new IllegalArgumentException("Value is too big to fit into a long");
		} else if (bi.longValue() < 0) {
			throw new IllegalArgumentException("Value cannot be negative");
		}
		for (int i = Long.BYTES - 1; i >= 0; i--) {
			this.writeByte(i < ulBytes.length ? ulBytes[i] : 0x00);
		}
		return this;
	}

	/**
	 * Writes an unsigned little-endian <code>long</code> to the packet.
	 * 
	 * @param l
	 *            the <code>long</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>l</code> is less than <code>0</code>.
	 */
	public final Packet writeUnsignedLongLE(long l) throws IllegalArgumentException {
		return this.writeUnsignedLongLE(new BigInteger(Long.toString(l)));
	}

	/**
	 * Writes a <code>float</code> to the packet.
	 * 
	 * @param f
	 *            the <code>float</code>.
	 * @return the packet.
	 */
	public final Packet writeFloat(double f) {
		buffer.writeFloat((float) f);
		return this;
	}

	/**
	 * Writes a little-endian <code>float</code> to the packet.
	 * 
	 * @param f
	 *            the <code>float</code>.
	 * @return the packet.
	 */
	public final Packet writeFloatLE(double f) {
		buffer.writeFloatLE((float) f);
		return this;
	}

	/**
	 * Writes a <code>double</code> to the packet.
	 * 
	 * @param d
	 *            the <code>double</code>.
	 * @return the packet.
	 */
	public final Packet writeDouble(double d) {
		buffer.writeDouble(d);
		return this;
	}

	/**
	 * Writes a <code>double</code> to the packet.
	 * 
	 * @param d
	 *            the <code>double</code>.
	 * @return the packet.
	 */
	public final Packet writeDoubleLE(double d) {
		buffer.writeDoubleLE(d);
		return this;
	}

	/**
	 * Writes a <code>VarInt</code> to the packet.
	 * 
	 * @param i
	 *            the <code>VarInt</code>.
	 * @return the packet.
	 * @throws RuntimeException
	 *             if an I/O error occurs despite the fact it should never
	 *             happen.
	 * @see VarInt#writeVarInt(int, java.io.OutputStream)
	 *      VarInt.writeVarInt(int, OutputStream)
	 */
	public final Packet writeVarInt(int i) throws RuntimeException {
		try {
			VarInt.writeVarInt(i, this.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	/**
	 * Writes a unsigned <code>VarInt</code> to the packet.
	 * 
	 * @param i
	 *            the <code>VarInt</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>i</code> is not within the range of
	 *             <code>0-4294967295</code>.
	 * @throws RuntimeException
	 *             if an I/O error occurs despite the fact it should never
	 *             happen.
	 * @see VarInt#writeUnsignedVarInt(int, java.io.OutputStream)
	 *      VarInt.writeUnsignedVarInt(int, OutputStream)
	 */
	public final Packet writeUnsignedVarInt(int i) throws RuntimeException {
		if (i < 0x00000000 || i > 0xFFFFFFFFL) {
			throw new IllegalArgumentException("Value must be in between 0-4294967295");
		}
		try {
			VarInt.writeUnsignedVarInt(i, this.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	/**
	 * Writes a <code>VarLong</code> to the packet.
	 * 
	 * @param l
	 *            the <code>VarLong</code>.
	 * @return the packet.
	 * @throws RuntimeException
	 *             if an I/O error occurs despite the fact it should never
	 *             happen.
	 * @see VarInt#writeVarLong(long, java.io.OutputStream)
	 *      VarInt.writeVarLong(long, OutputStream)
	 */
	public final Packet writeVarLong(long l) throws RuntimeException {
		try {
			VarInt.writeVarLong(l, this.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	/**
	 * Writes a unsigned <code>VarLong</code> to the packet.
	 * 
	 * @param l
	 *            the <code>VarLong</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>i</code> is not within the range of
	 *             <code>0-18446744073709551615L</code>.
	 * @throws RuntimeException
	 *             if an I/O error occurs despite the fact it should never
	 *             happen.
	 * @see VarInt#writeUnsignedVarLong(long, java.io.OutputStream)
	 *      VarInt.writeUnsignedVarLong(long, OutputStream)
	 */
	public final Packet writeUnsignedVarLong(long l) throws RuntimeException {
		if (l < 0x0000000000000000 || l > 0xFFFFFFFFFFFFFFFFL) {
			throw new IllegalArgumentException("Value must be in between 0-18446744073709551615L");
		}
		try {
			VarInt.writeUnsignedVarLong(l, this.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	/**
	 * Writes a UTF-8 string prefixed by an unsigned <code>short</code> to the
	 * packet.
	 * 
	 * @param s
	 *            the string.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if <code>s</code> is <code>null</code>.
	 */
	public final Packet writeString(String s) throws NullPointerException {
		if (s == null) {
			throw new NullPointerException("String cannot be null");
		}
		byte[] data = s.getBytes();
		this.writeUnsignedShort(data.length);
		this.write(data);
		return this;
	}

	/**
	 * Writes a UTF-8 string prefixed by a little-endian unsigned
	 * <code>short</code> to the packet.
	 * 
	 * @param s
	 *            the string.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if <code>s</code> is <code>null</code>.
	 */
	public final Packet writeStringLE(String s) throws NullPointerException {
		if (s == null) {
			throw new NullPointerException("String cannot be null");
		}
		byte[] data = s.getBytes();
		this.writeUnsignedShortLE(data.length);
		this.write(data);
		return this;
	}

	/**
	 * Writes an IPv4/IPv6 address to the packet.
	 * 
	 * @param address
	 *            the address.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>address</code> or IP address are
	 *             <code>null</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, if
	 *             a <code>scope_id</code> was specified for a global IPv6
	 *             address, or the length of the address is not either
	 *             {@value RakNet#IPV4_ADDRESS_LENGTH} or
	 *             {@value RakNet#IPV6_ADDRESS_LENGTH} <code>byte</code>s.
	 */
	public final Packet writeAddress(InetSocketAddress address) throws NullPointerException, UnknownHostException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		byte[] ipAddress = address.getAddress().getAddress();
		int version = RakNet.getAddressVersion(address);
		if (version == RakNet.IPV4) {
			this.writeUnsignedByte(RakNet.IPV4);
			for (byte b : ipAddress) {
				this.writeByte(~b & 0xFF);
			}
			this.writeUnsignedShort(address.getPort());
		} else if (version == RakNet.IPV6) {
			this.writeUnsignedByte(RakNet.IPV6);
			this.writeShortLE(RakNet.AF_INET6);
			this.writeShort(address.getPort());
			this.writeInt(0x00); // Flow info
			this.write(ipAddress);
			this.writeInt(0x00); // Scope ID
		} else {
			throw new UnknownHostException(
					"Unknown protocol for address with length of " + ipAddress.length + " bytes");
		}
		return this;
	}

	/**
	 * Writes an IPv4 address to the packet.
	 * 
	 * @param host
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the port is not in between <code>0-65535</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could not be
	 *             found, or if a <code>scope_id</code> was specified for a
	 *             global IPv6 address.
	 */
	public final Packet writeAddress(InetAddress host, int port)
			throws NullPointerException, IllegalArgumentException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("Host cannot be null");
		} else if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Port must be in between 0-65535");
		}
		return this.writeAddress(new InetSocketAddress(host, port));
	}

	/**
	 * Writes an IPv4 address to the packet (IPv6 is not yet supported).
	 * 
	 * @param host
	 *            the IP address.
	 * @param port
	 *            the port.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the port is not in between <code>0-65535</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could not be
	 *             found, or if a <code>scope_id</code> was specified for a
	 *             global IPv6 address.
	 */
	public final Packet writeAddress(String host, int port)
			throws NullPointerException, IllegalArgumentException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("Host cannot be null");
		} else if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Port must be in between 0-65535");
		}
		return this.writeAddress(InetAddress.getByName(host), port);
	}

	/**
	 * Writes a <code>UUID</code> to the packet.
	 * 
	 * @param uuid
	 *            the <code>UUID</code>.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>uuid</code> is <code>null</code>.
	 */
	public final Packet writeUUID(UUID uuid) throws NullPointerException {
		if (uuid == null) {
			throw new NullPointerException("UUID cannot be null");
		}
		this.writeLong(uuid.getMostSignificantBits());
		this.writeLong(uuid.getLeastSignificantBits());
		return this;
	}

	/**
	 * Returns the packet as a <code>byte[]</code>.
	 * 
	 * @return the packet as a <code>byte[]</code>, <code>null</code> if the
	 *         buffer being used within the packet is a direct buffer.
	 */
	public byte[] array() {
		if (buffer.isDirect()) {
			return null;
		}
		return Arrays.copyOfRange(buffer.array(), 0, buffer.writerIndex());
	}

	/**
	 * Returns the size of the packet in <code>byte</code>s.
	 * <p>
	 * This is to be used only for packets that are being written to. To get the
	 * amount of bytes that are still readable, use the {@link #remaining()}
	 * method.
	 * 
	 * @return the size of the packet in <code>byte</code>s.
	 */
	public int size() {
		return buffer.writerIndex();
	}

	/**
	 * Returns the packet buffer.
	 * <p>
	 * This method will not increase the buffer's reference count via the
	 * {@link ByteBuf#retain()} method. It is up to the original packet creator
	 * to release this packet's buffer.
	 * <p>
	 * Packet buffers are released when they are actually sent over the internal
	 * pipelines of either a server or a client. As a result, one does not
	 * normally need to worry about releasing a packet buffer so long as they
	 * plan to eventually send the packet.
	 * 
	 * @return the packet buffer.
	 */
	public ByteBuf buffer() {
		return this.buffer;
	}

	/**
	 * Returns a copy of the packet buffer.
	 * 
	 * @return a copy of the packet buffer.
	 */
	public ByteBuf copy() {
		return buffer.copy();
	}

	/**
	 * Releases the packet's buffer.
	 * 
	 * @return <code>true</code> if and only if the reference count became
	 *         <code>0</code> and this object has been deallocated,
	 *         <code>false</code> otherwise.
	 */
	public boolean release() {
		return buffer.release();
	}

	/**
	 * Returns the packet's {@link java.io.InputStream InputStream}
	 * 
	 * @return the packet's {@link java.io.InputStream InputStream}.
	 */
	public final PacketDataInputStream getInputStream() {
		return this.input;
	}

	/**
	 * Returns the packet's {@link java.io.OutputStream OutputStream}.
	 * 
	 * @return the packet's {@link java.io.OutputStream OutputStream}.
	 */
	public final PacketDataOutputStream getOutputStream() {
		return this.output;
	}

	/**
	 * Returns how many readable <code>byte</code>s are left in the packet's
	 * buffer.
	 * <p>
	 * This is to only be used for packets that are being read from. To get the
	 * amount of bytes that have been written to the packet, use the
	 * {@link #size()} method.
	 * 
	 * @return how many readable <code>byte</code>s are left in the packet's
	 *         buffer.
	 */
	public int remaining() {
		return buffer.readableBytes();
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param buffer
	 *            the buffer to read from and write to, a <code>null</code>
	 *            value will have a new buffer be used instead.
	 * @return the packet.
	 */
	public final Packet setBuffer(ByteBuf buffer) {
		this.buffer = buffer == null ? Unpooled.buffer() : buffer;
		return this;
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param datagram
	 *            the {@link DatagramPacket} whose buffer to read from and write
	 *            to.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>datagram</code> packet is <code>null</code>.
	 */
	public final Packet setBuffer(DatagramPacket datagram) throws NullPointerException {
		if (datagram == null) {
			throw new NullPointerException("Datagram packet cannot be null");
		}
		return this.setBuffer(datagram.content());
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param data
	 *            the <code>byte[]</code> to create the new buffer from.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>data</code> is <code>null</code>.
	 */
	public final Packet setBuffer(byte[] data) throws NullPointerException {
		if (data == null) {
			throw new NullPointerException("Data cannot be null");
		}
		return this.setBuffer(Unpooled.copiedBuffer(data));
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param packet
	 *            the packet whose buffer to copy to read from and write to.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>packet</code> is <code>null</code>.
	 */
	public final Packet setBuffer(Packet packet) throws NullPointerException {
		if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		}
		return this.setBuffer(packet.copy());
	}

	/**
	 * Flips the packet.
	 * <p>
	 * Flipping the packet will cause the current internal buffer to be released
	 * with the a new buffer taking it's place. The newly created buffer will
	 * retain the reference count of the original buffer before it was
	 * de-allocated.
	 * 
	 * @return the packet.
	 */
	public Packet flip() {
		byte[] data = buffer.array();
		int increment = buffer.refCnt();
		buffer.release(increment); // No longer needed
		this.buffer = Unpooled.copiedBuffer(data);
		buffer.retain(increment);
		return this;
	}

	/**
	 * Clears the packet's buffer.
	 * 
	 * @return the packet.
	 */
	public Packet clear() {
		buffer.clear();
		return this;
	}

}
