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
 * Copyright (c) 2016 Trent Summerlin
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

import static net.marfgamer.raknet.utils.RakNetUtils.*;

/**
 * Used to test <code>RakNetUtils</code>, meant for testing with Minecraft:
 * Pocket Edition servers and clients
 *
 * @author Trent Summerlin
 */
public class UtilitiesTest {

	private static final String SERVER_ADDRESS = "sg.lbsg.net";
	private static final int SERVER_PORT = 19132;

	public static void main(String[] args) {
		System.out.println("System RakNet ID: " + getRakNetID());
		System.out.println("Network Interface MTU: " + getNetworkInterfaceMTU());
		System.out.println("Server identifier: "
				+ removeColors(getServerIdentifier(SERVER_ADDRESS, SERVER_PORT)).replace(";", " - "));
		System.out.println("Server compatible?: " + (isServerCompatible(SERVER_ADDRESS, SERVER_PORT) ? "Yes" : "No"));
	}

	/**
	 * Used to remove all color codes from a Minecraft: Pocket Edition server
	 * identifier
	 * 
	 * @param identifier
	 *            The identifier to remove colors from
	 * @return String
	 */
	private static final String removeColors(String identifier) {
		char[] c = identifier.toCharArray();
		StringBuilder noColor = new StringBuilder();
		for (int i = 0; i < c.length; i++) {
			if (c[i] != '\u00A7') {
				noColor.append(c[i]);
			} else {
				i++;
			}
		}
		return noColor.toString();
	}

}
