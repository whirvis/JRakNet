/*
 *       _   _____            _      _   _          _
 *      | | |  __ \          | |    | \ | |        | |
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.whirvis.jraknet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.server.BlockedAddress;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import com.whirvis.jraknet.session.RakNetClientSession;
import com.whirvis.jraknet.session.RakNetServerSession;

/**
 * Used to test the sequenced packet feature of <code>RakNetSession</code>
 * through a stress test by sending 1000 packets and seeing if any were lost
 * (Completely lost or received before the earlier one could be handled, as the
 * reliability used in this test is <code>UNRELIABLE_SEQUENCED</code>).
 * 
 * <br>
 * <br>
 * 
 * This test was created in a response to GitHub issue #35 after it was
 * discovered during beta testing of Trent's remake of Five Night's at Freddy's
 * 3 to Java in its spectator mode inside multiplayer mode.
 *
 * @author Trent "Whirvis" Summerlin
 */

public class SequencedPacketTest {

	private static final Logger log = LogManager.getLogger(SequencedPacketTest.class);

	// Test data
	private static final short SEQUENCE_START_ID = 0xFE;
	private static final int PACKET_SEND_COUNT = 1000;
	private static long startSend = -1;
	private static int packetReceiveCount = 0;
	private static boolean[] packetsReceived = new boolean[PACKET_SEND_COUNT];

	public static void main(String[] args) throws RakNetException, InterruptedException, UnknownHostException {

		log.info("Creating server...");
		createServer();

		log.info("Sleeping 3000MS");
		Thread.sleep(3000L);

		log.info("Creating client...");
		createClient();

		// In case of timeout
		long currentTime = System.currentTimeMillis();
		while (true) {
			if (currentTime - startSend >= 30000 && startSend > -1) {
				log.info("Failed to complete test due to timeout (Took over 30 seconds!), printing results...");
				printResults();
				System.exit(1);
			}
			currentTime = System.currentTimeMillis();

			try {
				Thread.sleep(0, 1); // Lower CPU usage
			} catch (InterruptedException e) {
				log.warn("Sequenced packet test sleep interrupted");
			}
		}
	}

	/**
	 * Prints the results of the test
	 */
	private static void printResults() {
		log.info("Server - Sequenced packet test finished, lost "
				+ (packetReceiveCount >= PACKET_SEND_COUNT ? "no"
						: Float.toString(
								((float) PACKET_SEND_COUNT - packetReceiveCount / (float) PACKET_SEND_COUNT) * 100)
								.substring(0, 3).replace(".", "") + "% of")
				+ " packets (Took " + (System.currentTimeMillis() - startSend) + "MS)");
		if (packetReceiveCount < PACKET_SEND_COUNT) {
			// Create list of lost packets
			ArrayList<Integer> packetsLost = new ArrayList<Integer>();
			for (int i = 0; i < packetsReceived.length; i++) {
				if (packetsReceived[i] == false) {
					packetsLost.add(i);
				}
			}

			// Print out said list
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < packetsLost.size(); i++) {
				Integer wi = packetsLost.get(i);
				builder.append(wi.intValue() + (i + 1 < packetsLost.size() ? ", " : ""));
			}
			log.info("Packet" + (packetsLost.size() == 1 ? "" : "s") + " lost: " + builder.toString());
		}
	}

	/**
	 * @return the server that will receive the sequenced packets.
	 * @throws RakNetException
	 *             if any problems occur during the stress test.
	 */
	private static RakNetServer createServer() throws RakNetException {
		RakNetServer server = new RakNetServer(RakNetTest.WHIRVIS_DEVELOPMENT_PORT, 1);

		// Add listener
		server.addListener(new RakNetServerListener() {

			@Override
			public void onClientConnect(RakNetClientSession session) {
				// Only accept the packet if it's from the same device
				try {
					if (!InetAddress.getLocalHost().equals(session.getAddress().getAddress())) {
						server.removeSession(session, "Session is not from local address");
						server.blockAddress(session.getInetAddress(), BlockedAddress.PERMANENT_BLOCK);
						return; // The sender is not from our address!
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}

				log.info("Server - Client connected from " + session.getAddress() + "!");
			}

			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				log.info("Server - Client from " + session.getAddress() + " disconnected! (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
				// Get packet index
				int packetIndex = packet.readInt();
				packetReceiveCount++;
				packetsReceived[packetIndex] = true;

				// Tell user how many packets were dropped
				if (packetIndex >= PACKET_SEND_COUNT - 1) {
					printResults();
					System.exit(0);
				}
			}

			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
				System.exit(1);
			}

		});

		server.startThreaded();
		return server;
	}

	/**
	 * @return the client that will be sending the sequenced packets.
	 * @throws RakNetException
	 *             if any problems occur during the stress test.
	 * @throws UnknownHostException
	 *             if the localhost address cannot be found.
	 */
	private static RakNetClient createClient() throws RakNetException, UnknownHostException {
		// Create client and add hooks
		RakNetClient client = new RakNetClient();

		// Add listener
		client.addListener(new RakNetClientListener() {

			int packetSize;

			@Override
			public void onConnect(RakNetServerSession session) {
				log.info("Client - Connected to server with MTU " + session.getMaximumTransferUnit());

				// Send 100 sequenced packets
				log.info("Client - Sending " + PACKET_SEND_COUNT + " packets...");
				startSend = System.currentTimeMillis();
				for (int i = 0; i < PACKET_SEND_COUNT; i++) {
					RakNetPacket sequencedPacket = new RakNetPacket(SEQUENCE_START_ID);
					sequencedPacket.writeInt(i);
					packetSize += sequencedPacket.size();
					session.sendMessage(Reliability.UNRELIABLE_SEQUENCED, sequencedPacket);
				}

				// Notify user
				log.info("Client - Sent " + PACKET_SEND_COUNT + " packets (" + packetSize + " bytes, "
						+ (packetSize / 4) + " ints)");
			}

			@Override
			public void onDisconnect(RakNetServerSession session, String reason) {
				log.info("Client - Lost connection to server! (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
				System.exit(1);
			}

		});

		// Connect to server
		client.connectThreaded(InetAddress.getLocalHost(), RakNetTest.WHIRVIS_DEVELOPMENT_PORT);
		return client;
	}

}
