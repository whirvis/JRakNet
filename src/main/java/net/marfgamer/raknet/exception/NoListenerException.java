package net.marfgamer.raknet.exception;

/**
 * This exception is thrown whenever there is no listener set for the client or
 * server
 *
 * @author MarfGamer
 */
public class NoListenerException extends RuntimeException {

	private static final long serialVersionUID = 1841007286123953067L;

	public NoListenerException(String task) {
		super(task);
	}

}
