package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.netty.channel.Channel;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.acknowledge.Record;
import net.marfgamer.raknet.protocol.connected.ConnectedClientHandshake;
import net.marfgamer.raknet.protocol.connected.ConnectedConnectionRequest;
import net.marfgamer.raknet.protocol.connected.ConnectedServerHandshake;
import net.marfgamer.raknet.server.RakNetServer;

public class RakNetClientSession extends RakNetSession {

	private final RakNetServer server;
	private long timestamp;

	public RakNetClientSession(RakNetServer server, long guid, int maximumTransferUnit, Channel channel,
			InetSocketAddress address) {
		super(guid, maximumTransferUnit, channel, address);
		this.server = server;
	}

	public RakNetServer getServer() {
		return this.server;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	@Override
	public void onAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet) {
		server.getListener().onAcknowledge(this, record, reliability, channel, packet);
	}

	@Override
	public void handlePacket(RakNetPacket packet, int channel) throws UnknownHostException {
		short id = packet.getId();
		if (id == MessageIdentifier.ID_CONNECTION_REQUEST) {
			ConnectedConnectionRequest connectionRequest = new ConnectedConnectionRequest(packet);
			connectionRequest.decode();

			if (connectionRequest.clientGuid == this.getGUID()) {
				ConnectedServerHandshake serverHandshake = new ConnectedServerHandshake();
				serverHandshake.clientAddress = this.getAddress();
				serverHandshake.clientTimestamp = connectionRequest.timestamp;
				serverHandshake.serverTimestamp = server.getTimestamp();
				serverHandshake.encode();

				this.sendPacket(RELIABLE_ORDERED, serverHandshake);
				this.setState(RakNetState.HANDSHAKING);
			}
		} else if (id == MessageIdentifier.ID_NEW_INCOMING_CONNECTION) {
			ConnectedClientHandshake clientHandshake = new ConnectedClientHandshake(packet);
			clientHandshake.decode();

			if (clientHandshake.serverTimestamp == server.getTimestamp()) {
				this.timestamp = (System.currentTimeMillis() - clientHandshake.clientTimestamp);

				this.setState(RakNetState.CONNECTED);
				server.getListener().clientConnected(this);
			}
		} else if (id == MessageIdentifier.ID_DISCONNECTION_NOTIFICATION) {
			server.removeSession(this, "Client disconnected");
		} else if (id >= MessageIdentifier.ID_USER_PACKET_ENUM) {
			server.getListener().handlePacket(this, packet, channel);
		}
	}

	@Override
	public void onTimeout() throws Exception {
		server.removeSession(this, "Timeout");
	}

}
