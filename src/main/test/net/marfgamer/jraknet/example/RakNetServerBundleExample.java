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

import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.identifier.MCPEIdentifier;
import net.marfgamer.jraknet.server.RakNetServer;
import net.marfgamer.jraknet.session.RakNetClientSession;

/**
 * A simple <code>RakNetServer</code> that is extending
 * <code>RakNetServer</code> and can be tested through a Minecraft: Pocket
 * Edition client using the local multiplayer features built into the game
 *
 * @author MarfGamer
 */
public class RakNetServerBundleExample extends RakNetServer {

	public RakNetServerBundleExample() {
		super(19132, 10, new MCPEIdentifier("JRakNet Example Server", 91, "0.16.2", 0, 10, System.currentTimeMillis(),
				"New World", "Survival"));
	}

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
	public void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
		System.out.println("Client from address " + session.getAddress() + " sent packet with ID 0x"
				+ Integer.toHexString(packet.getId()).toUpperCase() + " on channel " + channel);
	}

	public static void main(String[] args) {
		// Create server
		RakNetServerBundleExample bundle = new RakNetServerBundleExample();

		// Start server
		bundle.start();
	}

}
