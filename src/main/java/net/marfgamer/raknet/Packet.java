package net.marfgamer.raknet;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;

public class Packet {

	public static final int ADDRESS_VERSION = 0x04;

	private final ByteBuf buffer;

	public Packet(ByteBuf buffer) {
		this.buffer = buffer;
	}

	public Packet(DatagramPacket datagram) {
		this(Unpooled.copiedBuffer(datagram.content()));
	}

	public Packet(Packet packet) {
		this(Unpooled.copiedBuffer(packet.buffer));
	}

	public Packet() {
		this(Unpooled.buffer());
	}

	public void read(byte[] dest) {
		for (int i = 0; i < dest.length; i++) {
			dest[i] = buffer.readByte();
		}
	}

	public byte[] read(int length) {
		byte[] data = new byte[length];
		for (int i = 0; i < data.length; i++) {
			data[i] = buffer.readByte();
		}
		return data;
	}

	public byte readByte() {
		return buffer.readByte();
	}

	public short readUByte() {
		return (short) (buffer.readByte() & 0xFF);
	}

	public boolean readBoolean() {
		return (this.readUByte() > 0x00);
	}

	public short readShort() {
		return buffer.readShort();
	}

	public short readShortLE() {
		return buffer.readShortLE();
	}

	public int readUShort() {
		return (buffer.readShort() & 0xFFFF);
	}

	public int readUShortLE() {
		return (buffer.readShortLE() & 0xFFFF);
	}

	public int readTriadLE() {
		return (buffer.readByte() & 0xFF) | ((buffer.readByte() & 0xFF) << 8) | ((buffer.readByte() & 0x0F) << 16);
	}

	public int readInt() {
		return buffer.readInt();
	}

	public int readIntLE() {
		return buffer.readIntLE();
	}

	public int readUInt() {
		return (buffer.readInt() & 0xFFFFFFFF);
	}

	public int readUIntLE() {
		return (buffer.readIntLE() & 0xFFFFFFFF);
	}

	public long readLong() {
		return buffer.readLong();
	}

	public long readLongLE() {
		return buffer.readLongLE();
	}

	public float readFloat() {
		return buffer.readFloat();
	}

	public double readDouble() {
		return buffer.readDouble();
	}

	public boolean checkMagic() {
		byte[] magicCheck = this.read(MAGIC.length);
		return Arrays.equals(MAGIC, magicCheck);
	}

	public String readString() {
		int len = this.readUShort();
		byte[] data = this.read(len);
		return new String(data);
	}

	public String readStringLE() {
		int len = this.readUShortLE();
		byte[] data = this.read(len);
		return new String(data);
	}

	public InetSocketAddress readAddress() {
		short version = this.readUByte();
		if (version == ADDRESS_VERSION) {
			byte[] addressBytes = new byte[4];
			for (int i = 0; i < addressBytes.length; i++) {
				addressBytes[i] = (byte) (~this.readByte() & 0xFF);
			}
			int port = this.readUShort();

			try {
				return new InetSocketAddress(InetAddress.getByAddress(addressBytes), port);
			} catch (UnknownHostException e) {
				return null;
			}
		} else {
			throw new IllegalArgumentException("Unknown protocol IPv" + version + "!");
		}
	}

	public Packet write(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			buffer.writeByte(data[i]);
		}
		return this;
	}

	public Packet pad(int length) {
		for (int i = 0; i < length; i++) {
			buffer.writeByte(0x00);
		}
		return this;
	}

	public Packet writeByte(int b) {
		buffer.writeByte((byte) b);
		return this;
	}

	public Packet writeUByte(int b) {
		buffer.writeByte(((byte) b) & 0xFF);
		return this;
	}

	public Packet writeBoolean(boolean b) {
		buffer.writeByte(b ? 0x01 : 0x00);
		return this;
	}

	public Packet writeShort(int s) {
		buffer.writeShort(s);
		return this;
	}

	public Packet writeShortLE(int s) {
		buffer.writeShortLE(s);
		return this;
	}

	public Packet writeUShort(int s) {
		buffer.writeShort(((short) s) & 0xFFFF);
		return this;
	}

	public Packet writeUShortLE(int s) {
		buffer.writeShortLE(((short) s) & 0xFFFF);
		return this;
	}

	public Packet writeTriadLE(int t) {
		buffer.writeByte((byte) (t & 0xFF));
		buffer.writeByte((byte) ((t >> 8) & 0xFF));
		buffer.writeByte((byte) ((t >> 16) & 0xFF));
		return this;
	}

	public Packet writeInt(int i) {
		buffer.writeInt(i);
		return this;
	}

	public Packet writeUInt(long i) {
		buffer.writeIntLE(((int) i) & 0xFFFFFFFF);
		return this;
	}

	public Packet writeIntLE(int i) {
		buffer.writeIntLE(i);
		return this;
	}

	public Packet writeUIntLE(long i) {
		buffer.writeIntLE(((int) i) & 0xFFFFFFFF);
		return this;
	}

	public Packet writeLong(long l) {
		buffer.writeLong(l);
		return this;
	}

	public Packet writeLongLE(long l) {
		buffer.writeLongLE(l);
		return this;
	}

	public Packet writeFloat(double f) {
		buffer.writeFloat((float) f);
		return this;
	}

	public Packet writeDouble(double d) {
		buffer.writeDouble(d);
		return this;
	}

	public Packet writeMagic() {
		this.write(MAGIC);
		return this;
	}

	public Packet writeString(String s) {
		byte[] data = s.getBytes();
		this.writeUShort(data.length);
		this.write(data);
		return this;
	}

	public Packet writeStringLE(String s) {
		byte[] data = s.getBytes();
		this.writeUShortLE(data.length);
		this.write(data);
		return this;
	}

	public void writeAddress(InetSocketAddress address) {
		byte[] addressBytes = address.getAddress().getAddress();
		if (addressBytes.length == ADDRESS_VERSION) {
			this.writeUByte(ADDRESS_VERSION);
			for (int i = 0; i < ADDRESS_VERSION; i++) {
				this.writeByte(~addressBytes[i] & 0xFF);
			}
			this.writeUShort(address.getPort());
		} else {
			throw new IllegalArgumentException("Unknown protocol IPv" + addressBytes.length + "!");
		}
	}

	public void writeAddress(String address, int port) {
		this.writeAddress(new InetSocketAddress(address, port));
	}

	public byte[] array() {
		return Arrays.copyOfRange(buffer.array(), 0, buffer.writerIndex());
	}

	public int size() {
		return array().length;
	}

	public ByteBuf buffer() {
		return this.buffer.retain();
	}

	public int remaining() {
		return buffer.readableBytes();
	}

}
