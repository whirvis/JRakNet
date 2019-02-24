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
package com.whirvis.jraknet.interactive;

import java.net.InetSocketAddress;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.whirvis.jraknet.RakNetTest;
import com.whirvis.jraknet.discovery.DiscoveredServer;
import com.whirvis.jraknet.discovery.Discovery;
import com.whirvis.jraknet.discovery.DiscoveryListener;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.identifier.MinecraftIdentifier;

/**
 * Used to test the broadcast feature in <code>RakNetClient</code>.
 *
 * @author Whirvis T. Wheatley
 */
public class BroadcastTest {

	private final HashMap<InetSocketAddress, MinecraftIdentifier> discovered;
	private final BroadcastFrame frame;

	public BroadcastTest() {
		Discovery.addPort(RakNetTest.MINECRAFT_DEFAULT_PORT);
		this.discovered = new HashMap<InetSocketAddress, MinecraftIdentifier>();
		this.frame = new BroadcastFrame();
	}

	/**
	 * The class used to listen for server discovery updates.
	 *
	 * @author Whirvis T. Wheatley
	 */
	private class ServerDiscoveryListener implements DiscoveryListener {

		@Override
		public void onServerDiscovered(DiscoveredServer server) {
			if (MinecraftIdentifier.isMinecraftIdentifier(server.getIdentifier())) {
				discovered.put(server.getAddress(), new MinecraftIdentifier(server.getIdentifier()));
			}
			frame.updatePaneText(discovered.values().toArray(new MinecraftIdentifier[discovered.size()]));
		}

		@Override
		public void onServerIdentifierUpdate(DiscoveredServer server, Identifier oldIdentifier) {
			if (MinecraftIdentifier.isMinecraftIdentifier(server.getIdentifier())) {
				discovered.put(server.getAddress(), new MinecraftIdentifier(server.getIdentifier()));
				frame.updatePaneText(discovered.values().toArray(new MinecraftIdentifier[discovered.size()]));
			} else {
				discovered.remove(server.getAddress());
			}
		}

		@Override
		public void onServerForgotten(DiscoveredServer server) {
			discovered.remove(server.getAddress());
			frame.updatePaneText(discovered.values().toArray(new MinecraftIdentifier[discovered.size()]));
		}

	}

	/**
	 * Starts the test.
	 */
	public void start() {
		Discovery.addListener(new ServerDiscoveryListener());
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
