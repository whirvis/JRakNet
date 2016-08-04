package net.marfgamer.raknet.session;

import io.netty.channel.socket.DatagramChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;

import net.marfgamer.raknet.protocol.Message;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.protocol.raknet.internal.EncapsulatedPacket;

public class RakNetSession {

	// Client info
	private RakNetState state = RakNetState.DISCONNECTED;
	private final InetSocketAddress address;
	private final DatagramChannel channel;

	// Packet info
	private int customSeqNumber = 0;
	private int[] sendIndex = new int[32];
	private int[] receiveIndex = new int[32];
	private final HashMap<Integer, CustomPacket>[] recoveryQueue;
	private final HashMap<Integer, EncapsulatedPacket>[] handleQueue;

	@SuppressWarnings("unchecked")
	public RakNetSession(InetSocketAddress address, DatagramChannel channel) {
		this.address = address;
		this.channel = channel;
		this.recoveryQueue = new HashMap[32];
		this.handleQueue = new HashMap[32];
	}

	public RakNetState getState() {
		return this.state;
	}

	public void setState(RakNetState state) {
		this.state = state;
	}

	public InetSocketAddress getSocketAddress() {
		return this.address;
	}

	public InetAddress getAddress() {
		return address.getAddress();
	}

	public int getPort() {
		return address.getPort();
	}

	public void handleDataPacket(EncapsulatedPacket encapsulated) {
		Reliability reliability = encapsulated.reliability;
		

		if (reliability.isOrdered()) {

		} else if (reliability.isSequenced()) {
			if (encapsulated.orderIndex > receiveIndex[encapsulated.orderChannel]) {
				
				return;
			}
		}

		// Packet has been handled
		
	}

	public void sendDataPacket(EncapsulatedPacket encapsulated) {
		CustomPacket custom = new CustomPacket();
		custom.seqNumber = customSeqNumber++;
		custom.packets.add(encapsulated);
		custom.encode();
	}

	public void sendDataPacket(Reliability reliability, Message packet) {
		EncapsulatedPacket encapsulated = new EncapsulatedPacket();
		encapsulated.reliability = reliability;
		encapsulated.orderChannel = 0; // TODO
		encapsulated.orderIndex = sendIndex[encapsulated.orderIndex]++;
		encapsulated.payload = packet.array();
		this.sendDataPacket(encapsulated);
	}

}
