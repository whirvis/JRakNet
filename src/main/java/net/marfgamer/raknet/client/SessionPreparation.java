package net.marfgamer.raknet.client;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.exception.client.AlreadyConnectedException;
import net.marfgamer.raknet.exception.client.ConnectionBannedException;
import net.marfgamer.raknet.exception.client.IncompatibleProtocolException;
import net.marfgamer.raknet.exception.client.NoFreeIncomingConnectionsException;
import net.marfgamer.raknet.protocol.login.OpenConnectionResponseOne;
import net.marfgamer.raknet.protocol.login.OpenConnectionResponseTwo;
import net.marfgamer.raknet.protocol.login.error.AlreadyConnected;
import net.marfgamer.raknet.protocol.login.error.ConnectionBanned;
import net.marfgamer.raknet.protocol.login.error.IncompatibleProtocol;
import net.marfgamer.raknet.protocol.login.error.NoFreeIncomingConnections;
import net.marfgamer.raknet.session.RakNetServerSession;

/**
 * This class is used to easily store data during login and create the session
 * when the client is connected
 *
 * @author MarfGamer
 */
public class SessionPreparation {

	// Preparation data
	private final RakNetClient client;
	public boolean cancelled;
	public RakNetException cancelReason;

	// Server data
	public long guid = -1;
	public int maximumTransferUnit = -1;
	public InetSocketAddress address = null;
	public boolean loginPackets[] = new boolean[2];

	public SessionPreparation(RakNetClient client) {
		this.client = client;
	}

	/**
	 * Handles the specified packet and automatically updates the preparation
	 * data
	 * 
	 * @param packet
	 *            - The packet to handle
	 */
	public void handlePacket(RakNetPacket packet) {
		short packetId = packet.getId();
		if (packetId == ID_OPEN_CONNECTION_REPLY_1) {
			OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne(packet);
			connectionResponseOne.decode();

			if (connectionResponseOne.magic == true && connectionResponseOne.useSecurity == false
					&& connectionResponseOne.maximumTransferUnit > RakNet.MINIMUM_TRANSFER_UNIT
					&& connectionResponseOne.maximumTransferUnit < RakNetClient.PHYSICAL_MAXIMUM_TRANSFER_UNIT) {
				this.maximumTransferUnit = connectionResponseOne.maximumTransferUnit;
				this.guid = connectionResponseOne.serverGuid;
				this.loginPackets[0] = true;
			} else {
				this.cancelled = true;
			}
		} else if (packetId == ID_OPEN_CONNECTION_REPLY_2) {
			OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo(packet);
			connectionResponseTwo.decode();

			if (connectionResponseTwo.magic == true && connectionResponseTwo.encryptionEnabled == false
					&& connectionResponseTwo.serverGuid == this.guid
					&& connectionResponseTwo.maximumTransferUnit == this.maximumTransferUnit) {
				this.loginPackets[1] = true;
			} else {
				this.cancelled = true;
			}
		} else if (packetId == ID_ALREADY_CONNECTED) {
			AlreadyConnected alreadyConnected = new AlreadyConnected(packet);
			alreadyConnected.decode();

			this.cancelReason = new AlreadyConnectedException(client);
			this.cancelled = true;
		} else if (packetId == ID_NO_FREE_INCOMING_CONNECTIONS) {
			NoFreeIncomingConnections noFreeIncomingConnections = new NoFreeIncomingConnections(packet);
			noFreeIncomingConnections.decode();

			this.cancelReason = new NoFreeIncomingConnectionsException(client);
			this.cancelled = true;
		} else if (packetId == ID_CONNECTION_BANNED) {
			ConnectionBanned connectionBanned = new ConnectionBanned(packet);
			connectionBanned.decode();

			this.cancelReason = new ConnectionBannedException(client);
			this.cancelled = true;
		} else if (packetId == ID_INCOMPATIBLE_PROTOCOL_VERSION) {
			IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol(packet);
			incompatibleProtocol.decode();

			if (incompatibleProtocol.serverGuid == this.guid) {
				this.cancelReason = new IncompatibleProtocolException(client, RakNet.CLIENT_NETWORK_PROTOCOL,
						incompatibleProtocol.networkProtocol);
				this.cancelled = true;
			}
		}
	}

	/**
	 * Returns whether or not the session has enough data to be created to be
	 * used by the client
	 * 
	 * @return Whether or not the session has enough data to be created
	 */
	public boolean readyForSession() {
		// It was cancelled, why are we finishing?
		if (cancelled == true) {
			return false;
		}

		// Not all of the data has been set
		if (this.guid == -1 || this.maximumTransferUnit == -1 || this.address == null) {
			return false;
		}

		// Not all of the packets needed to connect have been handled
		for (boolean handled : loginPackets) {
			if (handled == false) {
				return false;
			}
		}

		// Nothing returned false, everything is ready!
		return true;
	}

	/**
	 * Creates the session with the data set during login
	 * 
	 * @param channel
	 *            - The channel the session will send data through
	 * @return RakNetServerSession
	 */
	public RakNetServerSession createSession(Channel channel) {
		return new RakNetServerSession(client, guid, maximumTransferUnit, channel, address);
	}

}
