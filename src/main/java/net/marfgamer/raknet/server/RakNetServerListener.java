package net.marfgamer.raknet.server;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.session.RakNetSession;

public abstract class RakNetServerListener {
	
	public void handlePing(ServerPing ping, RakNetServer server) {
	}
	
	public void handlePacket(Packet packet, RakNetSession session, RakNetServer server) {
	}
	
	public void clientConnected(RakNetSession session, RakNetServer server) {
	}
	
	public void clientDisconnected(RakNetSession session, String reason, RakNetServer server) {
	}
	
	public void serverShutdown(RakNetServer server) {
	}
	
}
