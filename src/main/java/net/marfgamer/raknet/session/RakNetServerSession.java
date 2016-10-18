package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.protocol.MessageIdentifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.acknowledge.Record;
import net.marfgamer.raknet.protocol.connected.ConnectedClientHandshake;
import net.marfgamer.raknet.protocol.connected.ConnectedServerHandshake;

public class RakNetServerSession extends RakNetSession {

	private final RakNetClient client;

	public RakNetServerSession(RakNetClient client, long guid, int maximumTransferUnit, Channel channel,
			InetSocketAddress address) {
		super(guid, maximumTransferUnit, channel, address);
		this.client = client;
	}

	@Override
	public void onAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet)
			throws Exception {

	}

	@Override
	public void handlePacket(RakNetPacket packet, int channel) throws Exception {
		short packetId = packet.getId();
		System.out.println(packetId);
		if(packetId == MessageIdentifier.ID_CONNECTION_REQUEST_ACCEPTED) {
			ConnectedServerHandshake serverHandshake = new ConnectedServerHandshake(packet);
			serverHandshake.decode();
			
			ConnectedClientHandshake clientHandshake = new ConnectedClientHandshake();
			clientHandshake.clientTimestamp = serverHandshake.clientTimestamp;
			clientHandshake.serverTimestamp = serverHandshake.serverTimestamp;
			clientHandshake.serverAddress = client.getSession().getAddress();
			clientHandshake.encode();
			
			this.sendPacket(Reliability.RELIABLE, clientHandshake);
			System.out.println("Received 0x10 and sent response");
		}
	}

	@Override
	public void closeConnection(String reason) {

	}

}
