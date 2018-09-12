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
 * Copyright (c) 2016-2018 Trent Summerlin
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Trent Summerlin
 */
public class RakNetClientTest {

	private static final Logger log = LoggerFactory.getLogger(RakNetClientTest.class);

	public static void main(String[] args) {

		// Create client and add listener
		RakNetClient client = new RakNetClient();
		client.addListener(new RakNetClientListener() {

			@Override
			public void onConnect(RakNetServerSession session) {
				log.info("Connected to " + session.getConnectionType().getName() + " server with address "
						+ session.getAddress() + "!");
				client.disconnectAndShutdown();
			}

			@Override
			public void onAcknowledge(RakNetServerSession session, Record record, EncapsulatedPacket packet) {
				log.info(session.getConnectionType().getName() + " server has acknowledged packet with ID: "
						+ MessageIdentifier.getName(packet.payload.readUnsignedByte()));
			}

			@Override
			public void onNotAcknowledge(RakNetServerSession session, Record record, EncapsulatedPacket packet) {
				log.info(session.getConnectionType().getName() + " server has not acknowledged packet with ID: "
						+ MessageIdentifier.getName(packet.payload.readUnsignedByte()));
			}

			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				log.error("Exception caused by " + address);
				cause.printStackTrace();
			}

		});
		log.info("Created client, connecting to " + RakNetTest.LIFEBOAT_SURVIVAL_GAMES_ADDRESS + "...");

		// Connect to server
		try {
			client.connect(RakNetTest.LIFEBOAT_SURVIVAL_GAMES_ADDRESS);
		} catch (RakNetException e) {
			e.printStackTrace();
			client.disconnectAndShutdown(e.getClass().getName() + ": " + e.getMessage());
		}
	}

}
