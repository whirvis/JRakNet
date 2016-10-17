package net.marfgamer.raknet.server;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.acknowledge.Record;
import net.marfgamer.raknet.session.RakNetClientSession;

public interface RakNetServerListener {

	public default void handlePing(ServerPing ping) {
	}

	public default void serverShutdown() {
	}

	public default void clientPreConnected(InetSocketAddress address) {
	}

	public default void clientConnected(RakNetClientSession session) {
	}

	public default void clientDisconnected(RakNetClientSession session, String reason) {
	}

	public default void onAcknowledge(RakNetClientSession session, Record record, Reliability reliability, int channel,
			RakNetPacket packet) {
	}

	public default void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
	}

}
