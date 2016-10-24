package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.login.ConnectionRequest;
import net.marfgamer.raknet.protocol.login.ConnectionRequestAccepted;
import net.marfgamer.raknet.protocol.login.NewIncomingConnection;
import net.marfgamer.raknet.protocol.login.error.ConnectionAttemptFailed;
import net.marfgamer.raknet.protocol.message.acknowledge.Record;
import net.marfgamer.raknet.protocol.session.DisconnectionNotification;
import net.marfgamer.raknet.server.RakNetServer;

/**
 * This class represents a client connection and handles the login sequence
 * packets
 *
 * @author MarfGamer
 */
public class RakNetClientSession extends RakNetSession {

	private final RakNetServer server;
	private final long timeCreated;
	private long timestamp;

	public RakNetClientSession(RakNetServer server, long timeCreated, long guid, int maximumTransferUnit,
			Channel channel, InetSocketAddress address) {
		super(guid, maximumTransferUnit, channel, address);
		this.server = server;
		this.timeCreated = timeCreated;
		// The timestamp is determined during login
	}

	/**
	 * Returns the server this session is connected to
	 * 
	 * @return The server this session is connected to
	 */
	public RakNetServer getServer() {
		return this.server;
	}

	/**
	 * Returns the time this session was created
	 * 
	 * @return The time this session was created
	 */
	public long getTimeCreated() {
		return this.timeCreated;
	}

	/**
	 * Returns the client's timestamp<br>
	 * Note: This is not determined during creation but rather during login
	 * 
	 * @return The client's timestamp
	 */
	public long getTimestamp() {
		return (System.currentTimeMillis() - this.timestamp);
	}

	@Override
	public void onAcknowledge(Record record, Reliability reliability, int channel, RakNetPacket packet) {
		server.getListener().onAcknowledge(this, record, reliability, channel, packet);
	}

	@Override
	public void handlePacket(RakNetPacket packet, int channel) {
		short id = packet.getId();
		System.out.println(id);
		if (id == MessageIdentifier.ID_CONNECTION_REQUEST && this.getState() == RakNetState.DISCONNECTED) {
			ConnectionRequest request = new ConnectionRequest(packet);
			request.decode();

			if (request.clientGuid == this.getGloballyUniqueId()) {
				ConnectionRequestAccepted requestAccepted = new ConnectionRequestAccepted();
				requestAccepted.clientAddress = this.getAddress();
				requestAccepted.clientTimestamp = request.timestamp;
				requestAccepted.serverTimestamp = server.getTimestamp();
				requestAccepted.encode();

				this.timestamp = (System.currentTimeMillis() - request.timestamp);
				this.sendMessage(Reliability.RELIABLE_ORDERED, requestAccepted);
				this.setState(RakNetState.HANDSHAKING);
			} else {
				ConnectionAttemptFailed attemptFailed = new ConnectionAttemptFailed();
				attemptFailed.encode();

				this.sendMessage(Reliability.RELIABLE_ORDERED, attemptFailed);
				this.setState(RakNetState.DISCONNECTED);
				server.removeSession(this, "Connection failed, invalid GUID");
			}
		} else if (id == MessageIdentifier.ID_NEW_INCOMING_CONNECTION && this.getState() == RakNetState.HANDSHAKING) {
			NewIncomingConnection clientHandshake = new NewIncomingConnection(packet);
			clientHandshake.decode();

			if (clientHandshake.serverTimestamp == server.getTimestamp()) {
				this.timestamp = (System.currentTimeMillis() - clientHandshake.clientTimestamp);

				this.setState(RakNetState.CONNECTED);
				server.getListener().onClientConnect(this);
			} else {
				ConnectionAttemptFailed attemptFailed = new ConnectionAttemptFailed();
				attemptFailed.encode();

				this.sendMessage(Reliability.RELIABLE_ORDERED, attemptFailed);
				this.setState(RakNetState.DISCONNECTED);
				server.removeSession(this, "Connection failed, invalid timestamp");
			}
		} else if (id == MessageIdentifier.ID_DISCONNECTION_NOTIFICATION) {
			DisconnectionNotification disconnectionNotification = new DisconnectionNotification(packet);
			disconnectionNotification.decode();

			server.removeSession(this, "Client disconnected");
		} else if (id >= MessageIdentifier.ID_USER_PACKET_ENUM) {
			server.getListener().handlePacket(this, packet, channel);
		}
	}

}
