package net.marfgamer.raknet.protocol.unconnected;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class UnconnectedOpenConnectionRequestTwo extends RakNetPacket {
	
	public boolean magic;
	public InetSocketAddress address;
	public int maximumTransferUnit;
	public long clientGuid;

	public UnconnectedOpenConnectionRequestTwo(Packet packet) {
		super(packet);
	}

	public UnconnectedOpenConnectionRequestTwo() {
		super(MessageIdentifier.ID_OPEN_CONNECTION_REQUEST_2);
	}

	@Override
	public void encode() {
		this.writeMagic();
	}

	@Override
	public void decode() throws UnknownHostException {
		this.magic = this.checkMagic();
		this.address = this.readAddress();
		this.maximumTransferUnit = this.readUShort();
		this.clientGuid = this.readLong();
	}
	

}
