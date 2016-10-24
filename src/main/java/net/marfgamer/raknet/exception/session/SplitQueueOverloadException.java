package net.marfgamer.raknet.exception.session;

/**
 * This exception is thrown whenever there are too many split packets in the
 * queue at once
 *
 * @author MarfGamer
 */
public class SplitQueueOverloadException extends RuntimeException {

	private static final long serialVersionUID = 969985052588965615L;

	public SplitQueueOverloadException() {
		super("Too many split packets in a single queue!");
	}

}
