package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

public class AlreadyConnectedException extends RakNetClientException {

	private static final long serialVersionUID = -482118372058339060L;

	public AlreadyConnectedException(RakNetClient client) {
		super(client, "We are already connected to the server!");
	}

}
