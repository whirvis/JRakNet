/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2018 Trent Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.message.CustomPacket;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.server.BlockedAddress;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import com.whirvis.jraknet.session.RakNetClientSession;
import com.whirvis.jraknet.session.RakNetServerSession;

/**
 * Used to test the split packet feature of <code>RakNetSession</code> through a
 * stress test by sending a packet as big as possible (Average packet size is
 * over 146,000 bytes!).
 *
 * @author Trent Summerlin
 */
public class SplitPacketTest {

	private static final Logger log = LoggerFactory.getLogger(SplitPacketTest.class);

	// Test data
	private static final short SPLIT_START_ID = 0xFE;
	private static final short SPLIT_END_ID = 0xFF;
	private static long startSend = -1;

	public static void main(String[] args) throws RakNetException, InterruptedException, UnknownHostException {

		log.info("Creating server...");
		createServer();

		log.info("Sleeping 3000MS");
		Thread.sleep(3000L);

		log.info("Creating client...");
		createClient();
	}

	/**
	 * @return the server that will receive the giant packet.
	 * @throws RakNetException
	 *             if any problems occur during the stress test.
	 */
	private static RakNetServer createServer() throws RakNetException {
		RakNetServer server = new RakNetServer(UtilityTest.WHIRVIS_DEVELOPMENT_PORT, 1);

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

				log.info("Server: Client connected from " + session.getAddress() + "!");
			}

			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				log.info("Server: Client from " + session.getAddress() + " disconnected! (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
				log.info("Server: Received packet of " + packet.size() + " bytes from " + session.getAddress()
						+ ", checking data...");

				// Check packet ID
				log.info("Server: Checking header byte...");
				if (packet.getId() != SPLIT_START_ID) {
					log.error("Server: Packet header is " + packet.getId() + " when it should be " + SPLIT_START_ID
							+ "!");
					System.exit(1);
				}

				// Check shorts
				log.info("Server: Checking if data is sequenced correctly...");
				long lastInt = -1;
				while (packet.remaining() >= 4) {
					long currentInt = packet.readUnsignedInt();
					if (currentInt - lastInt != 1) {
						log.error("Server: Short data was not split correctly!");
						System.exit(1);
					} else {
						lastInt = currentInt;
					}
				}

				// Check packet footer
				log.info("Server: Checking footer byte...");
				if (packet.readUnsignedByte() != SPLIT_END_ID) {
					log.error("Server: Packet footer is " + packet.getId() + " when it should be " + SPLIT_START_ID
							+ "!");
					System.exit(1);
				}

				log.info("Server: Split packet test passed! (Took " + (System.currentTimeMillis() - startSend) + "MS)");
				System.exit(0);
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
	 * @return the client that will be sending the giant packet.
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

			private Packet packet;

			@Override
			public void onConnect(RakNetServerSession session) {
				log.info("Client: Connected to server with MTU " + session.getMaximumTransferUnit());

				// Calculate maximum packet size
				this.packet = new RakNetPacket(SPLIT_START_ID);
				int maximumPacketSize = (session.getMaximumTransferUnit() - CustomPacket.calculateDummy()
						- EncapsulatedPacket.calculateDummy(Reliability.RELIABLE_ORDERED, false))
						* RakNet.MAX_SPLIT_COUNT;

				// Fill up packet
				int integersWritten = 0;
				for (int i = 0; i < (maximumPacketSize - 2) / 4; i++) {
					packet.writeUnsignedInt(i);
					integersWritten++;
				}
				packet.writeUnsignedByte(SPLIT_END_ID);

				// Send packet
				log.info("Client: Sending giant packet... (" + packet.size() + " bytes, " + integersWritten + " ints)");
				startSend = System.currentTimeMillis();
				session.sendMessage(Reliability.RELIABLE_ORDERED, packet);
			}

			@Override
			public void onDisconnect(RakNetServerSession session, String reason) {
				log.error("Client: Lost connection to server! (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
				System.exit(1);
			}

		});

		// Connect to server
		client.connectThreaded(InetAddress.getLocalHost(), UtilityTest.WHIRVIS_DEVELOPMENT_PORT);
		return client;
	}

}
