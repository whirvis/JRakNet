package net.marfgamer.raknet.event.server;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.event.Event;

public class ServerPingEvent extends Event {
	
	private String identifier;
	private InetSocketAddress sender;
	
	public ServerPingEvent(String identifier, InetSocketAddress sender) {
		this.identifier = identifier;
		this.sender = sender;
	}
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public InetSocketAddress getSender() {
		return this.sender;
	}
	
}
