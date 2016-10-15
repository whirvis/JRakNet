package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.netty.channel.Channel;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.protocol.MessageIdentifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.acknowledge.Record;
import net.marfgamer.raknet.protocol.client.ConnectedConnectionRequest;
import net.marfgamer.raknet.protocol.client.ConnectedServerHandshake;
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
	public void handlePacket(Packet packet, int channel) throws UnknownHostException {
		short id = packet.readUByte();
		if (id == MessageIdentifier.ID_CONNECTION_REQUEST) {
			ConnectedConnectionRequest connectionRequest = new ConnectedConnectionRequest(packet);
			connectionRequest.decode();
			this.setGUID(connectionRequest.clientGuid);

			ConnectedServerHandshake serverHandshake = new ConnectedServerHandshake();
			serverHandshake.clientAddress = this.getAddress();
			serverHandshake.clientTimestamp = connectionRequest.timestamp;
			serverHandshake.serverTimestamp = server.getTimestamp();
			serverHandshake.encode();
			this.sendPacket(RELIABLE_ORDERED, serverHandshake);
			this.setState(RakNetState.HANDSHAKING);
		} else if (id == MessageIdentifier.ID_NEW_INCOMING_CONNECTION) {
			// TODO: Handle this packet
			this.setState(RakNetState.CONNECTED);
			server.getListener().clientConnected(this);
		} else if (id == MessageIdentifier.ID_DISCONNECTION_NOTIFICATION) {
			server.removeSession(this, "Client disconnected");
		} else if (id >= MessageIdentifier.ID_USER_PACKET_ENUM) {
			/*
			 * We already read the ID so we need to artificially re-add the ID
			 * uBtye back the Packet
			 */
			Packet rewrap = new Packet();
			rewrap.writeUByte(id);
			rewrap.write(packet.array());

			server.getListener().handlePacket(this, rewrap, channel);
		}
		// TODO: Ping and Pong
	}

}
