package net.marfgamer.raknet.protocol.login.error;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class AlreadyConnected extends RakNetPacket {

	public AlreadyConnected() {
		super(MessageIdentifier.ID_ALREADY_CONNECTED);
	}

	public AlreadyConnected(Packet packet) {
		super(packet);
	}

}
