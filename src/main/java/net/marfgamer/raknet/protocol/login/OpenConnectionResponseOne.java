package net.marfgamer.raknet.protocol.login;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class OpenConnectionResponseOne extends RakNetPacket {

	public static final byte USE_SECURITY_BIT = 0x01;

	public boolean magic;
	public long serverGuid;
	public int maximumTransferUnit;

	/*
	 * JRakNet does not support RakNet's built in security function, it is
	 * poorly documented!
	 */
	public final boolean useSecurity = false;

	public OpenConnectionResponseOne(Packet packet) {
		super(packet);
	}

	public OpenConnectionResponseOne() {
		super(MessageIdentifier.ID_OPEN_CONNECTION_REPLY_1);
	}

	@Override
	public void encode() {
		this.writeMagic();
		this.writeLong(serverGuid);

		// Set security flags
		byte securityFlags = 0x00;
		securityFlags |= (useSecurity ? USE_SECURITY_BIT : 0x00);
		this.writeUByte(securityFlags);
		if (useSecurity) { // We would use == true but Eclipse throws warning
			throw new RuntimeException("Security is not yet supported!");
		}

		this.writeUShort(maximumTransferUnit);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.serverGuid = this.readLong();

		byte securityFlags = 0x00;
		securityFlags |= this.readUByte(); // Use security
		if ((securityFlags & USE_SECURITY_BIT) == USE_SECURITY_BIT) {
			throw new RuntimeException("Security is not yet supported!");
		}

		this.maximumTransferUnit = this.readUShort();
	}

}
