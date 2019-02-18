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
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
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
import com.whirvis.jraknet.session.RakNetClientSession;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

/**
 * Used by the {@link com.whirvis.jraknet.server.RakNetServer RakNetServer} with
 * the sole purpose of sending received packets to the server so they can be
 * handled. Any errors that occurs will also be sent to the server to be dealt
 * with.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0
 * @see com.whirvis.jraknet.client.RakNetServer RakNetServer
 */
public class RakNetServerHandler extends ChannelInboundHandlerAdapter {

	private final Logger log;
	private final RakNetServer server;
	private final ConcurrentHashMap<InetAddress, BlockedAddress> blocked;
	private InetSocketAddress causeAddress;

	/**
	 * Creates a RakNet server Netty handler.
	 * 
	 * @param server
	 *            the server to send received packets to.
	 * @see com.whirvis.jraknet.RakNetServer RakNetServer
	 */
	public RakNetServerHandler(RakNetServer server) {
		this.log = LogManager
				.getLogger("RakNet server handler #" + Long.toHexString(server.getGloballyUniqueId()).toUpperCase());
		this.server = server;
		this.blocked = new ConcurrentHashMap<InetAddress, BlockedAddress>();
	}

	/**
	 * Blocks the IP address. All currently connected clients with the IP
	 * address (regardless of port) will be disconnected with the same reason
	 * that the IP address was blocked.
	 * 
	 * @param address
	 *            the IP address to block.
	 * @param reason
	 *            the reason the address was blocked. A <code>null</code> reason
	 *            will have <code>"Address blocked"</code> be used as the reason
	 *            instead.
	 * @param time
	 *            how long the address will blocked in milliseconds.
	 * @throws NullPointerException
	 *             if <code>address</code> is <code>null</code>.
	 */
	protected void blockAddress(InetAddress address, String reason, long time) {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		}
		blocked.put(address, new BlockedAddress(time));
		for (RakNetClientSession client : server.getClients()) {
			if (!client.getAddress().getAddress().equals(address)) {
				continue; // Client is not blocked
			}
			server.disconnectClient(client, reason == null ? "Address blocked" : reason);
		}
		server.callEvent(listener -> listener.onAddressBlocked(address, reason, time));
		log.info("Blocked address " + address + " due to \"" + reason + "\" for " + time + " milliseconds");
	}

	/**
	 * Unblocks the IP address.
	 * 
	 * @param address
	 *            the IP address to unblock.
	 * @throws NullPointerException
	 *             if <code>address</code> is <code>null</code>.
	 */
	protected void unblockAddress(InetAddress address) {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (blocked.remove(address) == null) {
			return; // No address was unblocked
		}
		server.callEvent(listener -> listener.onAddressUnblocked(address));
		log.info("Unblocked address " + address);
	}

	/**
	 * Returns whether or not the IP address is blocked.
	 * 
	 * @param address
	 *            the IP address to check.
	 * @return <code>true</code> if the IP address is blocked,
	 *         <code>false</code> otherwise.
	 */
	public boolean isAddressBlocked(InetAddress address) {
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

			// Check if address is blocked
			if (this.isAddressBlocked(sender.getAddress())) {
				BlockedAddress status = blocked.get(sender.getAddress());
				if (!status.shouldUnblock()) {
					datagram.content().release(); // No longer needed
					return; // Address still blocked
				}
				this.unblockAddress(sender.getAddress());
			}

			// Handle the packet and release the buffer
			server.handleMessage(packet, sender);
			datagram.content().readerIndex(0); // Reset position
			log.debug("Sent packet to server and reset datagram buffer read position");
			server.callEvent(listener -> listener.handleNettyMessage(datagram.content(), sender));
			datagram.content().release(); // No longer needed
			log.debug("Sent datagram buffer to server and released it");

			// No exceptions occurred, release the suspect
			this.causeAddress = null;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		server.handleHandlerException(this.causeAddress, cause);
	}

}
