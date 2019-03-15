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
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.protocol.status.UnconnectedPing;
import com.whirvis.jraknet.protocol.status.UnconnectedPingOpenConnections;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Used by the {@link Discovery} system to server pings in the background.
 * <p>
 * Only one instance of this class can exist at a time. When a discovery
 * listener is added, if discovery is enabled and there is at least one
 * discovery address to broadcast pings to then the discovery system will check
 * if one of these threads is already running. If none exist, one will be
 * created and started automatically. When there are no listeners, no discovery
 * addresses, or discovery is disabled, the discovery thread will shutdown and
 * nullify its own reference in the discovery system. If another listener is
 * added, the discovery system is enabled, and there is at least one discovery
 * address, the process will repeat.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.11.0
 */
public final class DiscoveryThread extends Thread {

	private final Logger log;
	private final Bootstrap bootstrap;
	private final NioEventLoopGroup group;
	private final DiscoveryHandler handler;
	private Channel channel;
	private long lastPingBroadcast;

	/**
	 * Allocates a discovery thread.
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
			this.interrupt(); // Cause thread to immediately break out of loop
			Discovery.setDiscoveryMode(DiscoveryMode.DISABLED);
			log.error("Failed to bind channel necessary for broadcasting pings, disabled discovery system");
		}
		this.setName(log.getName());
	}

	@Override
	public void run() {
		log.debug("Started discovery thread");
		while (!Discovery.LISTENERS.isEmpty() && !Discovery.DISCOVERY_ADDRESSES.isEmpty()
				&& Discovery.discoveryMode != DiscoveryMode.DISABLED && !this.isInterrupted()) {
			if (Discovery.thread != this) {
				/*
				 * Normally we would just break the thread here, but if two of
				 * these are running it indicates a synchronization problem in
				 * the code.
				 */
				throw new IllegalStateException(
						"Discovery thread must be this while running, are there multiple discovery threads running?");
			}
			try {
				Thread.sleep(0, 1); // Save CPU usage
			} catch (InterruptedException e) {
				this.interrupt(); // Interrupted during sleep
				continue;
			}
			long currentTime = System.currentTimeMillis();

			// Forget servers that have taken too long to respond back
			ArrayList<InetSocketAddress> forgottenServers = new ArrayList<InetSocketAddress>();
			for (InetSocketAddress address : Discovery.DISCOVERED.keySet()) {
				DiscoveredServer discovered = Discovery.DISCOVERED.get(address);
				if (discovered.hasTimedOut()) {
					forgottenServers.add(address);
					Discovery.callEvent(listener -> listener.onServerForgotten(discovered));
				}
			}
			Discovery.DISCOVERED.keySet().removeAll(forgottenServers);
			if (!forgottenServers.isEmpty()) {
				log.debug("Forgot " + forgottenServers.size() + " server" + (forgottenServers.size() == 1 ? "" : "s"));
			}

			// Broadcast ping to local and external servers
			if (currentTime - lastPingBroadcast > 1000L) {
				UnconnectedPing ping = Discovery.getDiscoveryMode() == DiscoveryMode.OPEN_CONNECTIONS
						? new UnconnectedPingOpenConnections() : new UnconnectedPing();
				ping.timestamp = Discovery.getTimestamp();
				ping.pingId = Discovery.getPingId();
				ping.encode();
				if (ping.failed()) {
					this.interrupt();
					Discovery.setDiscoveryMode(DiscoveryMode.DISABLED);
					log.error("Failed to encode unconnected ping, disabled discovery system");
				}
				for (InetSocketAddress address : Discovery.DISCOVERY_ADDRESSES.keySet()) {
					channel.writeAndFlush(new DatagramPacket(ping.buffer(), address));
				}
				log.debug("Sent unconnected ping to " + Discovery.DISCOVERY_ADDRESSES.size() + " server"
						+ (Discovery.DISCOVERY_ADDRESSES.size() == 1 ? "" : "s"));
				this.lastPingBroadcast = currentTime;
			}
		}

		/*
		 * If there are no listeners, no discovery addresses, or discovery is
		 * simply disabled, we will destroy this thread by nullifying the
		 * scheduler's reference after the loop has been broken out of. If any
		 * of these conditions changes, then a new discovery thread will be
		 * created automatically.
		 */
		channel.close();
		group.shutdownGracefully(0L, 1000L, TimeUnit.MILLISECONDS);
		this.channel = null;
		if (Discovery.thread == this) {
			Discovery.thread = null;
			log.debug("Terminated discovery thread");
		}
	}

}
