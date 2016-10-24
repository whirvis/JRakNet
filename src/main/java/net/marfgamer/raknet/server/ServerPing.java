package net.marfgamer.raknet.server;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.identifier.Identifier;

/**
 * Used primarily to box an identifier sent in the response of a server to a
 * ping sent by a client (Or maybe another server? :O) so it can be modified by
 * the user
 *
 * @author MarfGamer
 */
public class ServerPing {

	private final InetSocketAddress sender;
	private Identifier identifier;

	public ServerPing(InetSocketAddress sender, Identifier identifier) {
		this.sender = sender;
		this.identifier = identifier;
	}

	/**
	 * Returns the sender's address of the ping
	 * 
	 * @return The sender's address of the ping
	 */
	public InetSocketAddress getSender() {
		return this.sender;
	}

	/**
	 * Returns the identifier being sent back to the sender
	 * 
	 * @return The identifier being sent back to the sender
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Sets the identifier being sent back to the sender
	 * 
	 * @param identifier
	 *            - The new identifier
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

}
