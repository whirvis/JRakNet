package net.marfgamer.raknet.protocol.login;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class OpenConnectionResponseTwo extends RakNetPacket {

	public boolean magic;
	public long serverGuid;
	public InetSocketAddress clientAddress;
	public int maximumTransferUnit;
	public boolean encryptionEnabled;

	public OpenConnectionResponseTwo(Packet packet) {
		super(packet);
	}

	public OpenConnectionResponseTwo() {
		super(MessageIdentifier.ID_OPEN_CONNECTION_REPLY_2);
	}

	@Override
	public void encode() {
		this.writeMagic();
		this.writeLong(serverGuid);
		this.writeAddress(clientAddress);
		this.writeUShort(maximumTransferUnit);
		this.writeBoolean(encryptionEnabled);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.serverGuid = this.readLong();
		this.clientAddress = this.readAddress();
		this.maximumTransferUnit = this.readUShort();
		this.encryptionEnabled = this.readBoolean();
	}

}
