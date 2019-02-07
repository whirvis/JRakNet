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
package com.whirvis.jraknet.client.discovery;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.client.RakNetClient;

/**
 * This thread is used by a {@link com.whirvis.jraknet.client.RakNetClient
 * RakNetClient} to discover servers without needing to be explicitly started.
 * <p>
 * Only one instance of this class can exist at a time. When a client is
 * created, it will automatically check if a discovery system is already
 * running. If none exists, one will be created and started automatically. Once
 * the last client using the discovery system has shutdown, it will shut down
 * the discovery system and nullify the reference. If another client is created
 * after this, the process will repeat.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0
 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
 * @see com.whirvis.jraknet.client.discovery.DiscoveredServer DiscoveredServer
 */
public class DiscoveryThread extends Thread {

	private static final Logger LOG = LogManager.getLogger(DiscoveryThread.class);

	private ConcurrentLinkedQueue<RakNetClient> clients;
	private volatile boolean running;

	/**
	 * Creates a discovery system.
	 */
	public DiscoveryThread() {
		this.clients = new ConcurrentLinkedQueue<RakNetClient>();
		this.setName("DiscoveryThread");
	}

	/**
	 * Returns the clients that are currently using the discovery system.
	 * 
	 * @return the clients that are currently using the discovery system.
	 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
	 */
	public RakNetClient[] getClients() {
		return clients.toArray(new RakNetClient[clients.size()]);
	}

	/**
	 * Adds the client to the discovery system so it can discover servers. This
	 * is the equivalent of the client enabling its discovery system.
	 * 
	 * @param client
	 *            the client enabling its discovery system.
	 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
	 */
	public void addClient(RakNetClient client) {
		if (clients.contains(client)) {
			LOG.warn("Client #" + client.getGloballyUniqueId()
					+ " attempted to add itself to the discovery thread even though it is already in the thread.");
			return;
		}
		clients.add(client);
		LOG.debug("Added client #" + client.getGloballyUniqueId() + " to the discovery thread");
	}

	/**
	 * Removes the client from the discovery system. This is the equivalent of
	 * the client disabling its discovery system. This method is also called
	 * automatically by the client when it is shutdown.
	 * 
	 * @param client
	 *            the client disabling its discovery system.
	 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
	 */
	public void removeClient(RakNetClient client) {
		if (!clients.contains(client)) {
			LOG.warn("Client #" + client.getGloballyUniqueId()
					+ " attempted to remove itself from the discovery thread even though it is not in the thread");
			return;
		}
		clients.remove(client);
		LOG.debug("Removed client #" + client.getGloballyUniqueId() + " from the discovery thread");
	}

	/**
	 * Returns whether or not the thread is running
	 * 
	 * @return <code>true</code> if the thread is running, <code>false</code>
	 *         otherwise.
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Shuts down the discovery system. This method should only ever be called
	 * by the last remaining client once it has been shutdown.
	 * 
	 * @throws IllegalStateException
	 *             if any clients are still using the discovery system.
	 */
	public void shutdown() {
		if (clients.size() > 0) {
			throw new IllegalStateException(clients.size() + " are still using the discovery system");
		}
		this.running = false;
		LOG.info("Shutdown discovery thread");
	}

	@Override
	public void run() {
		this.running = true;
		LOG.info("Started discovery thread");
		try {
			while (running == true) {
				// Lower CPU usage and prevent ping spamming
				Thread.sleep(1000);
				if (clients.size() <= 0) {
					continue;
				}

				// Update discovery data for clients
				for (RakNetClient client : this.clients) {
					client.updateDiscoveryData();
				}
				LOG.debug(
						"Sent discovery info out for " + clients.size() + " client" + (clients.size() == 1 ? "" : "s"));
			}
		} catch (InterruptedException e) {
			this.running = false;
			e.printStackTrace();
			LOG.error("Discovery thread has crashed");
		}
	}

}
