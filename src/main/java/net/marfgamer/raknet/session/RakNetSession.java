package net.marfgamer.raknet.session;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.protocol.Reliability;

public class RakNetSession {
	
	private final InetSocketAddress address;
	private final Channel channel;
	
	public RakNetSession(InetSocketAddress address, Channel channel) {
		this.address = address;
		this.channel = channel;
	}
	
	public InetSocketAddress getSocketAddress() {
		return this.address;
	}
	
	public void sendMessage(Packet packet, Reliability reliability, int channel) {
		
	}
	
	public void sendMessage(Packet packet, Reliability reliability) {
		this.sendMessage(packet, reliability, 0);
	}
	
}
