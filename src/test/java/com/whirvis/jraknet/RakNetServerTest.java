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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.identifier.MinecraftIdentifier;
import com.whirvis.jraknet.protocol.MessageIdentifier;
import com.whirvis.jraknet.protocol.login.NewIncomingConnection;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import com.whirvis.jraknet.server.ServerPing;
import com.whirvis.jraknet.session.RakNetClientSession;
import com.whirvis.jraknet.util.RakNetUtils;
import com.whirvis.jraknet.windows.UniversalWindowsProgram;

/**
 * Used to test <code>RakNetServer</code> by starting a server on the default
 * Minecraft port.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class RakNetServerTest {

	private static final Logger log = LoggerFactory.getLogger(RakNetServerTest.class);

	public static void main(String[] args) {
		// Add loopback exemption for Minecraft
		if (!UniversalWindowsProgram.MINECRAFT.addLoopbackExempt()) {
			log.warn("Failed to add loopback exemption for Minecraft");
		}

		// Create server and add listener
		RakNetServer server = new RakNetServer(UtilityTest.MINECRAFT_DEFAULT_PORT, 10);
		server.addListener(new RakNetServerListener() {

			@Override
			public void onClientPreConnect(InetSocketAddress address) {
				log.info("Client from " + address + " has instantiated the connection, waiting for "
						+ NewIncomingConnection.class.getSimpleName() + " packet");
			}

			@Override
			public void onClientPreDisconnect(InetSocketAddress address, String reason) {
				log.info("Client from " + address + " has failed to login for \"" + reason + "\"");
			}

			@Override
			public void onClientConnect(RakNetClientSession session) {
				log.info(session.getConnectionType().getName() + " client from address " + session.getAddress()
						+ " has connected to the server");
			}

			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				log.info(session.getConnectionType().getName() + " client from address " + session.getAddress()
						+ " has been disconnected for \"" + reason + "\"");
			}

			@Override
			public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
				log.info("Received packet from " + session.getConnectionType().getName() + " client with address "
						+ session.getAddress() + " with packet ID " + RakNetUtils.toHexStringId(packet) + " on channel "
						+ channel);
			}

			@Override
			public void handlePing(ServerPing ping) {
				MinecraftIdentifier identifier = new MinecraftIdentifier("A JRakNet server test",
						UtilityTest.MINECRAFT_PROTOCOL_NUMBER, UtilityTest.MINECRAFT_VERSION, server.getSessionCount(),
						server.getMaxConnections(), server.getGloballyUniqueId(), "New World", "Survival");
				ping.setIdentifier(identifier);
			}

			@Override
			public void onAcknowledge(RakNetClientSession session, Record record, EncapsulatedPacket packet) {
				log.info(session.getConnectionType().getName() + " client with address " + session.getAddress()
						+ " has received packet with ID: "
						+ MessageIdentifier.getName(packet.payload.readUnsignedByte()));
			}

			@Override
			public void onNotAcknowledge(RakNetClientSession session, Record record, EncapsulatedPacket packet) {
				log.info(session.getConnectionType().getName() + " client with address " + session.getAddress()
						+ " has lost packet with ID: " + MessageIdentifier.getName(packet.payload.readUnsignedByte()));
			}

			@Override
			public void onHandlerException(InetSocketAddress address, Throwable cause) {
				log.error("Exception caused by " + address);
				cause.printStackTrace();
			}

			@Override
			public void onAddressBlocked(InetAddress address, String reason, long time) {
				log.info(
						"Blocked address " + address + " due to \"" + reason + "\" for " + (time / 1000L) + " seconds");
			}

			@Override
			public void onAddressUnblocked(InetAddress address) {
				log.info("Unblocked address " + address);
			}

		});

		// Start server
		try {
			server.start();
		} catch (RakNetException e) {
			e.printStackTrace();
			server.shutdown(e.getClass().getName() + ": " + e.getMessage());
		}
	}

}