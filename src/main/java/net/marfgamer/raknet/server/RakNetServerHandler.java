/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 MarfGamer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 */
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