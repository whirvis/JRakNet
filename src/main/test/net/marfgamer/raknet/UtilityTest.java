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

import static net.marfgamer.raknet.util.RakNetUtils.*;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.identifier.MCPEIdentifier;
import net.marfgamer.raknet.util.RakNetUtils;

/**
 * Used to test the various functions in the RakNetUtils class
 *
 * @author MarfGamer
 */
public class UtilityTest {

	private static final char UNICODE_MINECRAFT_COLOR_SYMBOL = '\u00A7';
	protected static final int MINECRAFT_POCKET_EDITION_DEFAULT_PORT = 19132;
	protected static final InetSocketAddress LIFEBOAT_SURVIVAL_GAMES_ADDRESS = new InetSocketAddress("sg.lbsg.net",
			MINECRAFT_POCKET_EDITION_DEFAULT_PORT);

	public static void main(String[] args) throws RakNetException {
		// Tell the user the sever we are pinging
		System.out.println("Server address: " + LIFEBOAT_SURVIVAL_GAMES_ADDRESS);
		System.out.println("Maximum Transfer Unit: " + RakNetUtils.getMaximumTransferUnit());

		// Check if the server is online
		System.out.print("Pinging server... ");
		if (isServerOnline(LIFEBOAT_SURVIVAL_GAMES_ADDRESS)) {
			System.out.println("Success!");
		} else {
			throw new RakNetException("Failed to connect to server, unable to proceed with testing!");
		}

		System.out.print("Checking compatibility... ");
		if (isServerCompatible(LIFEBOAT_SURVIVAL_GAMES_ADDRESS)) {
			System.out.println("Success!");
		} else {
			throw new RakNetException("Invalid protocol, we are unable to continue with testing!");
		}

		// Get the server identifier
		System.out.print("Server identifier: ");
		MCPEIdentifier identifier = new MCPEIdentifier(getServerIdentifier(LIFEBOAT_SURVIVAL_GAMES_ADDRESS));
		System.out.println("[Name: " + identifier.getServerName().replaceAll(UNICODE_MINECRAFT_COLOR_SYMBOL + ".", "")
				+ "] [Version: " + identifier.getVersionTag() + "] [Player count: " + identifier.getOnlinePlayerCount()
				+ "/" + identifier.getMaxPlayerCount() + "]");
	}

}
