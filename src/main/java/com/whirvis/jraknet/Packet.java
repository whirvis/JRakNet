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
package com.whirvis.jraknet;

import java.math.BigDecimal;
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
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public class Packet {

	private static final int ADDRESS_VERSION_IPV4 = 0x04;
	private static final int ADDRESS_VERSION_IPV6 = 0x06;
	private static final int ADDRESS_VERSION_IPV4_LENGTH = 0x04;
	private static final int ADDRESS_VERSION_IPV6_LENGTH = 0x10;
	private static final int ADDRESS_VERSION_IPV6_MYSTERY_LENGTH = 0x0A;

	private ByteBuf buffer;
	private PacketDataInputStream input;
	private PacketDataOutputStream output;

	/**
	 * Creates a packet using the specified {@link io.netty.ByteBuf ByteBuf}
	 * 
	 * @param buffer
	 *            the {@link io.netty.ByteBuf ByteBuf} to read from and write
	 *            to.
	 */
	public Packet(ByteBuf buffer) {
		if (buffer == null) {
			throw new IllegalArgumentException("No content");
		} else if (buffer instanceof EmptyByteBuf) {
			throw new IllegalArgumentException("No content");
		}
		this.buffer = buffer;
		this.input = new PacketDataInputStream(this);
		this.output = new PacketDataOutputStream(this);
	}

	/**
	 * Creates packet from an existing
	 * {@link io.netty.channel.socket.DatagramPacket DatagramPacket}.
	 * 
	 * @param datagram
	 *            the {@link io.netty.channel.socket.DatagramPacket
	 *            DatagramPacket} to read from.
	 */
	public Packet(DatagramPacket datagram) {
		this(Unpooled.copiedBuffer(datagram.content()));
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
	 *            the packet whose buffer to copy to read from and write to.
	 */
	public Packet(Packet packet) {
		this(Unpooled.copiedBuffer(packet.copy()));
	}

	/**
	 * Creates an empty packet.
	 */
	public Packet() {
		this(Unpooled.buffer());
	}

	/**
	 * Reads data into the specified <code>byte[]</code>.
	 * 
	 * @param dest
	 *            the <code>byte[]</code> to read the data into.
	 * @return the packet.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than the length of the <code>dest</code>
	 *             readable bytes left in the packet. TODO
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
	 *             if there are less than the <code>length</code> readable bytes
	 *             left in the packet. TODO
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
		buffer.skipBytes(length > this.remaining() ? this.remaining() : length);
	}

	/**
	 * Reads a <code>byte</code>.
	 * 
	 * @return a <code>byte</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>1</code> readable byte left in
	 *             the packet.
	 */
	public final byte readByte() {
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
	public final short readUnsignedByte() {
		return (short) (buffer.readByte() & 0xFF);
	}

	/**
	 * Reads a flipped unsigned <code>byte</code> casted back to a
	 * <code>byte</code>.
	 * 
	 * @return a flipped unsigned <code>byte</code> casted back to a
	 *         <code>byte</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>1</code> readable byte left in
	 *             the packet.
	 */
	private final byte readCastedFlippedUnsignedByte() {
		return (byte) (~buffer.readByte() & 0xFF);
	}

	/**
	 * Returns a byte array of the read flipped unsigned <code>byte</code>s
	 * casted back to a byte.
	 * 
	 * @param length
	 *            the amount of <code>byte</code>s to read.
	 * @return a <code>byte[]</code> of the read flipped unsigned
	 *         <code>byte</code>s casted back to a <code>byte</code>. TODO
	 */
	private final byte[] readCFU(int length) {
		byte[] data = new byte[length];
		for (int i = 0; i < data.length; i++) {
			data[i] = this.readCastedFlippedUnsignedByte();
		}
		return data;
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
	public final boolean readBoolean() {
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
	public final char readChar() {
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
	public final char readCharLE() {
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
	public final short readShort() {
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
	public final short readShortLE() {
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
		return (buffer.readShort() & 0xFFFF);
	}

	/**
	 * Reads an unsigned little-endian <code>short</code>.
	 * 
	 * @return an unsigned little-endian <code>short</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>2</code> readable bytes left in
	 *             the packet.
	 */
	public final int readUnsignedShortLE() {
		return (buffer.readShortLE() & 0xFFFF);
	}

	/**
	 * Reads a <code>triad</code>.
	 * 
	 * @return a <code>triad</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>3</code> readable bytes left in
	 *             the packet.
	 */
	public final int readTriad() {
		return (buffer.readByte() << 16) | (buffer.readByte() << 8) | buffer.readByte();
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
		return buffer.readByte() | (buffer.readByte() << 8) | (buffer.readByte() << 16);
	}

	/**
	 * Reads an unsigned <code>triad</code>.
	 * 
	 * @return an unsigned <code>triad</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>3</code> readable bytes left in
	 *             the packet.
	 */
	public final long readUnsignedTriad() {
		return this.readTriad() & 0x0000000000FFFFFFL;
	}

	/**
	 * Reads an unsigned little-endian <code>triad</code>.
	 * 
	 * @return an unsigned little-endian <code>triad</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>3</code> readable bytes left in
	 *             the packet.
	 */
	public final long readUnsignedTriadLE() {
		return this.readTriadLE() & 0x0000000000FFFFFFL;
	}

	/**
	 * Reads an <code>int</code>.
	 * 
	 * @return an <code>int</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>4</code> readable bytes left in
	 *             the packet.
	 */
	public final int readInt() {
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
	public final int readIntLE() {
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
	public final long readUnsignedInt() {
		return buffer.readInt() & 0x00000000FFFFFFFFL;
	}

	/**
	 * Reads an unsigned little-endian <code>int</code>.
	 * 
	 * @return an unsigned little-endian <code>int</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>4</code> readable bytes left in
	 *             the packet.
	 */
	public final long readUnsignedIntLE() {
		return buffer.readIntLE() & 0x00000000FFFFFFFFL;
	}

	/**
	 * Reads a <code>long</code>.
	 * 
	 * @return a <code>long</code>.
	 * @throws IndexOutOfBoundsException
	 *             if there are less than <code>8</code> readable bytes left in
	 *             the packet.
	 */
	public final long readLong() {
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
	public final long readLongLE() {
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
	public final BigInteger readUnsignedLong() {
		byte[] ulBytes = this.read(8);
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
	public final BigInteger readUnsignedLongLE() {
		byte[] ulBytesReversed = this.read(8);
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
	public final float readFloat() {
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
	public final float readFloatLE() {
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
	public final double readDouble() {
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
	public final double readDoubleLE() {
		return buffer.readDoubleLE();
	}

	/**
	 * Reads a UTF-8 <code>String</code> with its length prefixed by an unsigned
	 * <code>short</code>.
	 * 
	 * @return a <code>String</code>. TODO
	 */
	public final String readString() {
		int len = this.readUnsignedShort();
		byte[] data = this.read(len);
		return new String(data);
	}

	/**
	 * Reads a UTF-8 <code>String</code> with its length prefixed by a unsigned
	 * little -endian <code>short</code>.
	 * 
	 * @return a <code>String</code>. TODO
	 */
	public final String readStringLE() {
		int len = this.readUnsignedShortLE();
		byte[] data = this.read(len);
		return new String(data);
	}

	/**
	 * Reads an IPv4/IPv6 address.
	 * 
	 * @return an IPv4/IPv6 address.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address. TODO
	 */
	public final InetSocketAddress readAddress() throws UnknownHostException {
		short version = this.readUnsignedByte();
		if (version == ADDRESS_VERSION_IPV4) {
			byte[] addressBytes = this.readCFU(ADDRESS_VERSION_IPV4_LENGTH);
			int port = this.readUnsignedShort();
			return new InetSocketAddress(InetAddress.getByAddress(addressBytes), port);
		} else if (version == ADDRESS_VERSION_IPV6) {
			// Read data
			byte[] addressBytes = this.readCFU(ADDRESS_VERSION_IPV6_LENGTH);
			this.read(ADDRESS_VERSION_IPV6_MYSTERY_LENGTH); // Mystery bytes
			int port = this.readUnsignedShort();
			return new InetSocketAddress(InetAddress.getByAddress(Arrays.copyOfRange(addressBytes, 0, 16)), port);
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
	public final UUID readUUID() {
		long mostSignificantBits = this.readLong();
		long leastSignificantBits = this.readLong();
		return new UUID(mostSignificantBits, leastSignificantBits);
	}

	/**
	 * Writes the <code>byte[]</code> to the packet.
	 * 
	 * @param data
	 *            the data to write.
	 * @return the packet.
	 */
	public final Packet write(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			buffer.writeByte(data[i]);
		}
		return this;
	}

	/**
	 * Writes the specified amount of <code>null (0x00)</code> bytes to the
	 * packet.
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
	 */
	public final Packet writeUnsignedByte(int b) {
		buffer.writeByte(((byte) b) & 0xFF);
		return this;
	}

	/**
	 * Writes a flipped unsigned <code>byte</code> casted back into a byte to
	 * the packet.
	 * 
	 * @param b
	 *            the <code>byte</code>
	 * @return the packet.
	 */
	private final Packet writeCFUByte(byte b) {
		buffer.writeByte(~b & 0xFF);
		return this;
	}

	/**
	 * Writes a byte array of the flipped unsigned <code>byte</code>s casted
	 * back to a byte to the packet.
	 * 
	 * @param data
	 *            the data to write.
	 * @return the packet.
	 */
	private final Packet writeCFU(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			this.writeCFUByte(data[i]);
		}
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
	 *             if <code>s</code> is less than <code>0</code>.
	 */
	public final Packet writeUnsignedShort(int s) throws IllegalArgumentException {
		if (s < 0) {
			throw new IllegalArgumentException("Value must be greater than or equal to 0");
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
	 *             if <code>s</code> is less than <code>0</code>.
	 */
	public final Packet writeUnsignedShortLE(int s) throws IllegalArgumentException {
		if (s < 0) {
			throw new IllegalArgumentException("Value must be greater than or equal to 0");
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
		buffer.writeByte((byte) (t >> 16));
		buffer.writeByte((byte) (t >> 8));
		buffer.writeByte((byte) t);
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
		buffer.writeByte((byte) t);
		buffer.writeByte((byte) (t >> 8));
		buffer.writeByte((byte) (t >> 16));
		return this;
	}

	/**
	 * Writes an unsigned <code>triad</code> to the packet.
	 * 
	 * @param t
	 *            the <code>triad</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>t</code> is less than <code>0</code>.
	 */
	public final Packet writeUnsignedTriad(int t) {
		if (t < 0) {
			throw new IllegalArgumentException("Value must be greater than or equal to 0");
		}
		return this.writeTriad(t & 0x00FFFFFF);
	}

	/**
	 * Writes an unsigned little-endian <code>triad</code> to the packet.
	 * 
	 * @param t
	 *            the <code>triad</code>.
	 * @return the packet.
	 * @throws IllegalArgumentException
	 *             if <code>t</code> is less than <code>0</code>.
	 */
	public final Packet writeUnsignedTriadLE(int t) {
		if (t < 0) {
			throw new IllegalArgumentException("Value must be greater than or equal to 0");
		}
		return this.writeTriadLE(t & 0x00FFFFFF);
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
	 */
	public final Packet writeUnsignedInt(long i) {
		if (i < 0) {
			throw new IllegalArgumentException("Value must be greater than or equal to 0");
		}
		buffer.writeInt(((int) i) & 0xFFFFFFFF);
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
	 */
	public final Packet writeUnsignedIntLE(long i) {
		if (i < 0) {
			throw new IllegalArgumentException("Value must be greater than or equal to 0");
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
	 */
	public final Packet writeUnsignedLong(BigInteger bi) {
		if (bi.longValue() < 0) {
			throw new IllegalArgumentException("Value must be greater than or equal to 0");
		}
		byte[] ulBytes = bi.toByteArray();
		if (ulBytes.length > Long.BYTES) {
			throw new IllegalArgumentException("BigInteger is too big to fit into a long");
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
	 */
	public final Packet writeUnsignedLong(long l) {
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
	 *             {@value Long#BYTES}.
	 */
	public final Packet writeUnsignedLongLE(BigInteger bi) throws IllegalArgumentException {
		if (bi.longValue() < 0) {
			throw new IllegalArgumentException("Value must be greater than or equal to 0");
		}
		byte[] ulBytes = bi.toByteArray();
		if (ulBytes.length > Long.BYTES) {
			throw new IllegalArgumentException("BigInteger is too big to fit into a long");
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
	 */
	public final Packet writeUnsignedLongLE(long l) {
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
	 * Writes a UTF-8 <code>String</code> prefixed by an unsigned
	 * <code>short</code> to the packet.
	 * 
	 * @param s
	 *            the <code>String</code>.
	 * @return the packet.
	 */
	public final Packet writeString(String s) {
		byte[] data = s.getBytes();
		this.writeUnsignedShort(data.length);
		this.write(data);
		return this;
	}

	/**
	 * Writes a UTF-8 <code>String</code> prefixed by a little-endian unsigned
	 * <code>short</code> to the packet.
	 * 
	 * @param s
	 *            the <code>String</code>.
	 * @return the packet.
	 */
	public final Packet writeStringLE(String s) {
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
	 *             if the <code>address</code> is <code>null</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, if
	 *             a scope_id was specified for a global IPv6 address, or the
	 *             length of the address is not either
	 *             {@value #ADDRESS_VERSION_IPV4_LENGTH} or
	 *             <code>{@value #ADDRESS_VERSION_IPV6_LENGTH}</code>
	 *             <code>byte</code>s.
	 */
	public final Packet writeAddress(InetSocketAddress address) throws NullPointerException, UnknownHostException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		}
		byte[] addressBytes = address.getAddress().getAddress();
		if (addressBytes.length == ADDRESS_VERSION_IPV4_LENGTH) {
			this.writeUnsignedByte(ADDRESS_VERSION_IPV4);
			this.writeCFU(addressBytes);
			this.writeUnsignedShort(address.getPort());
		} else if (addressBytes.length == ADDRESS_VERSION_IPV6_LENGTH) {
			this.writeUnsignedByte(ADDRESS_VERSION_IPV6);
			this.writeCFU(addressBytes);
			this.pad(ADDRESS_VERSION_IPV6_MYSTERY_LENGTH); // Mystery bytes
			this.writeUnsignedShort(address.getPort());
		} else {
			throw new UnknownHostException("Unknown protocol IPv" + addressBytes.length);
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
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 *             TODO
	 */
	public final Packet writeAddress(InetAddress host, int port) throws UnknownHostException {
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
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 *             TODO
	 */
	public final Packet writeAddress(String host, int port) throws UnknownHostException {
		return this.writeAddress(InetAddress.getByName(host), port);
	}

	/**
	 * Writes a <code>UUID</code> to the packet.
	 * 
	 * @param uuid
	 *            the <code>UUID</code>.
	 * @return the packet.
	 * TODO
	 */
	public final Packet writeUUID(UUID uuid) {
		this.writeLong(uuid.getMostSignificantBits());
		this.writeLong(uuid.getLeastSignificantBits());
		return this;
	}

	/**
	 * Returns the packet as a <code>byte[]</code>.
	 * 
	 * @return the packet as a <code>byte[]</code>.
	 */
	public byte[] array() {
		if (buffer.isDirect()) {
			return null;
		}
		return Arrays.copyOfRange(buffer.array(), 0, buffer.writerIndex());
	}

	/**
	 * Returns the size of the packet in <code>byte</code>s.
	 * 
	 * @return the size of the packet in <code>byte</code>s.
	 */
	public int size() {
		return array().length;
	}

	/**
	 * Returns the packet buffer.
	 * 
	 * @return the packet buffer.
	 */
	public ByteBuf buffer() {
		return buffer.retain();
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
	 *            the buffer to read from and write to.
	 * @return the packet.
	 */
	public final Packet setBuffer(ByteBuf buffer) {
		this.buffer = buffer;
		return this;
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param datagram
	 *            the {@link io.netty.channel.socket.DatagramPacket
	 *            DatagramPacket} to read from.
	 * @return the packet.
	 * TODO
	 */
	public final Packet setBuffer(DatagramPacket datagram) {
		return this.setBuffer(datagram.content());
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param data
	 *            the <code>byte[]</code> to read from.
	 * @return the packet.
	 * TODO
	 */
	public final Packet setBuffer(byte[] buffer) {
		return this.setBuffer(Unpooled.copiedBuffer(buffer));
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param packet
	 *            the packet whose buffer to copy to read from and write to.
	 * @return the packet.
	 * TODO
	 */
	public final Packet setBuffer(Packet packet) {
		return this.setBuffer(packet.copy());
	}

	/**
	 * Flips the packet.
	 * 
	 * @return the packet.
	 */
	public Packet flip() {
		byte[] data = buffer.array();
		this.buffer = Unpooled.copiedBuffer(data);
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
