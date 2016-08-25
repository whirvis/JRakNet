package net.marfgamer.raknet.server;

public class BlockedClient {

	private final long startTime;
	private final long time;

	public BlockedClient(long startTime, long time) {
		this.startTime = startTime;
		this.time = time;
	}

	public long getStartTime() {
		return this.startTime;
	}

	public long getTime() {
		return this.time;
	}

}
