package net.marfgamer.raknet.protocol.unconnected;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class UnconnectedNoFreeIncomingConnections extends RakNetPacket {

	public UnconnectedNoFreeIncomingConnections() {
		super(MessageIdentifier.ID_NO_FREE_INCOMING_CONNECTIONS);
	}
	
}
