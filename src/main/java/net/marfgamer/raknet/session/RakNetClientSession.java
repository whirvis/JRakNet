package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.acknowledge.Record;
import net.marfgamer.raknet.server.RakNetServer;

public class RakNetClientSession extends RakNetSession {

	private final RakNetServer server;

	public RakNetClientSession(RakNetServer server, long guid, int maximumTransferUnit, Channel channel,
			InetSocketAddress address) {
		super(guid, maximumTransferUnit, channel, address);
		this.server = server;
	}

	public RakNetServer getServer() {
		return this.server;
	}

	@Override
	public void onAcknowledge(Record record, Reliability reliability, int channel, Packet packet) {
		server.getListener().onAcknowledge(this, record, reliability, channel, packet);
	}

	@Override
	public void handlePacket(Packet packet, int channel) {
		server.getListener().handlePacket(this, packet, channel);
	}

}
