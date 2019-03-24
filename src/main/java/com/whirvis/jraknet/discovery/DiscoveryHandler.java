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
package com.whirvis.jraknet.discovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.status.UnconnectedPong;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

/**
 * Used by the {@link Discovery} system with the sole purpose of sending
 * received packets to the discovery system so they can be handled. If any
 * errors occur while handling a packet, it will be ignored.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.11.0
 */
public final class DiscoveryHandler extends ChannelInboundHandlerAdapter {

	private Logger log;
	private final ArrayList<InetAddress> blocked;
	private InetSocketAddress causeAddress;

	/**
	 * Creates a discovery system Netty handler.
	 */
	protected DiscoveryHandler() {
		this.log = LogManager.getLogger("jraknet-discovery-handler");
		this.blocked = new ArrayList<InetAddress>();
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

			// Check if the address is blocked
			if (blocked.contains(sender.getAddress())) {
				return; // Address blocked
			}

			// Handle the packet and release the buffer
			if (packet.getId() == RakNetPacket.ID_UNCONNECTED_PONG) {
				UnconnectedPong pong = new UnconnectedPong(packet);
				pong.decode();
				if (!pong.failed()) {
					Discovery.updateDiscoveryData(sender, pong);
					log.debug("Sent unconnected pong to discovery system");
				}
			}
			datagram.content().release(); // No longer needed

			// No exceptions occurred, release the suspect
			this.causeAddress = null;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (!blocked.contains(causeAddress.getAddress())) {
			blocked.add(causeAddress.getAddress());
			log.warn("Blocked address " + causeAddress.getAddress() + " that caused " + cause.getClass().getSimpleName()
					+ " to be thrown, discovering servers from it will no longer be possible");
		} else {
			log.error("Blocked address still cause exception to be thrown", cause);
		}
	}

}
