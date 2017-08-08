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
 * Copyright (c) 2016, 2017 Trent "MarfGamer" Summerlin
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
package net.marfgamer.jraknet.interactive;

import java.net.InetSocketAddress;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetLogger;
import net.marfgamer.jraknet.UtilityTest;
import net.marfgamer.jraknet.client.RakNetClient;
import net.marfgamer.jraknet.client.RakNetClientListener;
import net.marfgamer.jraknet.client.discovery.DiscoveryMode;
import net.marfgamer.jraknet.identifier.Identifier;
import net.marfgamer.jraknet.identifier.MCPEIdentifier;

/**
 * Used to test the broadcast feature in <code>RakNetClient</code>.
 *
 * @author Trent "MarfGamer" Summerlin
 */
public class BroadcastTest {

	private final RakNetClient client;
	private final HashMap<InetSocketAddress, MCPEIdentifier> discovered;
	private final BroadcastFrame frame;

	public BroadcastTest() {
		this.client = new RakNetClient(DiscoveryMode.ALL_CONNECTIONS,
				UtilityTest.MINECRAFT_POCKET_EDITION_DEFAULT_PORT);
		this.discovered = new HashMap<InetSocketAddress, MCPEIdentifier>();
		this.frame = new BroadcastFrame(client);
	}

	/**
	 * The class used to listen for server discovery updates.
	 *
	 * @author Trent "MarfGamer" Summerlin
	 */
	private class ServerDiscoveryListener implements RakNetClientListener {

		@Override
		public void onServerDiscovered(InetSocketAddress address, Identifier identifier) {
			if (MCPEIdentifier.isMCPEIdentifier(identifier)) {
				discovered.put(address, new MCPEIdentifier(identifier));
			}
			frame.updatePaneText(discovered.values().toArray(new MCPEIdentifier[discovered.size()]));
		}

		@Override
		public void onServerIdentifierUpdate(InetSocketAddress address, Identifier identifier) {
			if (MCPEIdentifier.isMCPEIdentifier(identifier)) {
				discovered.put(address, new MCPEIdentifier(identifier));
			}
			frame.updatePaneText(discovered.values().toArray(new MCPEIdentifier[discovered.size()]));
		}

		@Override
		public void onServerForgotten(InetSocketAddress address) {
			discovered.remove(address);
			frame.updatePaneText(discovered.values().toArray(new MCPEIdentifier[discovered.size()]));
		}

	}

	/**
	 * Starts the test.
	 */
	public void start() {
		// Enable logging
		RakNet.enableLogging(RakNetLogger.LEVEL_INFO);

		// Set client options
		client.setListener(new ServerDiscoveryListener());

		// Create window
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		RakNet.enableLogging();
		BroadcastTest test = new BroadcastTest();
		test.start();
	}

}
