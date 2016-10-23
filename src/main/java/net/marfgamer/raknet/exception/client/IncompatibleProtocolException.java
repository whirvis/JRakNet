package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

/**
 * This exception is thrown when the client does not share the same protocol as
 * the server it is attempting to connect to
 *
 * @author MarfGamer
 */
public class IncompatibleProtocolException extends RakNetClientException {

	private static final long serialVersionUID = -3390229698349252537L;

	private final int clientProtocol;
	private final int serverProtocol;

	public IncompatibleProtocolException(RakNetClient client, int clientProtocol, int serverProtocol) {
		super(client, (clientProtocol < serverProtocol ? ("Outdated client! I'm still on " + clientProtocol)
				: ("Outdated server! They're still on" + serverProtocol)));
		this.clientProtocol = clientProtocol;
		this.serverProtocol = serverProtocol;
	}

	/**
	 * Returns the protocol the client is on
	 * 
	 * @return The protocol the client is on
	 */
	public int getClientProtocol() {
		return this.clientProtocol;
	}

	/**
	 * Returns the protocol the server is on
	 * 
	 * @return The protocol the server is on
	 */
	public int getServerProtocol() {
		return this.serverProtocol;
	}

}
