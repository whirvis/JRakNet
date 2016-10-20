package net.marfgamer.raknet.protocol.login.error;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class ConnectionAttemptFailed extends RakNetPacket {

	public ConnectionAttemptFailed() {
		super(MessageIdentifier.ID_CONNECTION_ATTEMPT_FAILED);
	}

	public ConnectionAttemptFailed(Packet packet) {
		super(packet);
	}
	
}
