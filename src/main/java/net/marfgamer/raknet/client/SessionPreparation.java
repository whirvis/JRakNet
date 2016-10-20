package net.marfgamer.raknet.client;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.session.RakNetServerSession;

public class SessionPreparation {

	private final RakNetClient client;

	public boolean cancelled;
	public RakNetException cancelReason;

	public long guid = -1;
	public int maximumTransferUnit = -1;
	public InetSocketAddress address = null;
	public boolean loginPackets[] = new boolean[2];

	public SessionPreparation(RakNetClient client) {
		this.client = client;
	}

	public boolean readyForSession() {
		// It was cancelled, why are we finishing?
		if (cancelled == true) {
			return false;
		}

		// Not all of the data has been set
		if (this.guid == -1 || this.maximumTransferUnit == -1 || this.address == null) {
			return false;
		}

		// Not all of the packets needed to connect have been handled
		for (boolean handled : loginPackets) {
			if (handled == false) {
				return false;
			}
		}

		// Nothing returned false, everything is ready!
		return true;
	}

	public RakNetServerSession createSession(Channel channel) {
		return new RakNetServerSession(client, guid, maximumTransferUnit, channel, address);
	}

}
