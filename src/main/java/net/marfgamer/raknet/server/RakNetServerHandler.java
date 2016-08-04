package net.marfgamer.raknet.server;

import java.io.IOException;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.protocol.Message;

public class RakNetServerHandler extends ChannelInboundHandlerAdapter {

	private final RakNetServer server;

	public RakNetServerHandler(RakNetServer server) {
		this.server = server;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof DatagramPacket) {
			DatagramPacket datagram = (DatagramPacket) msg;
			Message packet = new Message(Unpooled.copiedBuffer(datagram.content()));
			server.handleRaw(packet, datagram.sender());
		} else {
			throw new IOException(
					"Received " + msg.getClass().getName() + " instead of " + DatagramPacket.class.getName());
		}
	}

	public void channelReadComplete() {

	}

}
