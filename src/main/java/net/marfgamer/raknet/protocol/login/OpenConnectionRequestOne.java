package net.marfgamer.raknet.protocol.login;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class OpenConnectionRequestOne extends RakNetPacket {

	public static final int MTU_PADDING = 18; // 1 for ID, 1 for protocol
												// version, 16 for magic

	public boolean magic;
	public int protocolVersion;
	public int maximumTransferUnit;

	public OpenConnectionRequestOne(Packet packet) {
		super(packet);
	}

	public OpenConnectionRequestOne() {
		super(MessageIdentifier.ID_OPEN_CONNECTION_REQUEST_1);
	}

	@Override
	public void encode() {
		this.writeMagic();
		this.writeUByte(protocolVersion);
		this.pad(maximumTransferUnit - MTU_PADDING);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.protocolVersion = this.readUByte();
		this.maximumTransferUnit = (this.remaining() + MTU_PADDING);
		this.read(this.remaining()); // Go ahead and get rid of those bytes
	}

}
