package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.client.RakNetClientHandler;
import net.marfgamer.raknet.exception.RakNetClientException;

public class NettyHandlerException extends RakNetClientException {

	private static final long serialVersionUID = -7405227886962804185L;

	private final RakNetClientHandler handler;
	private final Throwable cause;

	public NettyHandlerException(RakNetClient client, RakNetClientHandler handler, Throwable cause) {
		super(client, "Exception in handler! " + cause.getMessage());
		this.handler = handler;
		this.cause = cause;
	}

	public RakNetClientHandler getHandler() {
		return this.handler;
	}

	public Throwable getCause() {
		return this.cause;
	}

}
