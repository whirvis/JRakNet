package net.marfgamer.raknet.client;

import java.util.ArrayList;
import java.util.Collections;

import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.exception.client.DiscoveryAlreadyEnabledException;

public class DiscoveryThread extends Thread {

	private final ArrayList<RakNetClient> clients;
	private boolean running;

	public DiscoveryThread() {
		this.clients = new ArrayList<RakNetClient>();
	}

	protected void addClient(RakNetClient client) throws RakNetException {
		if (clients.contains(client)) {
			throw new DiscoveryAlreadyEnabledException(client);
		}
		clients.add(client);
	}

	protected boolean isRunning() {
		return this.running;
	}

	@Override
	public void run() {
		this.running = true;
		try {
			while (this.running == true) {
				clients.removeAll(Collections.singleton(null));
				for (RakNetClient client : clients) {
					if (client.discoveryEnabled()) {
						client.sendPing();
					}
				}
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopDiscovery() {
		this.running = false;
	}

}
