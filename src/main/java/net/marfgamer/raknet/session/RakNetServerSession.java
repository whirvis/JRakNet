package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.protocol.MessageIdentifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.login.NewIncomingConnection;
import net.marfgamer.raknet.protocol.login.ConnectionRequestAccepted;
import net.marfgamer.raknet.protocol.message.acknowledge.Record;

/**
 * This class represents a server connection and handles the login sequence
 * packets
 *
 * @author MarfGamer
 */
public class RakNetServerSession extends RakNetSession {

	private final RakNetClient client;

	public RakNetServerSession(RakNetClient client, long guid, int maximumTransferUnit, Channel channel,
			InetSocketAddress address) {
		super(guid, maximumTransferUnit, channel, address);
		this.client = client;
	}

	@Override
	public void onAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet) {

	}

	@Override
	public void handlePacket(RakNetPacket packet, int channel) {
		short packetId = packet.getId();
		if (packetId == MessageIdentifier.ID_CONNECTION_REQUEST_ACCEPTED) {
			ConnectionRequestAccepted serverHandshake = new ConnectionRequestAccepted(packet);
			serverHandshake.decode();

			NewIncomingConnection clientHandshake = new NewIncomingConnection();
			clientHandshake.clientTimestamp = serverHandshake.clientTimestamp;
			clientHandshake.serverTimestamp = serverHandshake.serverTimestamp;
			clientHandshake.serverAddress = client.getSession().getAddress();
			clientHandshake.encode();

			this.sendMessage(Reliability.RELIABLE, clientHandshake);
		} else {
			client.getListener().handlePacket(this, packet, channel);
		}
	}

}
