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
package net.marfgamer.raknet.client.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.marfgamer.raknet.client.RakNetClient;

/**
 * This class is a Thread that allows clients to discover servers on the local
 * network without having to actually be started
 *
 * @author MarfGamer
 */
public class DiscoveryThread extends Thread {

	private List<RakNetClient> clients;
	private boolean running;

	public DiscoveryThread() {
		this.clients = Collections.synchronizedList(new ArrayList<RakNetClient>());
	}

	/**
	 * Adds a client to the discovery system so it can discover servers
	 * 
	 * @param client
	 *            The client enabling its discovery system
	 */
	public void addClient(RakNetClient client) {
		if (clients.contains(client)) {
			return;
		}
		clients.add(client);
	}

	/**
	 * Removes a client from the discovery system so it will no longer discover
	 * servers <br>
	 * Note: This method is normally called when a client is garbage collected
	 * 
	 * @param client
	 *            The client disabling its discovery system
	 */
	public void removeClient(RakNetClient client) {
		clients.remove(client);
	}

	/**
	 * Returns whether or not the thread has already been started
	 * 
	 * @return Whether or not the thread has already been started
	 */
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public synchronized void run() {
		this.running = true;
		while (true) {
			for (RakNetClient client : this.clients) {
				if (client.getDiscoveryMode() != DiscoveryMode.NONE) {
					client.updateDiscoveryData();
				}
			}

			// Exceptions caught here have to do with the thread
			try {
				Thread.sleep(1000L);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
