package net.marfgamer.raknet.exception;

import net.marfgamer.raknet.client.RakNetClient;

public class RakNetClientException extends RakNetException {

	private static final long serialVersionUID = 2441122006497992080L;

	private final RakNetClient client;

	public RakNetClientException(RakNetClient client, String error) {
		super(error);
		this.client = client;
	}

	public final RakNetClient getClient() {
		return this.client;
	}

}
