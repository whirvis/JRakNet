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
package net.marfgamer.raknet.protocol;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.marfgamer.raknet.protocol.identifier.MessageIdentifiers;

/**
 * Used to read and write data for RakNet packets with ease which all begin with
 * an unsigned byte for their ID.
 *
 * @author Whirvis T. Wheatley
 */
public class Message implements MessageIdentifiers {

	private ByteBuf buffer;
	private short id;

	public Message(short id) {
		this.buffer = Unpooled.buffer();
		this.id = id;
		this.putUByte(id);
	}

	public Message(ByteBuf buffer) {
		this.buffer = buffer;
		this.id = this.getUByte();
	}
	
	public Message(Message packet) {
		this.buffer = packet.buffer();
		this.id = packet.id;
	}

	public final short getId() {
		return this.id;
	}

	public void encode() {
	}

	public void decode() {
	}

	public void get(byte[] dest) {
		for (int i = 0; i < dest.length; i++) {
			dest[i] = buffer.readByte();
		}
	}

	public byte[] get(int length) {
		byte[] data = new byte[length];
		for (int i = 0; i < data.length; i++) {
			data[i] = buffer.readByte();
		}
		return data;
	}

	public byte getByte() {
		return buffer.readByte();
	}

	public short getUByte() {
		return (short) (buffer.readByte() & 0xFF);
	}

	public boolean getBoolean() {
		return (this.getUByte() > 0x00);
	}

	public short getShort() {
		return buffer.readShort();
	}

	public int getUShort() {
		return (buffer.readShort() & 0xFFFF);
	}

	public int getLTriad() {
		return (0xFF & buffer.readByte()) | (0xFF00 & (buffer.readByte() << 8))
				| (0xFF0000 & (buffer.readByte() << 16));
	}

	public int getInt() {
		return buffer.readInt();
	}

	public long getLong() {
		return buffer.readLong();
	}

	public float getFloat() {
		return buffer.readFloat();
	}

	public double getDouble() {
		return buffer.readDouble();
	}

	public boolean checkMagic() {
		byte[] magicCheck = this.get(MAGIC.length);
		return Arrays.equals(magicCheck, MAGIC);
	}

	public String getString() {
		int len = this.getUShort();
		byte[] data = this.get(len);
		return new String(data);
	}

	public InetSocketAddress getAddress() {
		short version = this.getUByte();
		if (version == 4) {
			String address = ((~this.getByte()) & 0xFF) + "." + ((~this.getByte()) & 0xFF) + "."
					+ ((~this.getByte()) & 0xFF) + "." + ((~this.getByte()) & 0xFF);
			int port = this.getUShort();
			return new InetSocketAddress(address, port);
		} else if (version == 6) {
			throw new UnsupportedOperationException("Can't read IPv6 address: Not Implemented");
		} else {
			throw new UnsupportedOperationException("Can't read IPv" + version + " address: unknown");
		}
	}

	public Message put(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			buffer.writeByte(data[i]);
		}
		return this;
	}

	public Message pad(int length) {
		for (int i = 0; i < length; i++) {
			buffer.writeByte(0x00);
		}
		return this;
	}

	public Message putByte(int b) {
		buffer.writeByte((byte) b);
		return this;
	}

	public Message putUByte(int b) {
		buffer.writeByte(((byte) b) & 0xFF);
		return this;
	}

	public void putBoolean(boolean b) {
		this.putUByte(b ? 0x01 : 0x00);
	}

	public Message putShort(int s) {
		buffer.writeShort(s);
		return this;
	}

	public Message putUShort(int s) {
		buffer.writeShort(((short) s) & 0xFFFF);
		return this;
	}

	public Message putLTriad(int t) {
		buffer.writeByte(t << 0);
		buffer.writeByte(t << 8);
		buffer.writeByte(t << 16);
		return this;
	}

	public Message putInt(int i) {
		buffer.writeInt(i);
		return this;
	}

	public Message putLong(long l) {
		buffer.writeLong(l);
		return this;
	}

	public Message putFloat(double f) {
		buffer.writeFloat((float) f);
		return this;
	}

	public Message putDouble(double d) {
		buffer.writeDouble(d);
		return this;
	}

	public Message putMagic() {
		this.put(MAGIC);
		return this;
	}

	public Message putString(String s) {
		byte[] data = s.getBytes();
		this.putUShort(data.length);
		this.put(data);
		return this;
	}

	public void putAddress(InetSocketAddress address) {
		this.putUByte(4);
		for (String part : address.getAddress().getHostAddress().split(Pattern.quote("."))) {
			this.putByte((byte) ((byte) ~(Integer.parseInt(part)) & 0xFF));
		}
		this.putUShort(address.getPort());
	}

	public void putAddress(String address, int port) {
		this.putAddress(new InetSocketAddress(address, port));
	}
	
	public byte[] array() {
		return Arrays.copyOfRange(buffer.array(), 0, buffer.writerIndex());
	}

	public int length() {
		return array().length;
	}

	public ByteBuf buffer() {
		return this.buffer.retain();
	}

	public int remaining() {
		return buffer.readableBytes();
	}

}