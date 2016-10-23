package net.marfgamer.raknet.protocol.login.error;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class NoFreeIncomingConnections extends RakNetPacket {

	public NoFreeIncomingConnections() {
		super(MessageIdentifier.ID_NO_FREE_INCOMING_CONNECTIONS);
	}

	public NoFreeIncomingConnections(Packet packet) {
		super(packet);
	}

}
