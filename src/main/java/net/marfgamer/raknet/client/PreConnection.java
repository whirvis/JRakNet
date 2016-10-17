package net.marfgamer.raknet.client;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.session.RakNetServerSession;

public class PreConnection {

	public boolean cancelled;
	public String cancelReason;
	public int maximumTransferUnit;
	public long serverGuid;
	public InetSocketAddress serverAddress;
	public RakNetServerSession serverSession;
	public boolean loginPackets[] = new boolean[2];

	public void reset() {
		this.cancelled = false;
		this.cancelReason = null;
		this.maximumTransferUnit = 0;
		this.serverAddress = null;
		this.loginPackets = new boolean[2];
	}

}
