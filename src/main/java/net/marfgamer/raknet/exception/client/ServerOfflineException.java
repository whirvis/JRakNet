package net.marfgamer.raknet.exception.client;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetClientException;

public class ServerOfflineException extends RakNetClientException {

	private static final long serialVersionUID = -3916155995964791602L;

	private final InetSocketAddress address;

	public ServerOfflineException(RakNetClient client, InetSocketAddress address) {
		super(client, "Server at address " + address.toString() + " is offline!");
		this.address = address;
	}

	public InetSocketAddress getAddress() {
		return this.address;
	}

}
