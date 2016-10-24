package net.marfgamer.raknet.client;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.identifier.Identifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.message.acknowledge.Record;
import net.marfgamer.raknet.session.RakNetServerSession;

/**
 * This interface is used by the client to let the user know when specific
 * events are triggered
 *
 * @author MarfGamer
 */
public interface RakNetClientListener {

	/**
	 * Called when a server is discovered on the local network
	 * 
	 * @param address
	 *            - The address of the server
	 * @param identifier
	 *            - The identifier of the server
	 */
	public default void onServerDiscovered(InetSocketAddress address, Identifier identifier) {
	}

	/**
	 * Called when the identifier of an already discovered server changes
	 * 
	 * @param address
	 *            - The address of the server
	 * @param identifier
	 *            - The new identifier
	 */
	public default void onServerIdentifierUpdate(InetSocketAddress address, Identifier identifier) {
	}

	/**
	 * Called when a previously discovered server has been forgotten by the
	 * client
	 * 
	 * @param address
	 *            - The address of the server
	 */
	public default void onServerForgotten(InetSocketAddress address) {
	}

	/**
	 * Called when the client fails to connect to a server
	 * 
	 * @param address
	 *            - The address of the server
	 * @param cause
	 *            - The cause for failure
	 */
	public default void onConnectionFailure(InetSocketAddress address, RakNetException cause) {
	}

	/**
	 * Called when the client successfully connects to a server
	 * 
	 * @param session
	 *            - The session assigned to the server
	 */
	public default void onConnect(RakNetServerSession session) {
	}

	/**
	 * Called when the client disconnects from the server
	 * 
	 * @param session
	 *            - The server the client disconnected from
	 * @param reason
	 *            - The reason for disconnection
	 */
	public default void onDisconnect(RakNetServerSession session, String reason) {
	}

	/**
	 * Called when a message sent with _REQUIRES_ACK_RECEIPT is acknowledged by
	 * the server
	 * 
	 * @param session
	 *            - The server that acknowledged the packet
	 * @param record
	 *            - The record of the acknowledged packet
	 * @param reliability
	 *            - The reliability of the acknowledged packet
	 * @param channel
	 *            - The channel of the acknowledged packet
	 * @param packet
	 *            - The acknowledged packet
	 */
	public default void onAcknowledge(RakNetServerSession session, Record record, Reliability reliability, int channel,
			RakNetPacket packet) {
	}

	/**
	 * Called when a packet has been received from the server and is ready to be
	 * handled
	 * 
	 * @param session
	 *            - The server that sent the packet
	 * @param packet
	 *            - The packet received from the server
	 * @param channel
	 *            - The channel the packet was sent on
	 */
	public default void handlePacket(RakNetServerSession session, RakNetPacket packet, int channel) {
	}

}
