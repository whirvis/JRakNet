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
package com.whirvis.jraknet.example;

import java.util.Random;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.identifier.MinecraftIdentifier;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import com.whirvis.jraknet.session.RakNetClientSession;

/**
 * A simple <code>RakNetServer</code> that can be tested through a Minecraft
 * client using the local multiplayer features built into the game.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class RakNetServerExample {

	public static void main(String[] args) throws RakNetException {
		// Create server
		RakNetServer server = new RakNetServer(19132, 10,
				new MinecraftIdentifier("JRakNet Example Server", 137, "1.2.0", 0, 10,
						new Random().nextLong() /* Server broadcast ID */, "New World", "Survival"));

		// Add listener
		server.addListener(new RakNetServerListener() {

			// Client connected
			@Override
			public void onClientConnect(RakNetClientSession session) {
				System.out.println("Client from address " + session.getAddress() + " has connected to the server");
			}

			// Client disconnected
			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				System.out.println("Client from address " + session.getAddress()
						+ " has disconnected from the server for the reason \"" + reason + "\"");
			}

			// Packet received
			@Override
			public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
				System.out.println("Client from address " + session.getAddress() + " sent packet with ID "
						+ RakNet.toHexStringId(packet) + " on channel " + channel);
			}

		});

		// Start server
		server.start();
	}

}
