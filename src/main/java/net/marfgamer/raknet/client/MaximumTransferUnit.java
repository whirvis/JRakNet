package net.marfgamer.raknet.client;

/**
 * This class represents one of the maximum transfer units used during login
 *
 * @author MarfGamer
 */
public class MaximumTransferUnit {

	private final int maximumTransferUnit;
	private int retries;

	public MaximumTransferUnit(int maximumTransferUnit, int retries) {
		this.maximumTransferUnit = maximumTransferUnit;
		this.retries = retries;
	}

	/**
	 * Returns the size of the maximum transfer unit
	 * 
	 * @return The size of the maximum transfer unit
	 */
	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Returns the amount of retries before the client stops using this maximum
	 * transfer unit and lowers it
	 * 
	 * @return The amount of retries before the client stops using this maximum
	 *         transfer unit and lowers it
	 */
	public int getRetries() {
		return this.retries;
	}

	/**
	 * Returns how many retries there are left and then lowers it by one
	 * 
	 * @return How many retries are left
	 */
	public int retry() {
		return this.retries--;
	}

}
