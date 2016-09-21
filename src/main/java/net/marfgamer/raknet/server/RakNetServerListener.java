package net.marfgamer.raknet.server;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.session.RakNetSession;
import net.marfgamer.raknet.session.pre.RakNetPreSession;

public abstract class RakNetServerListener {
	
	public void handlePing(ServerPing ping, RakNetServer server) {
	}
	
	public void handlePacket(Packet packet, RakNetSession session, RakNetServer server) {
	}
	
	public void clientPreConnect(RakNetPreSession session, RakNetServer server) {
		
	}
	
	public void clientConnected(RakNetSession session, RakNetServer server) {
	}
	
	public void clientDisconnected(RakNetSession session, String reason, RakNetServer server) {
	}
	
}
