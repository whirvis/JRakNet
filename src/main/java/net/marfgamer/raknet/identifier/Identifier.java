package net.marfgamer.raknet.identifier;

/**
 * This class represents an identifier sent from a server on the local network
 *
 * @author MarfGamer
 */
public class Identifier {

	protected String identifier;

	public Identifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Converts the identifier to a String
	 * 
	 * @return The identifier as a String
	 */
	public final String build() {
		return this.identifier;
	}

	@Override
	public final String toString() {
		return this.build();
	}

}
