package net.marfgamer.raknet.exception.client;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

/**
 * This exception is thrown whenever the server the client is attempting to is
 * offline
 *
 * @author MarfGamer
 */
public class ServerOfflineException extends RakNetClientException {

	private static final long serialVersionUID = -3916155995964791602L;

	private final InetSocketAddress address;

	public ServerOfflineException(RakNetClient client, InetSocketAddress address) {
		super(client, "Server at address " + address.toString() + " is offline!");
		this.address = address;
	}

	/**
	 * Returns the address of the server that is offline
	 * 
	 * @return The address of the server that is offline
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

}
