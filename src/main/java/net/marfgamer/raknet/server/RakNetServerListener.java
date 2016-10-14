package net.marfgamer.raknet.server;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.acknowledge.Record;
import net.marfgamer.raknet.session.RakNetClientSession;
import net.marfgamer.raknet.session.RakNetSession;

public abstract class RakNetServerListener {
	
	public void handlePing(ServerPing ping) {
	}
	
	public void serverShutdown() {
	}
	
	public void clientPreConnection(InetSocketAddress address) {
	}
	
	public void clientConnected(RakNetSession session) {
	}
	
	public void clientDisconnected(RakNetSession session, String reason) {
	}
	
	public void onAcknowledge(RakNetClientSession session, Record record, Reliability reliability, int channel, Packet packet) {
	}
	
	public void handlePacket(RakNetClientSession session, Packet packet, int channel) {
	}
	
}
