package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

public class ConnectionBannedException extends RakNetClientException {

	private static final long serialVersionUID = 8440218445920818619L;

	public ConnectionBannedException(RakNetClient client) {
		super(client, "We are banned from server!");
	}

}
