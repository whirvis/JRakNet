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
	 *            - The client enabling its discovery system
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
	 *            - The client disabling its discovery system
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
				client.updateDiscoveryData();
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
