package net.marfgamer.raknet.client.discovery;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.identifier.Identifier;

/**
 * This class represents a discovered RakNet server
 *
 * @author MarfGamer
 */
public class DiscoveredServer {

	public static final long SERVER_TIMEOUT_MILLI = 5000L;

	private final InetSocketAddress address;
	private long discoveryTimestamp;
	private Identifier identifier;

	public DiscoveredServer(InetSocketAddress address, long discoveryTimestamp, Identifier identifier) {
		this.address = address;
		this.discoveryTimestamp = discoveryTimestamp;
		this.identifier = identifier;
	}

	/**
	 * Returns the address of the discovered server
	 * 
	 * @return The address of the discovered server
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * Returns the last time the server sent a response back
	 * 
	 * @return The last time the server sent a response back
	 */
	public long getDiscoveryTimestamp() {
		return this.discoveryTimestamp;
	}

	/**
	 * Updates the last time the server sent a response back
	 * 
	 * @param discoveryTimestamp
	 *            - The new discovery timestamp
	 */
	public void setDiscoveryTimestamp(long discoveryTimestamp) {
		this.discoveryTimestamp = discoveryTimestamp;
	}

	/**
	 * Returns the identifier sent in the response
	 * 
	 * @return The identifier sent in the response
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Updates the identifier sent in the response
	 * 
	 * @param identifier
	 *            - The new identifier sent in the response
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

}
