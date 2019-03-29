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
package com.whirvis.jraknet;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.identifier.MinecraftIdentifier;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import com.whirvis.jraknet.server.ServerPing;
import com.whirvis.jraknet.windows.UniversalWindowsProgram;

/**
 * Tests {@link RakNetServer} by starting a server on the default Minecraft
 * port.
 * <p>
 * To test this, simply open a Minecraft client on the latest version of the
 * game, go to the friends list on the main menu, and connect to the server with
 * the name "A JRakNet server test". Once the client has connected and logged
 * in, the server will disconnect it and shutdown.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class RakNetServerTest {

	private static final Logger LOG = LogManager.getLogger(RakNetServerTest.class);

	private RakNetServerTest() {
		// Static class
	}

	/**
	 * The entry point for the test.
	 * 
	 * @param args
	 *            the program arguments. These values are ignored.
	 */
	public static void main(String[] args) throws RakNetException {
		if (!UniversalWindowsProgram.MINECRAFT.setLoopbackExempt(true)) {
			LOG.warn("Failed to add loopback exemption for Minecraft");
		}
		RakNetServer server = new RakNetServer(RakNetTest.MINECRAFT_DEFAULT_PORT, 10);
		server.addListener(new RakNetServerListener() {

			@Override
			public void onStart(RakNetServer server) {
				LOG.info("Server started");
			}

			@Override
			public void onShutdown(RakNetServer server) {
				LOG.info("Server shutdown");
			}

			@Override
			public void onPing(RakNetServer server, ServerPing ping) {
				ping.setIdentifier(new MinecraftIdentifier("A JRakNet server test",
						RakNetTest.MINECRAFT_PROTOCOL_NUMBER, RakNetTest.MINECRAFT_VERSION, server.getClientCount(),
						server.getMaxConnections(), server.getGloballyUniqueId(), "New World", "Survival"));
			}

			@Override
			public void onConnect(RakNetServer server, InetSocketAddress address, ConnectionType connectionType) {
				LOG.info("Client from " + address + " has connected using the " + connectionType.getName()
						+ " implementation, waiting for login");
			}

			@Override
			public void onLogin(RakNetServer server, RakNetClientPeer peer) {
				LOG.info("Client from " + peer.getAddress() + " has logged in");
				server.disconnect(peer, "Test successful");
				server.shutdown();
			}

			@Override
			public void onDisconnect(RakNetServer server, InetSocketAddress address, RakNetClientPeer peer,
					String reason) {
				LOG.info("Client from " + address + " " + (peer == null ? "failed to login" : "disconnected"));
			}

			@Override
			public void onAcknowledge(RakNetServer server, RakNetClientPeer peer, Record record,
					EncapsulatedPacket packet) {
				LOG.info(peer.getConnectionType().getName() + " client with address " + peer.getAddress()
						+ " has received " + RakNetPacket.getName(packet.payload.readUnsignedByte()) + " packet");
			}

			@Override
			public void onLoss(RakNetServer server, RakNetClientPeer peer, Record record, EncapsulatedPacket packet) {
				LOG.info(peer.getConnectionType().getName() + " client with address " + peer.getAddress() + " has lost "
						+ RakNetPacket.getName(packet.payload.readUnsignedByte()) + " packet");
			}

			@Override
			public void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
				LOG.info("Received packet from " + peer.getConnectionType().getName() + " client with address "
						+ peer.getAddress() + " with packet ID " + RakNetPacket.getName(packet) + " on channel "
						+ channel);
			}

			@Override
			public void onHandlerException(RakNetServer server, InetSocketAddress address, Throwable cause) {
				LOG.error("Exception caused by " + address, cause);
			}

			@Override
			public void onPeerException(RakNetServer server, RakNetClientPeer peer, Throwable cause) {
				LOG.error("Peer with address " + peer.getAddress() + " threw exception", cause);
			}

		});
		server.start();
	}

}