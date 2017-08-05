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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.marfgamer.jraknet.client.RakNetClient;
import net.marfgamer.jraknet.client.RakNetClientListener;
import net.marfgamer.jraknet.protocol.Reliability;
import net.marfgamer.jraknet.protocol.message.CustomPacket;
import net.marfgamer.jraknet.protocol.message.EncapsulatedPacket;
import net.marfgamer.jraknet.server.BlockedAddress;
import net.marfgamer.jraknet.server.RakNetServer;
import net.marfgamer.jraknet.server.RakNetServerListener;
import net.marfgamer.jraknet.session.RakNetClientSession;
import net.marfgamer.jraknet.session.RakNetServerSession;
import net.marfgamer.jraknet.util.RakNetUtils;

/**
 * Used to test the split packet feature of <code>RakNetSession</code> through a
 * stress test by sending a packet as big as possible (Average packet size is
 * over 146,000 bytes!).
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class SplitPacketTest {

	// Logger name
	private static final String LOGGER_NAME = "split packet test";

	// Test data
	private static final short SPLIT_START_ID = 0xFE;
	private static final short SPLIT_END_ID = 0xFF;
	private static long startSend = -1;

	public static void main(String[] args) throws RakNetException, UnknownHostException {
		// Enable logging
		RakNet.enableLogging();

		RakNetLogger.info(LOGGER_NAME, "Creating server...");
		createServer();

		RakNetLogger.info(LOGGER_NAME, "Sleeping 3000MS");
		RakNetUtils.threadLock(3000L);

		RakNetLogger.info(LOGGER_NAME, "Creating client...");
		createClient();
	}

	/**
	 * @return the server that will receive the giant packet.
	 * @throws RakNetException
	 *             if any problems occur during the stress test.
	 */
	private static RakNetServer createServer() throws RakNetException {
		RakNetServer server = new RakNetServer(UtilityTest.MARFGAMER_DEVELOPMENT_PORT, 1);

		// Client connected
		server.setListener(new RakNetServerListener() {

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

				RakNetLogger.info(LOGGER_NAME, "Server: Client connected from " + session.getAddress() + "!");
			}

			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				RakNetLogger.info(LOGGER_NAME,
						"Server: Client from " + session.getAddress() + " disconnected! (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
				RakNetLogger.info(LOGGER_NAME, "Server: Received packet of " + packet.size() + " bytes from "
						+ session.getAddress() + ", checking data...");

				// Check packet ID
				RakNetLogger.info(LOGGER_NAME, "Server: Checking header byte...");
				if (packet.getId() != SPLIT_START_ID) {
					RakNetLogger.error(LOGGER_NAME, "Server: Packet header is " + packet.getId() + " when it should be "
							+ SPLIT_START_ID + "!");
					System.exit(1);
				}

				// Check shorts
				RakNetLogger.info(LOGGER_NAME, "Server: Checking if data is sequenced correctly...");
				StringBuilder sequencedIntegers = new StringBuilder();
				long lastInt = -1;
				while (packet.remaining() >= 4) {
					long currentInt = packet.readUInt();
					if (currentInt - lastInt != 1) {
						RakNetLogger.error(LOGGER_NAME, "Server: Short data was not split correctly!");
						System.exit(1);
					} else {
						lastInt = currentInt;
						sequencedIntegers.append(lastInt + (packet.remaining() >= 4 ? ", " : "\n"));
					}
				}
				RakNetLogger.info(LOGGER_NAME, sequencedIntegers.toString());

				// Check packet footer
				RakNetLogger.info(LOGGER_NAME, "Server: Checking footer byte...");
				if (packet.readUByte() != SPLIT_END_ID) {
					RakNetLogger.error(LOGGER_NAME, "Server: Packet footer is " + packet.getId() + " when it should be "
							+ SPLIT_START_ID + "!");
					System.exit(1);
				}

				RakNetLogger.info(LOGGER_NAME,
						"Server: Split packet test passed! (Took " + (System.currentTimeMillis() - startSend) + "MS)");
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

		// Server connected
		client.setListener(new RakNetClientListener() {

			private Packet packet;

			@Override
			public void onConnect(RakNetServerSession session) {
				RakNetLogger.info(LOGGER_NAME,
						"Client: Connected to server with MTU " + session.getMaximumTransferUnit());

				// Calculate maximum packet size
				this.packet = new RakNetPacket(SPLIT_START_ID);
				int maximumPacketSize = (session.getMaximumTransferUnit() - CustomPacket.calculateDummy()
						- EncapsulatedPacket.calculateDummy(Reliability.RELIABLE_ORDERED, false))
						* RakNet.MAX_SPLIT_COUNT;

				// Fill up packet
				int integersWritten = 0;
				for (int i = 0; i < (maximumPacketSize - 2) / 4; i++) {
					packet.writeUInt(i);
					integersWritten++;
				}
				packet.writeUByte(SPLIT_END_ID);

				// Send packet
				RakNetLogger.info(LOGGER_NAME,
						"Client: Sending giant packet... (" + packet.size() + " bytes, " + integersWritten + " ints)");
				startSend = System.currentTimeMillis();
				session.sendMessage(Reliability.RELIABLE_ORDERED, packet);
			}

			@Override
			public void onDisconnect(RakNetServerSession session, String reason) {
				RakNetLogger.error(LOGGER_NAME, "Client: Lost connection to server! (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
				System.exit(1);
			}

		});

		// Connect to server
		client.connectThreaded(InetAddress.getLocalHost(), UtilityTest.MARFGAMER_DEVELOPMENT_PORT);
		return client;
	}

}
