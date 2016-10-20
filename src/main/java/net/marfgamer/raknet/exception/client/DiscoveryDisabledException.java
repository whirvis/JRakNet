package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

public class DiscoveryDisabledException extends RakNetClientException {

	private static final long serialVersionUID = 4696187991193732405L;

	public DiscoveryDisabledException(RakNetClient client) {
		super(client, "Discovery has not been enabled!");
	}

}
