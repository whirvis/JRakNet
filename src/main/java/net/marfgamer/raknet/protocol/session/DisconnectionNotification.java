package net.marfgamer.raknet.protocol.session;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class DisconnectionNotification extends RakNetPacket {
	
	public DisconnectionNotification() {
		super(MessageIdentifier.ID_DISCONNECTION_NOTIFICATION);
	}
	
	public DisconnectionNotification(Packet packet) {
		super(packet);
	}

}
