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
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.client.discovery;

import java.net.InetSocketAddress;

import com.whirvis.jraknet.identifier.Identifier;

/**
 * This class represents a server that has been discovered by the
 * <code>RakNetClient</code> and also stores the discovered server's address and
 * <code>Identifier</code> which are both crucial to making use of this class.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class DiscoveredServer {

	public static final long SERVER_TIMEOUT_MILLI = 5000L;
	public static final String IS_JRAKNET_FIELD = "isJraknet";

	// Server data
	private final InetSocketAddress address;
	private long discoveryTimestamp;
	private Identifier identifier;

	/**
	 * Constructs a <code>DiscoveredServer</code> with the specified address,
	 * discovery timestamp, and identifier.
	 * 
	 * @param address
	 *            the discovered server's address.
	 * @param discoveryTimestamp
	 *            the time the server was initially discovered.
	 * @param identifier
	 *            the server's identifier.
	 */
	public DiscoveredServer(InetSocketAddress address, long discoveryTimestamp, Identifier identifier) {
		this.address = address;
		this.discoveryTimestamp = discoveryTimestamp;
		this.identifier = identifier;
	}

	/**
	 * @return the address of the discovered server.
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * @return the last time the server sent a response back.
	 */
	public long getDiscoveryTimestamp() {
		return this.discoveryTimestamp;
	}

	/**
	 * Updates the last time the server sent a response back.
	 * 
	 * @param discoveryTimestamp
	 *            the new discovery timestamp.
	 */
	public void setDiscoveryTimestamp(long discoveryTimestamp) {
		this.discoveryTimestamp = discoveryTimestamp;
	}

	/**
	 * @return the last identifier received by the server.
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Updates the identifier.
	 * 
	 * @param identifier
	 *            the new identifier.
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DiscoveredServer) {
			DiscoveredServer discoveredServer = (DiscoveredServer) object;
			return (discoveredServer.getAddress().equals(this.getAddress())
					&& discoveredServer.getDiscoveryTimestamp() == this.getDiscoveryTimestamp()
					&& discoveredServer.getIdentifier().equals(this.getIdentifier()));
		}
		return false;
	}

	@Override
	public String toString() {
		return "DiscoveredServer [address=" + address + ", discoveryTimestamp=" + discoveryTimestamp + ", identifier="
				+ identifier + "]";
	}

}
