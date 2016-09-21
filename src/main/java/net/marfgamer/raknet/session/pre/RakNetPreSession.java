package net.marfgamer.raknet.session.pre;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.Packet;

public class RakNetPreSession {

	private final InetSocketAddress address;
	private final Channel channel;
	private PreSessionStatus status;

	public RakNetPreSession(InetSocketAddress address, Channel channel) {
		this.address = address;
		this.channel = channel;
	}

	public PreSessionStatus getStatus() {
		return this.status;
	}

	public void setStatus(PreSessionStatus status) {
		this.status = status;
	}

	public void sendPacket(Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

}
