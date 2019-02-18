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

import java.net.InetSocketAddress;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.protocol.status.UnconnectedPing;
import com.whirvis.jraknet.protocol.status.UnconnectedPingOpenConnections;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Used by the {@link com.whirvis.jraknet.discovery.Discovery Discovery} to
 * server pings in the background.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.11.0
 * @see com.whirvis.jraknet.discovery.Discovery Discovery
 * @see com.whirvis.jraknet.discovery.DiscoveredSerer DiscoveredServer
 */
public class DiscoveryThread extends Thread {

	private final Logger log;
	private final Bootstrap bootstrap;
	private final NioEventLoopGroup group;
	private final DiscoveryHandler handler;
	private Channel channel;
	private long lastPingBroadcast;

	/**
	 * Allocates a discovery thread.
	 * 
	 * @throws InterruptedException
	 *             if channel binding is interrupted.
	 */
	protected DiscoveryThread() {
		this.log = LogManager.getLogger("jraknet-discovery-thread");
		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new DiscoveryHandler();
		bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
		bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
		try {
			this.channel = bootstrap.bind(0).sync().channel();
		} catch (InterruptedException e) {
			this.interrupt(); // How unfortunate
		}
		this.setName(log.getName());
	}

	/**
	 * Sends a Netty message over the channel raw.
	 * 
	 * @param packet
	 *            the packet to send.
	 * @param address
	 *            the address to send the packet to.
	 */
	public final void sendNettyMessage(Packet packet, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
		log.debug("Sent netty message with size of " + packet.size() + " bytes (" + (packet.size() * 8) + " bits) to "
				+ address);
	}

	@Override
	public void run() {
		log.debug("Started discovery thread");
		while (Discovery.LISTENERS.size() > 0 && Discovery.getDiscoveryMode() != DiscoveryMode.DISABLED
				&& Discovery.getDiscoveryAddresses().length > 0 && Discovery.thread == this && !this.isInterrupted()) {
			try {
				Thread.sleep(0, 1); // Save CPU usage
			} catch (InterruptedException e) {
				this.interrupt(); // Interrupted during sleep
			}
			long currentTime = System.currentTimeMillis();

			// Forget servers that have taken too long to respond back
			ArrayList<InetSocketAddress> forgottenServers = new ArrayList<InetSocketAddress>();
			for (InetSocketAddress address : Discovery.DISCOVERED.keySet()) {
				DiscoveredServer discovered = Discovery.DISCOVERED.get(address);
				if (discovered.hasTimedOut()) {
					forgottenServers.add(address);
					Discovery.callEvent(listener -> listener.onServerForgotten(address, discovered.isExternal()));
				}
			}
			Discovery.DISCOVERED.keySet().removeAll(forgottenServers);
			if (forgottenServers.size() > 0) {
				log.debug("Forgot " + forgottenServers.size() + " server" + (forgottenServers.size() == 1 ? "" : "s"));
			}

			// Broadcast ping to local and external servers
			if (currentTime - lastPingBroadcast > 1000L) {
				UnconnectedPing ping = Discovery.getDiscoveryMode() == DiscoveryMode.OPEN_CONNECTIONS
						? new UnconnectedPingOpenConnections() : new UnconnectedPing();
				ping.timestamp = Discovery.getTimestamp();
				ping.pingId = Discovery.PING_ID;
				ping.encode();
				if (ping.failed()) {
					Discovery.setDiscoveryMode(DiscoveryMode.DISABLED);
					log.error("Failed to encode unconnected ping, disabled discovery system");
				}
				for (InetSocketAddress discoveryAddress : Discovery.getDiscoveryAddresses()) {
					this.sendNettyMessage(ping, discoveryAddress);
				}
				this.lastPingBroadcast = currentTime;
			}

		}
		
		/**
		 * TODO
		 */
		if (Discovery.thread == this) {
			Discovery.thread = null;
			log.debug("Terminated discovery thread");
		}
	}

}
