package net.marfgamer.raknet.client;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.RakNetPacket;

/**
 * This class is instantiated by the client with the sole purpose of sending
 * received packets to the client so they can be properly handled
 *
 * @author MarfGamer
 */
public class RakNetClientHandler extends ChannelInboundHandlerAdapter {

	private final RakNetClient client;
	private InetSocketAddress causeAddress;

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

			// If an exception happens it's because of this address
			this.causeAddress = sender;

			// Handle the packet and release the buffer
			client.handleMessage(packet, sender);
			datagram.content().release();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		client.handleHandlerException(this.causeAddress, cause);
	}

}
