package net.marfgamer.raknet.client;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.message.acknowledge.Record;
import net.marfgamer.raknet.session.RakNetServerSession;

public interface RakNetClientListener {

	public default void onConnectionFailure(InetSocketAddress address, RakNetException cause) {
	}

	public default void onPreConnect(InetSocketAddress address) {
	}

	public default void onConnect(RakNetServerSession session) {
	}

	public default void onDisconnect(RakNetServerSession session, String reason) {
	}

	public default void onAcknowledge(RakNetServerSession session, Record record, Reliability reliability, int channel,
			RakNetPacket packet) {
	}

	public default void handlePacket(RakNetServerSession session, RakNetPacket packet, int channel) {
	}

}
