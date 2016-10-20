package net.marfgamer.raknet.protocol.login;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class OpenConnectionRequestTwo extends RakNetPacket {

	public boolean magic;
	public InetSocketAddress address;
	public int maximumTransferUnit;
	public long clientGuid;

	public OpenConnectionRequestTwo(Packet packet) {
		super(packet);
	}

	public OpenConnectionRequestTwo() {
		super(MessageIdentifier.ID_OPEN_CONNECTION_REQUEST_2);
	}

	@Override
	public void encode() {
		this.writeMagic();
		this.writeAddress(address);
		this.writeShort(maximumTransferUnit);
		this.writeLong(clientGuid);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.address = this.readAddress();
		this.maximumTransferUnit = this.readUShort();
		this.clientGuid = this.readLong();
	}

}
