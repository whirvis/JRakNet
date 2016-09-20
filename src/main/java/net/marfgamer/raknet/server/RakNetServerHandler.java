package net.marfgamer.raknet.server;

import java.net.InetSocketAddress;
import java.util.HashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.Packet;

public class RakNetServerHandler extends ChannelInboundHandlerAdapter {

	private final RakNetServer server;
	private final HashMap<InetSocketAddress, BlockedClient> blocked;

	public RakNetServerHandler(RakNetServer server) {
		this.server = server;
		this.blocked = new HashMap<InetSocketAddress, BlockedClient>();
	}

	public void blockAddress(InetSocketAddress address, long time) {
		blocked.put(address, new BlockedClient(System.currentTimeMillis(), time));
	}

	public void unblockAddress(InetSocketAddress address) {
		blocked.remove(address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof DatagramPacket) {
			// Get packet and sender data
			DatagramPacket datagram = (DatagramPacket) msg;
			InetSocketAddress sender = datagram.sender();
			Packet packet = new Packet(datagram);

			// Is the sender blocked?
			if (blocked.containsKey(sender)) {
				BlockedClient status = blocked.get(sender);
				if(status.getTime() == -1) {
					return; // Permanently blocked
				}
				if (System.currentTimeMillis() - status.getStartTime() < status.getTime()) {
					return; // Time hasn't expired
				}
				blocked.remove(sender);
			}
			
			server.handleMessage(packet, sender);
		} else {
			System.err.println("Got " + msg.getClass().getName());
		}
	}

}