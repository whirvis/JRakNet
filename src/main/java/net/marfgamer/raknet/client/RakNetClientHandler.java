package net.marfgamer.raknet.client;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.RakNetPacket;

public class RakNetClientHandler extends ChannelInboundHandlerAdapter {

	private final RakNetClient client;

	public RakNetClientHandler(RakNetClient client) {
		this.client = client;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof DatagramPacket) {
			// Get packet and sender data
			DatagramPacket datagram = (DatagramPacket) msg;
			InetSocketAddress sender = datagram.sender();
			RakNetPacket packet = new RakNetPacket(datagram);
			
			client.handleMessage(packet, sender);
			datagram.content().release(); // No longer needed
		} else {
			System.err.println("Got " + msg.getClass().getName());
		}
	}

}
