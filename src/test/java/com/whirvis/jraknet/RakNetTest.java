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

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whirvis.jraknet.identifier.MinecraftIdentifier;

/**
 * Used to test the various functions in the <code>RakNet</code> class.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class RakNetTest {

	private static final Logger log = LoggerFactory.getLogger(RakNetTest.class);

	// Test data
	private static final String ADDRESS_TEST_VALID = "255.255.255.255:65535";
	private static final String ADDRESS_TEST_INVALID = "275.3.6.28:83245";
	private static final char UNICODE_MINECRAFT_COLOR_SYMBOL = '\u00A7';
	public static final int WHIRVIS_DEVELOPMENT_PORT = 30851;
	public static final int MINECRAFT_DEFAULT_PORT = 19132;
	public static final int MINECRAFT_PROTOCOL_NUMBER = 137;
	public static final String MINECRAFT_VERSION = "1.2";
	public static final InetSocketAddress LIFEBOAT_SURVIVAL_GAMES_ADDRESS = new InetSocketAddress("sg.lbsg.net",
			MINECRAFT_DEFAULT_PORT);

	public static void main(String[] args) throws RakNetException {
		log.info("Parsing valid address " + ADDRESS_TEST_VALID + " ?= "
				+ RakNet.parseAddressPassive(ADDRESS_TEST_VALID));
		log.info("Parsing invalid address " + ADDRESS_TEST_INVALID + " ?= "
				+ RakNet.parseAddressPassive(ADDRESS_TEST_INVALID));

		// Tell the user the sever we are pinging
		log.info("Server address: " + LIFEBOAT_SURVIVAL_GAMES_ADDRESS);
		log.info("Maximum Transfer Unit: " + RakNet.getMaximumTransferUnit());

		// Check if the server is online
		log.info("Pinging server... ");
		if (RakNet.isServerOnline(LIFEBOAT_SURVIVAL_GAMES_ADDRESS)) {
			log.info("Success!");
		} else {
			throw new RakNetException("Failed to ping server, unable to proceed with testing!");
		}

		log.info("Checking compatibility... ");
		if (RakNet.isServerCompatible(LIFEBOAT_SURVIVAL_GAMES_ADDRESS)) {
			log.info("Success!");
		} else {
			throw new RakNetException("Invalid protocol, we are unable to continue with testing!");
		}

		// Get the server identifier
		log.info("Getting server identifier...");
		MinecraftIdentifier identifier = new MinecraftIdentifier(
				RakNet.getServerIdentifier(LIFEBOAT_SURVIVAL_GAMES_ADDRESS));
		log.info("Success!: " + formatMCPEIdentifier(identifier));
	}

	/**
	 * @param identifier
	 *            the <code>Identifier</code> to format.
	 * @return a formated Minecraft identifier.
	 */
	public static String formatMCPEIdentifier(MinecraftIdentifier identifier) {
		if (identifier.getServerName() == null) {
			identifier.setServerName("Unknown name");
		}
		return ("[Name: " + identifier.getServerName().replaceAll(UNICODE_MINECRAFT_COLOR_SYMBOL + ".", "")
				+ "] [Version: " + identifier.getVersionTag() + "] [Player count: " + identifier.getOnlinePlayerCount()
				+ "/" + identifier.getMaxPlayerCount() + "] [Server type: " + identifier.getConnectionType().getName()
				+ "]");
	}

}
