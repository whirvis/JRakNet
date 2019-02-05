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
 * Copyright (c) 2016-2019 Trent Summerlin
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
package com.whirvis.jraknet.interactive;

import java.net.InetSocketAddress;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.whirvis.jraknet.RakNetTest;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.client.discovery.DiscoveryMode;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.identifier.MinecraftIdentifier;

/**
 * Used to test the broadcast feature in <code>RakNetClient</code>.
 *
 * @author Trent Summerlin
 */
public class BroadcastTest {

	private final RakNetClient client;
	private final HashMap<InetSocketAddress, MinecraftIdentifier> discovered;
	private final BroadcastFrame frame;

	public BroadcastTest() {
		this.client = new RakNetClient(DiscoveryMode.ALL_CONNECTIONS, RakNetTest.MINECRAFT_DEFAULT_PORT);
		this.discovered = new HashMap<InetSocketAddress, MinecraftIdentifier>();
		this.frame = new BroadcastFrame(client);
	}

	/**
	 * The class used to listen for server discovery updates.
	 *
	 * @author Trent Summerlin
	 */
	private class ServerDiscoveryListener implements RakNetClientListener {

		@Override
		public void onServerDiscovered(InetSocketAddress address, Identifier identifier) {
			if (MinecraftIdentifier.isMinecraftIdentifier(identifier)) {
				discovered.put(address, new MinecraftIdentifier(identifier));
			}
			frame.updatePaneText(discovered.values().toArray(new MinecraftIdentifier[discovered.size()]));
		}

		@Override
		public void onServerIdentifierUpdate(InetSocketAddress address, Identifier identifier) {
			if (MinecraftIdentifier.isMinecraftIdentifier(identifier)) {
				discovered.put(address, new MinecraftIdentifier(identifier));
			}
			frame.updatePaneText(discovered.values().toArray(new MinecraftIdentifier[discovered.size()]));
		}

		@Override
		public void onServerForgotten(InetSocketAddress address) {
			discovered.remove(address);
			frame.updatePaneText(discovered.values().toArray(new MinecraftIdentifier[discovered.size()]));
		}

	}

	/**
	 * Starts the test.
	 */
	public void start() {

		// Set client options
		client.addListener(new ServerDiscoveryListener());

		// Create window
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		BroadcastTest test = new BroadcastTest();
		test.start();
	}

}
