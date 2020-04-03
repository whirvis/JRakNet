/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 "Whirvis" Trent Summerlin
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

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.peer.RakNetPeer;
import com.whirvis.jraknet.peer.RakNetServerPeer;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.message.CustomPacket;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.server.BlockedAddress;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;

/**
 * Test the split packet feature of the
 * {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer} through a stress test.
 * <p>
 * This stress test sends the biggest packet possible over the
 * {@link Reliability#RELIABLE_ORDERED RELIABLE_ORDERED} reliability. The
 * average packet size when performing this test is 146,000 bytes.
 *
 * @author "Whirvis" Trent Summerlin
 * @since JRakNet v2.0.0
 */
public final class SplitPacketTest {

	private static final Logger LOG = LogManager.getLogger(SplitPacketTest.class);
	private static final short SPLIT_START_ID = 0xFE;
	private static final short SPLIT_END_ID = 0xFF;
	private static long startSend = -1;

	private SplitPacketTest() {
		// Static class
	}

	/**
	 * The entry point for the test.
	 * 
	 * @param args
	 *            the program arguments. These values are ignored.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @throws InterruptedException
	 *             if any thread has interrupted the current thread. The
	 *             <i>interrupted status</i> of the current thread is cleared
	 *             when this exception is thrown.
	 * @throws UnknownHostException
	 *             if the <code>localhost</code> address could not be found.
	 */
	public static void main(String[] args) throws RakNetException, InterruptedException, UnknownHostException {
		LOG.info("Creating server, sleeping 3000MS, and then creating the client...");
		createServer();
		RakNet.sleep(3000L);
		createClient();

		// Wait for either a result or for a timeout
		long currentTime = System.currentTimeMillis();
		while (true) {
			Thread.sleep(0, 1); // Lower CPU usage
			if (currentTime - startSend >= 30000 && startSend > -1) {
				LOG.info("Failed to complete test due to timeout (Took over 30 seconds!), printing results...");
				System.exit(1);
			}
			currentTime = System.currentTimeMillis();
		}
	}

	/**
	 * Creates the server for the test.
	 * 
	 * @return the server that will receive the giant packet.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 */
	private static RakNetServer createServer() throws RakNetException {
		RakNetServer server = new RakNetServer(RakNetTest.WHIRVIS_DEVELOPMENT_PORT, 1);
		server.addListener(new RakNetServerListener() {

			@Override
			public void onLogin(RakNetServer server, RakNetClientPeer peer) {
				if (RakNet.isLocalAddress(peer.getAddress())) {
					LOG.info("Server - Client logged in from " + peer.getAddress());
				} else {
					server.disconnect(peer, "Session is not from local address");
					server.blockAddress(peer.getInetAddress(), BlockedAddress.PERMANENT_BLOCK);
				}
			}

			@Override
			public void onDisconnect(RakNetServer server, InetSocketAddress address, RakNetClientPeer peer,
					String reason) {
				LOG.info("Server - Client from " + address + " disconnected (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
				LOG.info("Server - Received packet of " + packet.size() + " bytes from " + peer.getAddress()
						+ ", checking data...");

				// Check header byte
				LOG.info("Server - Checking header byte...");
				if (packet.getId() != SPLIT_START_ID) {
					LOG.error("Server - Packet header is " + packet.getId() + " when it should be " + SPLIT_START_ID);
					System.exit(1);
				}

				// Check sequencing
				LOG.info("Server - Checking if data is sequenced correctly...");
				long lastInt = -1;
				while (packet.remaining() >= 4) {
					long currentInt = packet.readUnsignedInt();
					if (currentInt - lastInt != 1) {
						LOG.error("Server - Short data was not split correctly");
						System.exit(1);
					} else {
						lastInt = currentInt;
					}
				}

				// Check footer byte
				LOG.info("Server - Checking footer byte...");
				if (packet.readUnsignedByte() != SPLIT_END_ID) {
					LOG.error("Server - Packet footer is " + packet.getId() + " when it should be " + SPLIT_START_ID);
					System.exit(1);
				}
				LOG.info("Server - Split packet test passed (Took " + (System.currentTimeMillis() - startSend) + "MS)");
				System.exit(0);
			}

			@Override
			public void onHandlerException(RakNetServer server, InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
				System.exit(1);
			}

		});
		server.start();
		return server;
	}

	/**
	 * Creates the client for the test.
	 * 
	 * @return the client that will be sending the giant packet.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @throws UnknownHostException
	 *             if the <code>localhost</code> address cannot be found.
	 */
	private static RakNetClient createClient() throws RakNetException, UnknownHostException {
		RakNetClient client = new RakNetClient();
		client.addListener(new RakNetClientListener() {

			@Override
			public void onLogin(RakNetClient client, RakNetServerPeer peer) {
				LOG.info("Client - Logged in to server with MTU " + peer.getMaximumTransferUnit()
						+ ", calculating maximum packet size...");
				Packet packet = new RakNetPacket(SPLIT_START_ID);
				int maximumPacketSize = (peer.getMaximumTransferUnit() - CustomPacket.MINIMUM_SIZE
						- EncapsulatedPacket.size(Reliability.RELIABLE_ORDERED, true)) * RakNetPeer.MAX_SPLIT_COUNT;

				// Create giant packet
				LOG.info("Client - Creating giant packet...");
				int intsWritten = 0;
				for (int i = 0; i < (maximumPacketSize - 2) / 4; i++) {
					packet.writeUnsignedInt(i);
					intsWritten++;
				}
				packet.writeUnsignedByte(SPLIT_END_ID);

				// Send giant packet
				LOG.info("Client - Sending giant packet... (" + packet.size() + " bytes, " + intsWritten + " ints)");
				startSend = System.currentTimeMillis();
				peer.sendMessage(Reliability.RELIABLE_ORDERED, packet);
			}

			@Override
			public void onDisconnect(RakNetClient client, InetSocketAddress address, RakNetServerPeer peer,
					String reason) {
				LOG.error("Client - Lost connection to server (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void onHandlerException(RakNetClient client, InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
				System.exit(1);
			}

		});
		client.connect("localhost", RakNetTest.WHIRVIS_DEVELOPMENT_PORT);
		return client;
	}

}
