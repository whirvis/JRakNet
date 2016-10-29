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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.identifier.MCPEIdentifier;
import net.marfgamer.raknet.protocol.login.NewIncomingConnection;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.server.RakNetServerListener;
import net.marfgamer.raknet.server.ServerPing;
import net.marfgamer.raknet.session.RakNetClientSession;

/**
 * Used to test the RakNetServer by starting a server on the default
 *
 * @author MarfGamer
 */
public class RakNetServerTest {

	public static void main(String[] args) throws RakNetException {
		RakNetServer server = new RakNetServer(UtilityTest.MINECRAFT_POCKET_EDITION_DEFAULT_PORT, 10);

		server.setListener(new RakNetServerListener() {

			@Override
			public void onClientPreConnect(InetSocketAddress address) {
				System.out.println("Client from " + address + " has instantiated the connection, waiting for "
						+ NewIncomingConnection.class.getSimpleName() + " packet");
			}

			@Override
			public void onClientConnect(RakNetClientSession session) {
				System.out.println("Client from address " + session.getAddress() + " has connected to the server");
			}

			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				System.out.println("Client from address " + session.getAddress() + " has been disconnected for \""
						+ reason + "\"");
			}

			@Override
			public void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
				System.out.println(
						"Received packet from client with address " + session.getAddress() + " with packet ID 0x"
								+ Integer.toHexString(packet.getId()).toUpperCase() + " on channel " + channel);
			}

			@Override
			public void handlePing(ServerPing ping) {
				MCPEIdentifier identifier = new MCPEIdentifier();

				// Set identifier properties
				{
					identifier.setServerProtocol(91);
					identifier.setTimestamp(System.currentTimeMillis());
					identifier.setServerName("A JRakNet server test");
					identifier.setVersionTag("0.16.0");
					identifier.setWorldName("New World");
					identifier.setGamemode("Developer");
					identifier.setOnlinePlayerCount(server.getSessionCount());
					identifier.setMaxPlayerCount(server.getMaxConnections());
				}
				
				ping.setIdentifier(identifier);
			}
			
			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				System.err.println("Exception caused by " + address);
				cause.printStackTrace();
			}

			@Override
			public void onAddressBlocked(InetAddress address, long time) {
				System.out.println("Blocked address " + address + " for " + (time / 1000L) + " seconds");
			}

			@Override
			public void onAddressUnblocked(InetAddress address) {
				System.out.println("Unblocked address " + address);
			}

		});

		server.startThreaded();
		System.out.println("Started server!");
	}

}