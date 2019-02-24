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
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.protocol.MessageIdentifier;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;
import com.whirvis.jraknet.session.RakNetServerSession;

/**
 * Used to test <code>RakNetClient</code> by connecting to the LifeBoat Survival
 * Games server, sending a dummy packet to make sure the ACK/NACK functions
 * work, and then disconnecting afterwards.
 *
 * @author Whirvis T. Wheatley
 */
public class RakNetClientTest {

	private static final Logger LOG = LogManager.getLogger(RakNetClientTest.class);

	static boolean s = false;
	
	public static void main(String[] args) throws IllegalStateException, RakNetException, UnknownHostException {
		// Create client and add listener
		RakNetClient client = new RakNetClient();
		client.addListener(new RakNetClientListener() {

			@Override
			public void onConnect(RakNetClient client, RakNetServerSession session) {
				LOG.info("Connected to " + session.getConnectionType().getName() + " server with address "
						+ session.getAddress() + "!");
				client.disconnect();
				if(s == false) {
					try {
						client.connect(RakNetTest.LIFEBOAT_SURVIVAL_GAMES_ADDRESS);
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (RakNetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					s = true;
				}
			}

			@Override
			public void onAcknowledge(RakNetClient client, RakNetServerSession session, Record record,
					EncapsulatedPacket packet) {
				LOG.info(session.getConnectionType().getName() + " server has acknowledged packet with ID: "
						+ MessageIdentifier.getName(packet.payload.readUnsignedByte()));
			}

			@Override
			public void onNotAcknowledge(RakNetClient client, RakNetServerSession session, Record record,
					EncapsulatedPacket packet) {
				LOG.info(session.getConnectionType().getName() + " server has not acknowledged packet with ID: "
						+ MessageIdentifier.getName(packet.payload.readUnsignedByte()));
			}

			@Override
			public void onHandlerException(RakNetClient client, InetSocketAddress address, Throwable cause) {
				LOG.error("Exception caused by " + address);
				cause.printStackTrace();
			}

		});
		LOG.info("Created client, connecting to " + RakNetTest.LIFEBOAT_SURVIVAL_GAMES_ADDRESS + "...");

		// Connect to server
		client.connect(RakNetTest.LIFEBOAT_SURVIVAL_GAMES_ADDRESS);
	}

}
