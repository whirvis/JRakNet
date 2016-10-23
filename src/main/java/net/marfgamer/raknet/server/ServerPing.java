package net.marfgamer.raknet.server;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.identifier.Identifier;

public class ServerPing {

	private final InetSocketAddress sender;
	private Identifier identifier;

	public ServerPing(InetSocketAddress sender, Identifier identifier) {
		this.sender = sender;
		this.identifier = identifier;
	}

	public InetSocketAddress getSender() {
		return this.sender;
	}

	public Identifier getIdentifier() {
		return this.identifier;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

}
