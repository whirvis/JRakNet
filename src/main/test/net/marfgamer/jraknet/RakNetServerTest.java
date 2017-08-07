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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import net.marfgamer.jraknet.identifier.MCPEIdentifier;
import net.marfgamer.jraknet.protocol.login.NewIncomingConnection;
import net.marfgamer.jraknet.server.RakNetServer;
import net.marfgamer.jraknet.server.RakNetServerListener;
import net.marfgamer.jraknet.server.ServerPing;
import net.marfgamer.jraknet.session.RakNetClientSession;

/**
 * Used to test <code>RakNetServer</code> by starting a server on the default
 * Minecraft: Pocket Edition port.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class RakNetServerTest {

	// Logger name
	private static final String LOGGER_NAME = "server test";

	public static void main(String[] args) {
		// Enable logging
		RakNet.enableLogging(RakNetLogger.LEVEL_INFO);

		// Create server and set listener
		RakNetServer server = new RakNetServer(UtilityTest.MINECRAFT_POCKET_EDITION_DEFAULT_PORT, 10);
		server.setListener(new RakNetServerListener() {

			@Override
			public void onClientPreConnect(InetSocketAddress address) {
				RakNetLogger.info(LOGGER_NAME,
						"Client from " + address + " has instantiated the connection, waiting for "
								+ NewIncomingConnection.class.getSimpleName() + " packet");
			}

			@Override
			public void onClientPreDisconnect(InetSocketAddress address, String reason) {
				RakNetLogger.info(LOGGER_NAME,
						"Client from " + address + " has failed to login for \"" + reason + "\"");
			}

			@Override
			public void onClientConnect(RakNetClientSession session) {
				RakNetLogger.info(LOGGER_NAME, (session.isJRakNet() ? "JRakNet" : "Vanilla")
						+ " client from address " + session.getAddress() + " has connected to the server");
			}

			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				RakNetLogger.info(LOGGER_NAME,
						(session.isJRakNet() ? "JRakNet" : "Vanilla") + " client from address "
								+ session.getAddress() + " has been disconnected for \"" + reason + "\"");
			}

			@Override
			public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
				RakNetLogger.info(LOGGER_NAME,
						"Received packet from " + (session.isJRakNet() ? "JRakNet" : "Vanilla")
								+ " client with address " + session.getAddress() + " with packet ID 0x"
								+ Integer.toHexString(packet.getId()).toUpperCase() + " on channel " + channel);
			}

			@Override
			public void handlePing(ServerPing ping) {
				MCPEIdentifier identifier = new MCPEIdentifier("A JRakNet server test", 91, "0.16.2",
						server.getSessionCount(), server.getMaxConnections(), server.getGloballyUniqueId(), "New World",
						"Survival");
				ping.setIdentifier(identifier);
			}

			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				RakNetLogger.error(LOGGER_NAME, "Exception caused by " + address);
				cause.printStackTrace();
			}

			@Override
			public void onAddressBlocked(InetAddress address, String reason, long time) {
				RakNetLogger.info(LOGGER_NAME,
						"Blocked address " + address + " due to \"" + reason + "\" for " + (time / 1000L) + " seconds");
			}

			@Override
			public void onAddressUnblocked(InetAddress address) {
				RakNetLogger.info(LOGGER_NAME, "Unblocked address " + address);
			}

		});

		// Start server
		server.start();
	}

}