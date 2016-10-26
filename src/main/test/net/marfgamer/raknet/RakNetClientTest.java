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
 * Copyright (c) 2016 MarfGamer
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
package net.marfgamer.raknet;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.client.RakNetClientListener;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.session.RakNetServerSession;

public class RakNetClientTest {

	private static final InetSocketAddress LIFEBOAT_SURVIVAL_GAMES_ADDRESS = new InetSocketAddress("sg.lbsg.net",
			19132);

	public static void main(String[] args) throws RakNetException {
		RakNetClient client = new RakNetClient();
		client.setListener(new RakNetClientListener() {

			@Override
			public void onConnect(RakNetServerSession session) {
				System.out.println("Connected to server with address " + session.getAddress() + "!");
				client.disconnect();
			}

			@Override
			public void onConnectionFailure(InetSocketAddress address, RakNetException cause) {
				System.out.println("Failed to connect to server with address " + address + " ["
						+ cause.getClass().getSimpleName() + "]");
			}

			@Override
			public void onDisconnect(RakNetServerSession session, String reason) {
				System.out.println("Disconnected from server with address " + session.getAddress() + " for reason \""
						+ reason + "\"");
			}

		});
		System.out.println("Created client, connecting to " + LIFEBOAT_SURVIVAL_GAMES_ADDRESS + "...");

		client.connect(LIFEBOAT_SURVIVAL_GAMES_ADDRESS);
	}

}
