package net.marfgamer.raknet.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.RakNetPacket;

/**
 * This class is instantiated by the server with the sole purpose of sending
 * received packets to the server so they can be properly handled
 *
 * @author MarfGamer
 */
public class RakNetServerHandler extends ChannelInboundHandlerAdapter {

	private final RakNetServer server;
	private final HashMap<InetAddress, BlockedClient> blocked;

	public RakNetServerHandler(RakNetServer server) {
		this.server = server;
		this.blocked = new HashMap<InetAddress, BlockedClient>();
	}

	/**
	 * Blocks the specified address for the specified amount of time
	 * 
	 * @param address
	 *            - The address to block
	 * @param time
	 *            - How long the address will be blocked in milliseconds
	 */
	public void blockAddress(InetAddress address, long time) {
		blocked.put(address, new BlockedClient(System.currentTimeMillis(), time));
		server.getListener().onAddressBlocked(address, time);
	}

	/**
	 * Unblocks the specified address
	 * 
	 * @param address
	 *            - The address to unblock
	 */
	public void unblockAddress(InetAddress address) {
		blocked.remove(address);
		server.getListener().onAddressUnblocked(address);
	}

	/**
	 * Returns whether or not the specified address is blocked
	 * 
	 * @param address
	 *            - The address to check
	 * @return Whether or not the specified address is blocked
	 */
	public boolean addressBlocked(InetAddress address) {
		return blocked.containsKey(address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof DatagramPacket) {
			// Get packet and sender data
			DatagramPacket datagram = (DatagramPacket) msg;
			InetSocketAddress sender = datagram.sender();
			RakNetPacket packet = new RakNetPacket(datagram);

			// Is the sender blocked?
			if (this.addressBlocked(sender.getAddress())) {
				BlockedClient status = blocked.get(sender.getAddress());
				if (status.getTime() == -1) {
					return; // Permanently blocked
				}
				if (System.currentTimeMillis() - status.getStartTime() < status.getTime()) {
					return; // Time hasn't expired
				}
				this.unblockAddress(sender.getAddress());
			}

			server.handleMessage(packet, sender);
			datagram.content().release(); // No longer needed
		}
	}

}