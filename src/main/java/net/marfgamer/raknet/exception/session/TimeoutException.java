package net.marfgamer.raknet.exception.session;

/**
 * This exception is thrown whenever a session has timed out
 *
 * @author MarfGamer
 */
public class TimeoutException extends RuntimeException {

	private static final long serialVersionUID = 4216977972114486611L;

	public TimeoutException() {
		super("The session has timed out!");
	}

}
