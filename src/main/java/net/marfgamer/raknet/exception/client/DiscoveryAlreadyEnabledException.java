package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

public class DiscoveryAlreadyEnabledException extends RakNetClientException {

	private static final long serialVersionUID = -1157130516330971690L;

	public DiscoveryAlreadyEnabledException(RakNetClient client) {
		super(client, "Discovery is already enabled for the client!");
	}

}
