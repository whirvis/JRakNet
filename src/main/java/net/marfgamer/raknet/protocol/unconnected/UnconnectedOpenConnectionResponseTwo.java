package net.marfgamer.raknet.protocol.unconnected;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class UnconnectedOpenConnectionResponseTwo extends RakNetPacket {

	public boolean magic;
	public long serverGuid;
	public InetSocketAddress clientAddress;
	public int maximumTransferUnit;
	public boolean encryptionEnabled;

	public UnconnectedOpenConnectionResponseTwo(Packet packet) {
		super(packet);
	}

	public UnconnectedOpenConnectionResponseTwo() {
		super(MessageIdentifier.ID_OPEN_CONNECTION_REPLY_2);
	}

	@Override
	public void encode() throws UnknownHostException {
		this.writeMagic();
		this.writeLong(serverGuid);
		this.writeAddress(clientAddress);
		this.writeUShort(maximumTransferUnit);
		this.writeBoolean(encryptionEnabled);
	}

	@Override
	public void decode() throws UnknownHostException {
		this.magic = this.checkMagic();
		this.serverGuid = this.readLong();
		this.clientAddress = this.readAddress();
		this.maximumTransferUnit = this.readUShort();
		this.encryptionEnabled = this.readBoolean();
	}

}
