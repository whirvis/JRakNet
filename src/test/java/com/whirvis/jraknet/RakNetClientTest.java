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
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.peer.RakNetServerPeer;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;

/**
 * Tests the {@link RakNetClient} by connecting to the LifeBoat Survival Games
 * server.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class RakNetClientTest {

	private static final Logger LOG = LogManager.getLogger(RakNetClientTest.class);

	private RakNetClientTest() {
		// Static class
	}

	/**
	 * The entry point for the test.
	 * 
	 * @param args
	 *            the program arguments. These values are ignored.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 */
	public static void main(String[] args) throws RakNetException {
		RakNetClient client = new RakNetClient();
		client.addListener(new RakNetClientListener() {

			@Override
			public void onConnect(RakNetClient client, InetSocketAddress address, ConnectionType connectionType) {
				LOG.info("Connected to " + connectionType.getName() + " server with address " + address);
			}

			@Override
			public void onLogin(RakNetClient client, RakNetServerPeer peer) {
				LOG.info("Logged into server");
				client.disconnect();
			}

			@Override
			public void onDisconnect(RakNetClient client, InetSocketAddress address, RakNetServerPeer peer, String reason) {
				LOG.info("Disconnected from server");
			}

			@Override
			public void onAcknowledge(RakNetClient client, RakNetServerPeer peer, Record record, EncapsulatedPacket packet) {
				LOG.info(peer.getConnectionType().getName() + " server has acknowledged " + RakNetPacket.getName(packet.payload.readUnsignedByte()) + " packet");
			}

			@Override
			public void onLoss(RakNetClient client, RakNetServerPeer peer, Record record, EncapsulatedPacket packet) {
				LOG.info(peer.getConnectionType().getName() + " server has not lost " + RakNetPacket.getName(packet.payload.readUnsignedByte()) + " packet");
			}

			@Override
			public void onHandlerException(RakNetClient client, InetSocketAddress address, Throwable cause) {
				LOG.error("Exception caused by " + address, cause);
			}

			@Override
			public void onPeerException(RakNetClient client, RakNetServerPeer peer, Throwable cause) {
				LOG.error("Peer with address " + peer.getAddress() + " threw exception", cause);
			}

		});
		LOG.info("Created client, connecting to " + RakNetTest.LIFEBOAT_SURVIVAL_GAMES_ADDRESS + "...");
		client.connect(RakNetTest.LIFEBOAT_SURVIVAL_GAMES_ADDRESS);
	}

}
