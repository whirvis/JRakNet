package net.marfgamer.raknet.exception;

import net.marfgamer.raknet.server.RakNetServer;

public class RakNetServerException extends RakNetException {

	private static final long serialVersionUID = -1822503535831173905L;

	private final RakNetServer server;

	public RakNetServerException(RakNetServer server, String error) {
		super(error);
		this.server = server;
	}

	public final RakNetServer getServer() {
		return this.server;
	}

}
