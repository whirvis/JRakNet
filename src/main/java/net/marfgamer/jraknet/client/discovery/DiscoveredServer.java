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
package net.marfgamer.jraknet.client.discovery;

import java.net.InetSocketAddress;

import net.marfgamer.jraknet.identifier.Identifier;

/**
 * This class represents a discovered RakNet server
 *
 * @author MarfGamer
 */
public class DiscoveredServer {

	public static final long SERVER_TIMEOUT_MILLI = 5000L;

	private final InetSocketAddress address;
	private long discoveryTimestamp;
	private Identifier identifier;

	public DiscoveredServer(InetSocketAddress address, long discoveryTimestamp, Identifier identifier) {
		this.address = address;
		this.discoveryTimestamp = discoveryTimestamp;
		this.identifier = identifier;
	}

	/**
	 * Returns the address of the discovered server
	 * 
	 * @return The address of the discovered server
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * Returns the last time the server sent a response back
	 * 
	 * @return The last time the server sent a response back
	 */
	public long getDiscoveryTimestamp() {
		return this.discoveryTimestamp;
	}

	/**
	 * Updates the last time the server sent a response back
	 * 
	 * @param discoveryTimestamp
	 *            The new discovery timestamp
	 */
	public void setDiscoveryTimestamp(long discoveryTimestamp) {
		this.discoveryTimestamp = discoveryTimestamp;
	}

	/**
	 * Returns the identifier sent in the response
	 * 
	 * @return The identifier sent in the response
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Updates the identifier sent in the response
	 * 
	 * @param identifier
	 *            The new identifier sent in the response
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

}
