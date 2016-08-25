package net.marfgamer.raknet.server.identifier;

public class Identifier {

	protected String identifier;

	public Identifier(String identifier) {
		this.identifier = identifier;
	}

	public final String build() {
		return this.identifier;
	}

	@Override
	public final String toString() {
		return this.build();
	}

}
