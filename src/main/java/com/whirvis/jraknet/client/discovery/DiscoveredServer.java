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
package com.whirvis.jraknet.client.discovery;

import java.net.InetSocketAddress;

import com.whirvis.jraknet.identifier.Identifier;

/**
 * Represents a server that has been discovered by the
 * {@link com.whirvis.jraknet.client.RakNetClient RakNetClient}.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0
 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
 * @see com.whirvis.jraknet.client.discovery.DiscoveryThread DiscoveryThread
 */
public class DiscoveredServer {

	/**
	 * The maximum time the server can not respond to a client ping before it is
	 * dropped from the list of the client's discovered servers.
	 */
	public static final long SERVER_TIMEOUT_MILLIS = 5000L;

	private final InetSocketAddress address;
	private long discoveryTimestamp;
	private Identifier identifier;

	/**
	 * Constructs a <code>DiscoveredServer</code>.
	 * 
	 * @param address
	 *            the discovered server's address.
	 * @param discoveryTimestamp
	 *            the time the server was initially discovered.
	 * @param identifier
	 *            the server's identifier.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public DiscoveredServer(InetSocketAddress address, long discoveryTimestamp, Identifier identifier) {
		this.address = address;
		this.discoveryTimestamp = discoveryTimestamp;
		this.identifier = identifier;
	}

	/**
	 * Returns the address of the discovered server.
	 * 
	 * @return the address of the discovered server.
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * Returns the last time the server sent back a response.
	 * 
	 * @return the last time the server sent back a response.
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
	 * Returns the last identifier sent by the server.
	 * 
	 * @return the last identifier sent by the server.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Updates the identifier.
	 * 
	 * @param identifier
	 *            the new identifier.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
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
