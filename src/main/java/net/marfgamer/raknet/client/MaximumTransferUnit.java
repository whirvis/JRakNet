package net.marfgamer.raknet.client;

public class MaximumTransferUnit {

	private final int maximumTransferUnit;
	private int retries;

	public MaximumTransferUnit(int maximumTransferUnit, int retries) {
		this.maximumTransferUnit = maximumTransferUnit;
		this.retries = retries;
	}

	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}
	
	public int getRetries() {
		return this.retries;
	}

	public int retry() {
		return this.retries--;
	}

}
