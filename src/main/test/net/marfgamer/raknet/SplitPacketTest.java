package net.marfgamer.raknet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.client.RakNetClientListener;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.message.CustomPacket;
import net.marfgamer.raknet.protocol.message.EncapsulatedPacket;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.server.RakNetServerListener;
import net.marfgamer.raknet.session.RakNetClientSession;
import net.marfgamer.raknet.session.RakNetServerSession;

public class SplitPacketTest {

	private static final int MARFGAMER_DEVELOPMENT_PORT = 30851;
	private static final short SPLIT_START_ID = 0xFE;
	private static final short SPLIT_END_ID = 0xFF;

	private static long startSend = -1;

	public static void main(String[] args) throws RakNetException, UnknownHostException, InterruptedException {
		System.out.println("Creating server...");
		createServer();
		
		System.out.println("Sleeping 5000MS...");
		Thread.sleep(5000);

		System.out.println("Creating client...");
		createClient();
	}

	private static RakNetServer createServer() throws RakNetException {
		RakNetServer server = new RakNetServer(MARFGAMER_DEVELOPMENT_PORT, 1);

		// Client connected
		server.setListener(new RakNetServerListener() {

			@Override
			public void onClientConnect(RakNetClientSession session) {
				System.out.println("Server: Client connected from " + session.getAddress() + "!");
			}

			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				System.out.println("Server: Client from " + session.getAddress() + " disconnected! (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
				System.out.println("Server: Received packet of " + packet.size() + " bytes from " + session.getAddress()
						+ ", checking data...");

				// Check packet ID
				System.out.println("Server: Checking header byte...");
				if (packet.getId() != SPLIT_START_ID) {
					System.err.println("Server: Packet header is " + packet.getId() + " when it should be "
							+ SPLIT_START_ID + "!");
					System.exit(1);
				}

				// Check shorts
				System.out.println("Server: Checking if data is sequenced correctly...");
				int lastShort = -1;
				while (packet.remaining() >= 2) {
					int currentShort = packet.readUShort();
					if (currentShort - lastShort != 1) {
						System.err.println("Server: Short data was not split correctly!");
						System.exit(1);
					} else {
						lastShort = currentShort;
					}
				}

				// Check packet footer
				System.out.println("Server: Checking footer byte...");
				if (packet.readUByte() != SPLIT_END_ID) {
					System.err.println("Server: Packet footer is " + packet.getId() + " when it should be "
							+ SPLIT_START_ID + "!");
					System.exit(1);
				}

				System.out.println(
						"Server: Split packet test passed! (Took " + (System.currentTimeMillis() - startSend) + "MS)");
				System.exit(0);
			}
			
			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
			}

		});

		server.startThreaded();
		return server;
	}

	private static RakNetClient createClient() throws RakNetException, UnknownHostException {
		// Create client and add hooks
		RakNetClient client = new RakNetClient();

		// Server connected
		client.setListener(new RakNetClientListener() {

			@Override
			public void onConnect(RakNetServerSession session) {
				System.out.println("Client: Connected to server with MTU " + session.getMaximumTransferUnit());

				// Send huge packet of doom
				RakNetPacket packet = new RakNetPacket(SPLIT_START_ID);
				int mtuTest = (session.getMaximumTransferUnit() - CustomPacket.calculateDummy() - EncapsulatedPacket.calculateDummy(Reliability.RELIABLE_ORDERED, false)) * 4;//(session.getMaximumTransferUnit() - CustomPacket.calculateDummy() - EncapsulatedPacket.calculateDummy(Reliability.RELIABLE_ORDERED, false)) * RakNet.MAX_SPLIT_COUNT;
				for (int i = 0; i <  (mtuTest - 2)
						/ 2; i++) { // Subtract 1 for start and end ID
					packet.writeUShort(i);
				}
				packet.writeUByte(SPLIT_END_ID);

				System.out.println("Client: Sending giant packet... (" + packet.size() + " bytes)");
				session.sendMessage(Reliability.RELIABLE_ORDERED, packet);
				startSend = System.currentTimeMillis();
			}

			@Override
			public void onDisconnect(RakNetServerSession session, String reason) {
				System.out.println("Client: Lost connection to server! (" + reason + ")");
			}
			
			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
			}

		});

		// Connect to server
		client.connectThreaded(InetAddress.getLocalHost(), MARFGAMER_DEVELOPMENT_PORT);
		return client;
	}

}
