package net.marfgamer.raknet.exception.client;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.client.RakNetClientHandler;
import net.marfgamer.raknet.exception.RakNetClientException;

/**
 * This exception is thrown whenever the <code>RakNetClientHandler</code>
 * catches an exception caused by the server the client is attempting to connect
 * to
 *
 * @author MarfGamer
 */
public class NettyHandlerException extends RakNetClientException {

	private static final long serialVersionUID = -7405227886962804185L;

	private final RakNetClientHandler handler;
	private final Throwable cause;

	public NettyHandlerException(RakNetClient client, RakNetClientHandler handler, Throwable cause) {
		super(client, "Exception in handler! " + cause.getMessage());
		this.handler = handler;
		this.cause = cause;
	}

	/**
	 * Returns the handler the client is using
	 * 
	 * @return The handler the client is using
	 */
	public RakNetClientHandler getHandler() {
		return this.handler;
	}

	/**
	 * Returns the exception that was caught by the handler
	 *
	 * @return The exception that was caught by the handler
	 */
	public Throwable getCause() {
		return this.cause;
	}

}
