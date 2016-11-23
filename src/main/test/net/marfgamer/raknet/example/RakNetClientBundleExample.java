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
package net.marfgamer.raknet.example;

import java.net.UnknownHostException;

import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.session.RakNetServerSession;

/**
 * A simple <code>RakNetClient</code> that is extending
 * <code>RakNetClient</code> in order to connect to the LifeBoat Survival Games
 * server, when it is connected the client disconnects and shuts down
 * 
 * @author MarfGamer
 */
public class RakNetClientBundleExample extends RakNetClient {

	// Server address and port
	private static final String SERVER_ADDRESS = "sg.lbsg.net";
	private static final int SERVER_PORT = 19132;

	// Server connected
	@Override
	public void onConnect(RakNetServerSession session) {
		System.out.println("Successfully connected to server with address " + session.getAddress());
		this.disconnect();
	}

	// Server disconnected
	@Override
	public void onDisconnect(RakNetServerSession session, String reason) {
		System.out.println("Sucessfully disconnected from server with address " + session.getAddress()
				+ " for the reason \"" + reason + "\"");
		System.exit(0);
	}

	public static void main(String[] args) throws RakNetException, UnknownHostException {
		// Create client
		RakNetClientBundleExample bundle = new RakNetClientBundleExample();

		// Connect to server
		bundle.connect(SERVER_ADDRESS, SERVER_PORT);
	}

}
