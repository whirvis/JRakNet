/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Trent Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
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
package com.whirvis.jraknet.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNetPacket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

/**
 * Used by the <code>RakNetServer</code> with the sole purpose of sending
 * received packets to the server so they can be handled.
 *
 * @author Trent Summerlin
 */
public class RakNetServerHandler extends ChannelInboundHandlerAdapter {

	private static final Logger LOG = LogManager.getLogger(RakNetServerHandler.class);

	private final String loggerName;
	private final RakNetServer server;
	private final ConcurrentHashMap<InetAddress, BlockedAddress> blocked;
	private InetSocketAddress causeAddress;

	/**
	 * Constructs a <code>RakNetClientServer</code> with the
	 * <code>RakNetClient</code>.
	 * 
	 * @param server
	 *            the <code>RakNetServer</code> to send received packets to.
	 */
	public RakNetServerHandler(RakNetServer server) {
		this.loggerName = "server handler #" + server.getGloballyUniqueId();
		this.server = server;
		this.blocked = new ConcurrentHashMap<InetAddress, BlockedAddress>();
	}

	/**
	 * Blocks the address with the reason for the
	 * amount time.
	 * 
	 * @param address
	 *            the address to block.
	 * @param reason
	 *            the reason the address was blocked.
	 * @param time
	 *            how long the address will be blocked in milliseconds.
	 */
	public void blockAddress(InetAddress address, String reason, long time) {
		blocked.put(address, new BlockedAddress(System.currentTimeMillis(), time));
		for (RakNetServerListener listener : server.getListeners()) {
			listener.onAddressBlocked(address, reason, time);
		}
		LOG.info(
				loggerName + "Blocked address " + address + " due to \"" + reason + "\" for " + time + " milliseconds");
	}

	/**
	 * Unblocks the address.
	 * 
	 * @param address
	 *            the address to unblock.
	 */
	public void unblockAddress(InetAddress address) {
		blocked.remove(address);
		for (RakNetServerListener listener : server.getListeners()) {
			listener.onAddressUnblocked(address);
		}
		LOG.info(loggerName + "Unblocked address " + address);
	}

	/**
	 * Returns whether or not the address is blocked.
	 * 
	 * @param address
	 *            the address to check.
	 * @return <code>true</code> if the address is blocked, <code>false</code>
	 *         otherwise.
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

			// If an exception happens it's because of this address
			this.causeAddress = sender;

			// Is the sender blocked?
			if (this.addressBlocked(sender.getAddress())) {
				BlockedAddress status = blocked.get(sender.getAddress());
				if (status.getTime() <= BlockedAddress.PERMANENT_BLOCK) {
					datagram.content().release(); // No longer needed
					return; // Permanently blocked
				}
				if (System.currentTimeMillis() - status.getStartTime() < status.getTime()) {
					datagram.content().release(); // No longer needed
					return; // Time hasn't expired
				}
				this.unblockAddress(sender.getAddress());
			}

			// Handle the packet and release the buffer
			server.handleMessage(packet, sender);
			datagram.content().readerIndex(0); // Reset position
			LOG.debug(loggerName + "Sent packet to server and reset Datagram buffer read position");
			for (RakNetServerListener listener : server.getListeners()) {
				listener.handleNettyMessage(datagram.content(), sender);
			}
			datagram.content().release(); // No longer needed
			LOG.debug(loggerName + "Sent Datagram buffer to server and released it");

			// No exceptions occurred, release the suspect
			this.causeAddress = null;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		server.handleHandlerException(this.causeAddress, cause);
	}

}
