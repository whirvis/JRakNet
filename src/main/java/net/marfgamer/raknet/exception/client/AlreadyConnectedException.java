package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

/**
 * This exception is thrown whenever the client is attempting to connect to a
 * server it is already connected to
 *
 * @author MarfGamer
 */
public class AlreadyConnectedException extends RakNetClientException {

	private static final long serialVersionUID = -482118372058339060L;

	public AlreadyConnectedException(RakNetClient client) {
		super(client, "We are already connected to the server!");
	}

}
