package net.marfgamer.raknet.protocol.unconnected;

import net.marfgamer.raknet.Packet;

public class UnconnectedPingOpenConnections extends UnconnectedPing {

	public UnconnectedPingOpenConnections() {
		super(true);
	}

	public UnconnectedPingOpenConnections(Packet packet) {
		super(packet);
	}

}
