package net.marfgamer.raknet.server;

/**
 * This class represents an address that the server has blocked and stores how
 * much time until they are unblocked
 *
 * @author MarfGamer
 */
public class BlockedClient {

	public static final int PERMANENT_BLOCK = -1;

	private final long startTime;
	private final long time;

	public BlockedClient(long startTime, long time) {
		this.startTime = startTime;
		this.time = time;
	}

	/**
	 * Returns when the address was first blocked
	 * 
	 * @return When the address was first blocked
	 */
	public long getStartTime() {
		return this.startTime;
	}

	/**
	 * Returns how long until the address is unblocked
	 * 
	 * @return How long until the address is unblocked
	 */
	public long getTime() {
		return this.time;
	}

}
