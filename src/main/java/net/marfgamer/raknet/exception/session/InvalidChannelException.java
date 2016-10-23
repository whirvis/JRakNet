package net.marfgamer.raknet.exception.session;

import net.marfgamer.raknet.RakNet;

/**
 * This exception is thrown whenever the channel in a sent packet or received
 * packet is higher than the limit
 *
 * @author MarfGamer
 */
public class InvalidChannelException extends IllegalArgumentException {

	private static final long serialVersionUID = -8690545139286694469L;

	public InvalidChannelException() {
		super("The channel can be no larger than " + RakNet.MAX_CHANNELS);
	}

}
