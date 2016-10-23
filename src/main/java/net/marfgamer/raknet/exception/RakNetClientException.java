package net.marfgamer.raknet.exception;

import net.marfgamer.raknet.client.RakNetClient;

/**
 * This class represents an exception specific to the <code>RakNetClient</code>
 *
 * @author MarfGamer
 */
public class RakNetClientException extends RakNetException {

	private static final long serialVersionUID = 2441122006497992080L;

	private final RakNetClient client;

	public RakNetClientException(RakNetClient client, String error) {
		super(error);
		this.client = client;
	}

	/**
	 * Returns the client that caught the exception
	 * 
	 * @return The client that caught the exception
	 */
	public final RakNetClient getClient() {
		return this.client;
	}

}
