/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
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

/**
 * Used to test the various functions in the <code>RakNet</code> class.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class RakNetTest {

	private static final Logger LOG = LogManager.getLogger(RakNetTest.class);

	/**
	 * A valid address to make sure the
	 * {@link RakNet#parseAddressPassive(String)} method functions.
	 */
	private static final String ADDRESS_TEST_VALID = "255.255.255.255:65535";

	/**
	 * A invalid address to make sure the
	 * {@link RakNet#parseAddressPassive(String)} method functions.
	 */
	private static final String ADDRESS_TEST_INVALID = "275.3.6.28:83245";

	/**
	 * The development port used by Whirvis' networking related programs.
	 */
	public static final int WHIRVIS_DEVELOPMENT_PORT = 30851;

	/**
	 * The default port used by Minecraft servers.
	 */
	public static final int MINECRAFT_DEFAULT_PORT = 19132;

	/**
	 * The current Minecraft protocol version number.
	 */
	public static final int MINECRAFT_PROTOCOL_NUMBER = 340;

	/**
	 * The current Minecraft version.
	 */
	public static final String MINECRAFT_VERSION = "1.10.0";

	/**
	 * The address of the Lifeboat Survival Games server.
	 */
	public static final InetSocketAddress LIFEBOAT_SURVIVAL_GAMES_ADDRESS = new InetSocketAddress("sg.lbsg.net",
			MINECRAFT_DEFAULT_PORT);

	private RakNetTest() {
		// Static class
	}

	/**
	 * The entry point for the test.
	 * 
	 * @param args
	 *            the program arguments. These values are ignored.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 */
	public static void main(String[] args) throws RakNetException {
		// Parse addresses
		LOG.info("Parsing valid address " + ADDRESS_TEST_VALID + " ?= "
				+ RakNet.parseAddressPassive(ADDRESS_TEST_VALID));
		LOG.info("Parsing invalid address " + ADDRESS_TEST_INVALID + " ?= "
				+ RakNet.parseAddressPassive(ADDRESS_TEST_INVALID));

		// Print server information
		LOG.info("Server address: " + LIFEBOAT_SURVIVAL_GAMES_ADDRESS);
		LOG.info("Maximum Transfer Unit: " + RakNet.getMaximumTransferUnit());

		// Check if server is online
		LOG.info("Checking if server is online... ");
		if (RakNet.isServerOnline(LIFEBOAT_SURVIVAL_GAMES_ADDRESS)) {
			LOG.info("\tServer is online");
		} else {
			throw new RakNetException("Server is offline, unable to proceed with testing");
		}

		// Check if server is compatible
		LOG.info("Checking compatibility... ");
		if (RakNet.isServerCompatible(LIFEBOAT_SURVIVAL_GAMES_ADDRESS)) {
			LOG.info("\tServer is incompatible");
		} else {
			throw new RakNetException("Server is incompatible, unable to proceed with testing");
		}

		// Get the server identifier
		LOG.info("Getting server identifier...");
		MinecraftIdentifier identifier = new MinecraftIdentifier(
				RakNet.getServerIdentifier(LIFEBOAT_SURVIVAL_GAMES_ADDRESS));
		LOG.info("\tIdentifier: " + formatMCPEIdentifier(identifier));
	}

	/**
	 * Formats the specified Minecraft identifier.
	 * 
	 * @param identifier
	 *            the Minecraft identifier to format.
	 * @return the formated Minecraft identifier.
	 */
	public static String formatMCPEIdentifier(MinecraftIdentifier identifier) {
		if (identifier.getServerName() == null) {
			identifier.setServerName("Unknown name");
		}
		return ("[Name: " + identifier.getServerName().replaceAll("\u00A7.", "") + "] [Version: "
				+ identifier.getVersionTag() + "] [Player count: " + identifier.getOnlinePlayerCount() + "/"
				+ identifier.getMaxPlayerCount() + "] [Server type: " + identifier.getConnectionType().getName() + "]");
	}

}
