package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

/**
 * This exception is thrown when the client is banned from the server it is
 * attempting to connect to
 *
 * @author MarfGamer
 */
public class ConnectionBannedException extends RakNetClientException {

	private static final long serialVersionUID = 8440218445920818619L;

	public ConnectionBannedException(RakNetClient client) {
		super(client, "Client is banned from server!");
	}

}
