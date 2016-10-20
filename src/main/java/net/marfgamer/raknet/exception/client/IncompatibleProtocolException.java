package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

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

	public int getClientProtocol() {
		return this.clientProtocol;
	}

	public int getServerProtocol() {
		return this.serverProtocol;
	}

}
