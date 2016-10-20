package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

public class NoFreeIncomingConnectionsException extends RakNetClientException {

	private static final long serialVersionUID = 5863972657532782029L;

	public NoFreeIncomingConnectionsException(RakNetClient client) {
		super(client, "The server has no free incoming connections!");
	}

}
