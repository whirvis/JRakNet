package net.marfgamer.raknet.protocol.login.error;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectionBanned extends RakNetPacket {
	
	public ConnectionBanned() {
		super(MessageIdentifier.ID_CONNECTION_BANNED);
	}
	
	public ConnectionBanned(Packet packet) {
		super(packet);
	}

}
