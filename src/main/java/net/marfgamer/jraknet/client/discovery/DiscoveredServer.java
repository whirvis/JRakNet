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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
import net.marfgamer.jraknet.util.ArrayUtils;

/**
 * This class represents a server that has been discovered by the
 * <code>RakNetClient</code> and also stores the discovered server's address and
 * <code>Identifier</code> which are both crucial to making use of this class.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class DiscoveredServer {

	public static final long SERVER_TIMEOUT_MILLI = 5000L;
	public static final String IS_JRAKNET_FIELD = "isJraknet";

	// Server data
	private final InetSocketAddress address;
	private long discoveryTimestamp;
	private Identifier identifier;
	private boolean isJraknet;

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
	 * @param isJraknet
	 *            whether or not the discovered server is a JRakNet server.
	 */
	public DiscoveredServer(InetSocketAddress address, long discoveryTimestamp, Identifier identifier,
			boolean isJraknet) {
		this.address = address;
		this.discoveryTimestamp = discoveryTimestamp;
		this.identifier = identifier;
		this.isJraknet = isJraknet;
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

	/**
	 * @return whether or not the discovered server is a JRakNet server.
	 */
	public boolean isJRakNet() {
		return this.isJraknet;
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
		return ArrayUtils.toJRakNetString(this.address, this.discoveryTimestamp, this.identifier);
	}

}
