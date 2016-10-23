package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

/**
 * This exception is thrown whenever the server the client is attempting to
 * connect to is full
 *
 * @author MarfGamer
 */
public class NoFreeIncomingConnectionsException extends RakNetClientException {

	private static final long serialVersionUID = 5863972657532782029L;

	public NoFreeIncomingConnectionsException(RakNetClient client) {
		super(client, "The server has no free incoming connections!");
	}

}
