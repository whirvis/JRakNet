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
package com.whirvis.jraknet.example;

import java.net.UnknownHostException;

import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.session.RakNetServerSession;

/**
 * A simple <code>RakNetClient</code> that connects to the LifeBoat Survival
 * Games server, when it is connected the client disconnects and shuts down.
 * 
 * @author Trent Summerlin
 */
public class RakNetClientExample {

	public static void main(String[] args) throws RakNetException, UnknownHostException {
		// Server address and port
		String SERVER_ADDRESS = "sg.lbsg.net";
		int SERVER_PORT = 19132;

		// Create client
		RakNetClient client = new RakNetClient();

		// Add listener
		client.addListener(new RakNetClientListener() {

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
