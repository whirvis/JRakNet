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
 * Copyright (c) 2016, 2017 Trent "MarfGamer" Summerlin
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
package net.marfgamer.jraknet.client.discovery;

import java.util.ArrayList;

import net.marfgamer.jraknet.RakNetLogger;
import net.marfgamer.jraknet.client.RakNetClient;
import net.marfgamer.jraknet.util.RakNetUtils;

/**
 * This <code>Thread</code> is used by the <code>RakNetClient</code> to discover
 * servers, allowing the client to discover servers without being started and
 * without having to take up multiple threads.
 *
 * @author Trent "MarfGamer" Summerlin
 */
public class DiscoveryThread extends Thread {

	// Logger name
	private static final String LOGGER_NAME = "discovery thread";

	// Discovery data
	private ArrayList<RakNetClient> clients;
	private volatile boolean running;

	/**
	 * Constructs a <code>DiscoveryThread</code>.
	 */
	public DiscoveryThread() {
		this.clients = new ArrayList<RakNetClient>();
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
			RakNetLogger.warn(LOGGER_NAME, "Client #" + client.getGloballyUniqueId()
					+ " attempted to add itself to the discovery thread even though it is already in the thread.");
			return;
		}
		clients.add(client);
		RakNetLogger.info(LOGGER_NAME, "Added client #" + client.getGloballyUniqueId() + " to the discovery thread");
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
			RakNetLogger.warn(LOGGER_NAME, "Client #" + client.getGloballyUniqueId()
					+ " attempted to remove itself from the discovery thread even though it is not in the thread");
			return;
		}
		clients.remove(client);
		RakNetLogger.info(LOGGER_NAME,
				"Removed client #" + client.getGloballyUniqueId() + " from the discovery thread");
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
		RakNetLogger.info(LOGGER_NAME, "Shutdown discovery thread");
	}

	@Override
	public synchronized void run() {
		this.running = true;
		RakNetLogger.info(LOGGER_NAME, "Started discovery thread");
		while (this.running) {
			for (RakNetClient client : this.clients) {
				client.updateDiscoveryData();
			}
			if (clients.size() > 0) {
				RakNetLogger.info(LOGGER_NAME,
						"Sent discovery info out for " + clients.size() + " client" + (clients.size() == 1 ? "" : "s"));
			} else {
				RakNetLogger.warn(LOGGER_NAME, "Sent discovery info out for no clients");
			}
			RakNetUtils.threadLock(1000L);
		}
	}

}
