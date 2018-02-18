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
 * Copyright (c) 2016-2018 Trent Summerlin
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
package com.whirvis.jraknet.client.discovery;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whirvis.jraknet.client.RakNetClient;

/**
 * This <code>Thread</code> is used by the <code>RakNetClient</code> to discover
 * servers, allowing the client to discover servers without being started and
 * without having to take up multiple threads.
 *
 * @author Trent Summerlin
 */
public class DiscoveryThread extends Thread {

	private static final Logger log = LoggerFactory.getLogger(DiscoveryThread.class);

	// Discovery data
	private ConcurrentLinkedQueue<RakNetClient> clients;
	private volatile boolean running;

	/**
	 * Constructs a <code>DiscoveryThread</code>.
	 */
	public DiscoveryThread() {
		this.clients = new ConcurrentLinkedQueue<RakNetClient>();
		this.setName("DiscoveryThread");
	}

	/**
	 * @return the clients that are currently using the discovery system.
	 */
	public RakNetClient[] getClients() {
		return clients.toArray(new RakNetClient[clients.size()]);
	}

	/**
	 * Adds a <code>RakNetClient</code> to the discovery system so it can discover
	 * servers.
	 * 
	 * @param client
	 *            the <code>RakNetClient</code> enabling its discovery system.
	 */
	public void addClient(RakNetClient client) {
		if (clients.contains(client)) {
			log.warn("Client #" + client.getGloballyUniqueId()
					+ " attempted to add itself to the discovery thread even though it is already in the thread.");
			return;
		}
		clients.add(client);
		log.debug("Added client #" + client.getGloballyUniqueId() + " to the discovery thread");
	}

	/**
	 * Removes a <code>RakNetClient</code> from the discovery system, this method is
	 * also called automatically by the client when it is garbage collected.
	 * 
	 * @param client
	 *            the <code>RakNetClient</code> disabling its discovery system.
	 */
	public void removeClient(RakNetClient client) {
		if (!clients.contains(client)) {
			log.warn("Client #" + client.getGloballyUniqueId()
					+ " attempted to remove itself from the discovery thread even though it is not in the thread");
			return;
		}
		clients.remove(client);
		log.debug("Removed client #" + client.getGloballyUniqueId() + " from the discovery thread");
	}

	/**
	 * @return <code>true</code> if the thread has been started.
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Shuts down the discovery system.
	 */
	public void shutdown() {
		this.running = false;
		log.info("Shutdown discovery thread");
	}

	@Override
	public void run() {
		this.running = true;
		log.info("Started discovery thread");
		while (this.running) {
			try {
				Thread.sleep(1000); // Lower CPU usage and prevent spamming
				if (clients.size() <= 0) {
					continue; // Do not loop through non-existent clients
				}
				for (RakNetClient client : this.clients) {
					client.updateDiscoveryData();
				}
				log.debug(
						"Sent discovery info out for " + clients.size() + " client" + (clients.size() == 1 ? "" : "s"));
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error("Discovery thread has crashed");
			}
		}
	}

}
