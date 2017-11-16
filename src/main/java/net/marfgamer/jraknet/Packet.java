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
 * Copyright (c) 2016, 2017 Trent "MarfGamer" Summerlin
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
package net.marfgamer.jraknet;

import static net.marfgamer.jraknet.protocol.MessageIdentifier.*;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.jraknet.protocol.ConnectionType;
import net.marfgamer.jraknet.stream.PacketDataInput;
import net.marfgamer.jraknet.stream.PacketDataOutput;

/**
 * Used to read and write data with ease.
 *
 * @author Trent "MarfGamer" Summerlin
 */
public class Packet {

	// Logger name
	private static final String LOGGER_NAME = "packet";

	// Packet constants
	public static final int ADDRESS_VERSION_IPV4 = 0x04;
	public static final int ADDRESS_VERSION_IPV6 = 0x06;
	public static final int ADDRESS_VERSION_IPV4_LENGTH = 0x04;
	public static final int ADDRESS_VERSION_IPV6_LENGTH = 0x10;
	public static final int ADDRESS_VERSION_IPV6_MYSTERY_LENGTH = 0x0A;

	// Packet data
	private ByteBuf buffer;
	private PacketDataInput input;
	private PacketDataOutput output;

	/**
	 * Constructs a <code>Packet</code> that reads from and writes to the specified
	 * <code>ByteBuf</code>.
	 * 
	 * @param buffer
	 *            the <code>ByteBuf</code> to read from and write to.
	 */
	public Packet(ByteBuf buffer) {
		this.buffer = buffer;
		this.input = new PacketDataInput(this);
		this.output = new PacketDataOutput(this);
	}

	/**
	 * Constructs a <code>Packet</code> that reads from and writes to the specified
	 * <code>DatagramPacket</code>
	 * 
	 * @param datagram
	 *            the <code>DatagramPacket</code> to read from and write to.
	 */
	public Packet(DatagramPacket datagram) {
		this(Unpooled.copiedBuffer(datagram.content()));
	}

	/**
	 * Constructs a <code>Packet</code> that reads from and writes to the specified
	 * byte array.
	 * 
	 * @param data
	 *            the byte[] to read from and write to.
	 */
	public Packet(byte[] data) {
		this(Unpooled.copiedBuffer(data));
	}

	/**
	 * Constructs a <code>Packet</code> that reads from and writes to the specified
	 * <code>Packet</code>.
	 * 
	 * @param packet
	 *            the <code>Packet</code> to read from and write to.
	 */
	public Packet(Packet packet) {
		this(Unpooled.copiedBuffer(packet.buffer));
	}

	/**
	 * Constructs a blank <code>Packet</code> using an empty <code>ByteBuf</code>.
	 */
	public Packet() {
		this(Unpooled.buffer());
	}

	/**
	 * Reads data into the specified byte array.
	 * 
	 * @param dest
	 *            the bytes to read the data into.
	 * @return the packet;
	 */
	public Packet read(byte[] dest) {
		for (int i = 0; i < dest.length; i++) {
			dest[i] = buffer.readByte();
		}
		return this;
	}

	/**
	 * Returns a byte array of the read data with the specified size.
	 * 
	 * @param length
	 *            the amount of bytes to read.
	 * @return a byte array of the read data with the specified size.
	 */
	public byte[] read(int length) {
		byte[] data = new byte[length];
		for (int i = 0; i < data.length; i++) {
			data[i] = buffer.readByte();
		}
		return data;
	}

	/**
	 * Reads a byte.
	 * 
	 * @return a byte.
	 */
	public byte readByte() {
		return buffer.readByte();
	}

	/**
	 * Reads an unsigned byte.
	 * 
	 * @return an unsigned byte.
	 */
	public short readUnsignedByte() {
		return (short) (buffer.readByte() & 0xFF);
	}

	/**
	 * Reads a flipped unsigned byte casted back to a byte.
	 * 
	 * @return a flipped unsigned byte casted back to a byte.
	 */
	private byte readCFUByte() {
		return (byte) (~buffer.readByte() & 0xFF);
	}

	/**
	 * Returns a byte array of the read flipped unsigned byte's casted back to a
	 * byte.
	 * 
	 * @param length
	 *            the amount of bytes to read.
	 * @return a byte array of the read flipped unsigned byte's casted back to a
	 *         byte with the specified size.
	 */
	private byte[] readCFU(int length) {
		byte[] data = new byte[length];
		for (int i = 0; i < data.length; i++) {
			data[0] = this.readCFUByte();
		}
		return data;
	}

	/**
	 * Reads a boolean (Anything larger than 0 is considered true).
	 * 
	 * @return a boolean.
	 */
	public boolean readBoolean() {
		return (this.readUnsignedByte() > 0x00);
	}

	/**
	 * Reads a short.
	 * 
	 * @return a short.
	 */
	public short readShort() {
		return buffer.readShort();
	}

	/**
	 * Reads a little endian short.
	 * 
	 * @return a little endian short.
	 */
	public short readShortLE() {
		return buffer.readShortLE();
	}

	/**
	 * Reads an unsigned short.
	 * 
	 * @return an unsigned short.
	 */
	public int readUnsignedShort() {
		return (buffer.readShort() & 0xFFFF);
	}

	/**
	 * Reads an unsigned little endian short.
	 * 
	 * @return an unsigned little endian short.
	 */
	public int readUnsignedShortLE() {
		return (buffer.readShortLE() & 0xFFFF);
	}

	/**
	 * Reads a little endian triad.
	 * 
	 * @return a little endian triad.
	 */
	public int readTriadLE() {
		return (buffer.readByte() & 0xFF) | ((buffer.readByte() & 0xFF) << 8) | ((buffer.readByte() & 0x0F) << 16);
	}

	/**
	 * Reads an integer.
	 * 
	 * @return an integer.
	 */
	public int readInt() {
		return buffer.readInt();
	}

	/**
	 * Reads a little endian integer.
	 * 
	 * @return a little endian integer.
	 */
	public int readIntLE() {
		return buffer.readIntLE();
	}

	/**
	 * Reads an unsigned integer.
	 * 
	 * @return an unsigned integer.
	 */
	public long readUnsignedInt() {
		return (buffer.readInt() & 0x00000000FFFFFFFFL);
	}

	/**
	 * Reads an unsigned little endian integer.
	 * 
	 * @return an unsigned little endian integer.
	 */
	public long readUnsignedIntLE() {
		return (buffer.readIntLE() & 0x00000000FFFFFFFFL);
	}

	/**
	 * Reads a long.
	 * 
	 * @return a long.
	 */
	public long readLong() {
		return buffer.readLong();
	}

	/**
	 * Reads a little endian long.
	 * 
	 * @return a little endian long.
	 */
	public long readLongLE() {
		return buffer.readLongLE();
	}

	/**
	 * Reads an unsigned long.
	 * 
	 * @return an unsigned long.
	 */
	public BigInteger readUnsignedLong() {
		byte[] ulBytes = this.read(8);
		return new BigInteger(ulBytes);
	}

	/**
	 * Reads an unsigned little endian long.
	 * 
	 * @return an unsigned little endian long.
	 */
	public BigInteger readUnsignedLongLE() {
		byte[] ulBytesReversed = this.read(8);
		byte[] ulBytes = new byte[ulBytesReversed.length];
		for (int i = 0; i < ulBytes.length; i++) {
			ulBytes[i] = ulBytesReversed[ulBytesReversed.length - i - 1];
		}
		return new BigInteger(ulBytes);
	}

	/**
	 * Reads a float.
	 * 
	 * @return a float.
	 */
	public float readFloat() {
		return buffer.readFloat();
	}

	/**
	 * Reads a double.
	 * 
	 * @return a double.
	 */
	public double readDouble() {
		return buffer.readDouble();
	}

	/**
	 * Reads a magic array and returns whether or not it is valid.
	 * 
	 * @return whether or not the magic array was valid.
	 */
	public boolean checkMagic() {
		byte[] magicCheck = this.read(MAGIC.length);
		return Arrays.equals(MAGIC, magicCheck);
	}

	/**
	 * Reads a UTF-8 String with it's length prefixed by a unsigned short.
	 * 
	 * @return a String.
	 */
	public String readString() {
		int len = this.readUnsignedShort();
		byte[] data = this.read(len);
		return new String(data);
	}

	/**
	 * Reads a UTF-8 String with it's length prefixed by a unsigned little endian
	 * short.
	 * 
	 * @return a String.
	 */
	public String readStringLE() {
		int len = this.readUnsignedShortLE();
		byte[] data = this.read(len);
		return new String(data);
	}

	/**
	 * Reads an IPv4/IPv6 address.
	 * 
	 * @return an IPv4/IPv6 address.
	 * @throws UnknownHostException
	 *             if an error occurs when reading the address.
	 */
	public InetSocketAddress readAddress() throws UnknownHostException {
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
	 * Reads and returns the connection type. Unlike most other methods, this one
	 * will check to make sure if there is enough data to read the connection type
	 * before actually reading it. This is because it is meant to be used strictly
	 * at the end of packets that can be used to signify the protocol implementation
	 * of the sender.
	 * 
	 * @return the connection type.
	 */
	public ConnectionType readConnectionType() {
		// We add an extra byte because we need to read the ID
		if (this.remaining() >= ConnectionType.MAGIC.length + 1) {
			byte[] connectionMagicCheck = this.read(ConnectionType.MAGIC.length);
			if (Arrays.equals(ConnectionType.MAGIC, connectionMagicCheck)) {
				short id = this.readUnsignedByte();
				if (id == ConnectionType.VANILLA.getId()) {
					RakNetLogger.debug(LOGGER_NAME, "Connection type " + ConnectionType.VANILLA
							+ " returned after connection type magic sequence");
				}
				return ConnectionType.getType(id);
			}
		}
		return ConnectionType.VANILLA;
	}

	/**
	 * Writes the specified byte array to the packet.
	 * 
	 * @param data
	 *            the data to write.
	 * @return the packet.
	 */
	public Packet write(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			buffer.writeByte(data[i]);
		}
		return this;
	}

	/**
	 * Writes the specified amount of null (0x00) bytes to the packet.
	 * 
	 * @param length
	 *            the amount of bytes to write.
	 * @return the packet.
	 */
	public Packet pad(int length) {
		for (int i = 0; i < length; i++) {
			buffer.writeByte(0x00);
		}
		return this;
	}

	/**
	 * Writes a byte to the packet.
	 * 
	 * @param b
	 *            the byte.
	 * @return the packet.
	 */
	public Packet writeByte(int b) {
		buffer.writeByte((byte) b);
		return this;
	}

	/**
	 * Writes an unsigned by to the packet.
	 * 
	 * @param b
	 *            the unsigned byte.
	 * @return the packet.
	 */
	public Packet writeUnsignedByte(int b) {
		buffer.writeByte(((byte) b) & 0xFF);
		return this;
	}

	/**
	 * Writes a flipped unsigned byte casted back into a byte to the packet.
	 * 
	 * @param b
	 *            the byte
	 * @return the packet.
	 */
	private Packet writeCFUByte(byte b) {
		buffer.writeByte(~b & 0xFF);
		return this;
	}

	/**
	 * Writes a byte array of the specified flipped unsigned byte's casted back to a
	 * byte to the packet.
	 * 
	 * @param data
	 *            the data to write.
	 * @return the packet.
	 */
	private Packet writeCFU(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			this.writeCFUByte(data[i]);
		}
		return this;
	}

	/**
	 * Writes a boolean value to the packet.
	 * 
	 * @param b
	 *            the boolean.
	 * @return the packet.
	 */
	public Packet writeBoolean(boolean b) {
		buffer.writeByte(b ? 0x01 : 0x00);
		return this;
	}

	/**
	 * Writes a short to the packet.
	 * 
	 * @param s
	 *            the short.
	 * @return the packet.
	 */
	public Packet writeShort(int s) {
		buffer.writeShort(s);
		return this;
	}

	/**
	 * Writes a little endian short to the packet.
	 * 
	 * @param s
	 *            the short.
	 * @return the packet.
	 */
	public Packet writeShortLE(int s) {
		buffer.writeShortLE(s);
		return this;
	}

	/**
	 * Writes a unsigned short to the packet.
	 * 
	 * @param s
	 *            the short.
	 * @return the packet.
	 */
	public Packet writeUnsignedShort(int s) {
		buffer.writeShort(((short) s) & 0xFFFF);
		return this;
	}

	/**
	 * Writes an unsigned little endian short to the packet.
	 * 
	 * @param s
	 *            the short.
	 * @return the packet.
	 */
	public Packet writeUnsignedShortLE(int s) {
		buffer.writeShortLE(((short) s) & 0xFFFF);
		return this;
	}

	/**
	 * Writes a little endian triad to the packet.
	 * 
	 * @param t
	 *            the triad.
	 * @return the packet.
	 */
	public Packet writeTriadLE(int t) {
		buffer.writeByte((byte) (t & 0xFF));
		buffer.writeByte((byte) ((t >> 8) & 0xFF));
		buffer.writeByte((byte) ((t >> 16) & 0xFF));
		return this;
	}

	/**
	 * Writes an integer to the packet.
	 * 
	 * @param i
	 *            the integer.
	 * @return the packet.
	 */
	public Packet writeInt(int i) {
		buffer.writeInt(i);
		return this;
	}

	/**
	 * Writes an unsigned integer to the packet.
	 * 
	 * @param i
	 *            the integer.
	 * @return the packet.
	 */
	public Packet writeUnsignedInt(long i) {
		buffer.writeInt(((int) i) & 0xFFFFFFFF);
		return this;
	}

	/**
	 * Writes a little endian integer to the packet.
	 * 
	 * @param i
	 *            the integer.
	 * @return the packet.
	 */
	public Packet writeIntLE(int i) {
		buffer.writeIntLE(i);
		return this;
	}

	/**
	 * Writes an unsigned little endian integer to the packet.
	 * 
	 * @param i
	 *            the integer.
	 * @return the packet.
	 */
	public Packet writeUnsignedIntLE(long i) {
		buffer.writeIntLE(((int) i) & 0xFFFFFFFF);
		return this;
	}

	/**
	 * Writes a long to the packet.
	 * 
	 * @param l
	 *            the long.
	 * @return the packet.
	 */
	public Packet writeLong(long l) {
		buffer.writeLong(l);
		return this;
	}

	/**
	 * Writes a little endian long to the packet.
	 * 
	 * @param l
	 *            the long.
	 * @return the packet.
	 */
	public Packet writeLongLE(long l) {
		buffer.writeLongLE(l);
		return this;
	}

	/**
	 * Writes an unsigned long to the packet.
	 * 
	 * @param bi
	 *            the long.
	 * @return the packet.
	 */
	public Packet writeUnsignedLong(BigInteger bi) {
		byte[] ulBytes = bi.toByteArray();
		if (ulBytes.length > 8) {
			throw new IllegalArgumentException("BigInteger is too big to fit into a long");
		}
		for (int i = 0; i < 8 - ulBytes.length; i++) {
			this.writeByte(0x00);
		}
		for (int i = 0; i < ulBytes.length; i++) {
			this.writeByte(ulBytes[i]);
		}
		return this;
	}

	/**
	 * Writes an unsigned long to the packet.
	 * 
	 * @param l
	 *            the long.
	 * @return the packet.
	 */
	public Packet writeUnsignedLong(long l) {
		return this.writeUnsignedLong(new BigInteger(Long.toString(l)));
	}

	/**
	 * Writes an unsigned little endian long to the packet.
	 * 
	 * @param bi
	 *            the long.
	 * @return the packet.
	 */
	public Packet writeUnsignedLongLE(BigInteger bi) {
		byte[] ulBytes = bi.toByteArray();
		if (ulBytes.length > 8) {
			throw new IllegalArgumentException("BigInteger is too big to fit into a long");
		}
		for (int i = ulBytes.length - 1; i >= 0; i--) {
			this.writeByte(ulBytes[i]);
		}
		for (int i = 0; i < 8 - ulBytes.length; i++) {
			this.writeByte(0x00);
		}
		return this;
	}

	/**
	 * Writes an unsigned little endian long to the packet.
	 * 
	 * @param l
	 *            the long.
	 * @return the packet.
	 */
	public Packet writeUnsignedLongLE(long l) {
		return this.writeUnsignedLongLE(new BigInteger(Long.toString(l)));
	}

	/**
	 * Writes a float to the packet.
	 * 
	 * @param f
	 *            the float.
	 * @return the packet.
	 */
	public Packet writeFloat(double f) {
		buffer.writeFloat((float) f);
		return this;
	}

	/**
	 * Writes a double to the packet.
	 * 
	 * @param d
	 *            the double.
	 * @return the packet.
	 */
	public Packet writeDouble(double d) {
		buffer.writeDouble(d);
		return this;
	}

	/**
	 * Writes the magic sequence to the packet.
	 * 
	 * @return the packet.
	 */
	public Packet writeMagic() {
		this.write(MAGIC);
		return this;
	}

	/**
	 * Writes a UTF-8 String prefixed by an unsigned short to the packet.
	 * 
	 * @param s
	 *            the String.
	 * @return the packet.
	 */
	public Packet writeString(String s) {
		byte[] data = s.getBytes();
		this.writeUnsignedShort(data.length);
		this.write(data);
		return this;
	}

	/**
	 * Writes a UTF-8 String prefixed by a little endian unsigned short to the
	 * packet.
	 * 
	 * @param s
	 *            the String.
	 * @return the packet.
	 */
	public Packet writeStringLE(String s) {
		byte[] data = s.getBytes();
		this.writeUnsignedShortLE(data.length);
		this.write(data);
		return this;
	}

	/**
	 * Writes an IPv4 address to the packet (Writing IPv6 is not yet supported).
	 * 
	 * @param address
	 *            the address.
	 * @return the packet.
	 * @throws UnknownHostException
	 *             if an error occurs when reading the address.
	 */
	public Packet writeAddress(InetSocketAddress address) throws UnknownHostException {
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
	 * Writes an IPv4 address to the packet (IPv6 is not yet supported).
	 * 
	 * @param address
	 *            the address.
	 * @param port
	 *            the port.
	 * @return the packet.
	 * @throws UnknownHostException
	 *             if an error occurs when reading the address.
	 */
	public Packet writeAddress(InetAddress address, int port) throws UnknownHostException {
		return this.writeAddress(new InetSocketAddress(address, port));
	}

	/**
	 * Writes an IPv4 address to the packet (IPv6 is not yet supported).
	 * 
	 * @param address
	 *            the address.
	 * @param port
	 *            the port.
	 * @return the packet.
	 * @throws UnknownHostException
	 *             if an error occurs when reading the address.
	 */
	public Packet writeAddress(String address, int port) throws UnknownHostException {
		return this.writeAddress(new InetSocketAddress(address, port));
	}

	/**
	 * Writes the connection type magic sequence and the ID of the JRakNet RakNet
	 * protocol implementation.
	 * 
	 * @return the connection type.
	 */
	public ConnectionType writeConnectionType() {
		this.write(ConnectionType.MAGIC);
		this.writeUnsignedByte(ConnectionType.JRAKNET.getId());
		return ConnectionType.JRAKNET;
	}

	/**
	 * @return the packet as a byte array.
	 */
	public byte[] array() {
		if (buffer.isDirect()) {
			return null;
		}
		return Arrays.copyOfRange(buffer.array(), 0, buffer.writerIndex());
	}

	/**
	 * @return the size of the packet in bytes.
	 */
	public int size() {
		return array().length;
	}

	/**
	 * @return the packet's buffer.
	 */
	public ByteBuf buffer() {
		return this.buffer.retain();
	}

	/**
	 * @return the packet's input.
	 */
	public PacketDataInput getDataInput() {
		return this.input;
	}

	/**
	 * @return the packet's output.
	 */
	public PacketDataOutput getDataOutput() {
		return this.output;
	}

	/**
	 * @return how many bytes are left in the packet's buffer.
	 */
	public int remaining() {
		return buffer.readableBytes();
	}

	/**
	 * Sets the buffer of the packet
	 * 
	 * @param buffer
	 *            the new buffer.
	 * @return the packet.
	 */
	public final Packet setBuffer(byte[] buffer) {
		this.buffer = Unpooled.copiedBuffer(buffer);
		return this;
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
	 * Clears the packets buffer.
	 * 
	 * @return the packet.
	 */
	public Packet clear() {
		buffer.clear();
		return this;
	}

}
