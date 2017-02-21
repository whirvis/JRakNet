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
 * Copyright (c) 2016, 2017 MarfGamer
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
package net.marfgamer.jraknet.example;

import java.net.UnknownHostException;

import net.marfgamer.jraknet.RakNetException;
import net.marfgamer.jraknet.client.RakNetClient;
import net.marfgamer.jraknet.client.RakNetClientListener;
import net.marfgamer.jraknet.session.RakNetServerSession;

/**
 * A simple <code>RakNetClient</code> that connects to the LifeBoat Survival
 * Games server, when it is connected the client disconnects and shuts down
 * 
 * @author MarfGamer
 */
public class RakNetClientExample {

	// Server address and port
	private static final String SERVER_ADDRESS = "sg.lbsg.net";
	private static final int SERVER_PORT = 19132;

	public static void main(String[] args) throws RakNetException, UnknownHostException {
		// Create client
		RakNetClient client = new RakNetClient();

		// Set listener
		client.setListener(new RakNetClientListener() {

			// Server connected
			@Override
			public void onConnect(RakNetServerSession session) {
				System.out.println("Successfully connected to server with address " + session.getAddress());
				client.disconnect();
			}

			// Server disconnected
			@Override
			public void onDisconnect(RakNetServerSession session, String reason) {
				System.out.println("Sucessfully disconnected from server with address " + session.getAddress()
						+ " for the reason \"" + reason + "\"");
				client.shutdown();
			}

		});

		// Connect to server
		client.connect(SERVER_ADDRESS, SERVER_PORT);
	}

}
