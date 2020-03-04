/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.discovery;

import java.net.InetSocketAddress;
import java.util.Objects;

import com.whirvis.jraknet.identifier.Identifier;

/**
 * Represents a server that has been discovered by the {@link Discovery} system.
 *
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v2.0.0
 * @see DiscoveryListener
 */
public final class DiscoveredServer {

	/**
	 * The maximum time a server can not respond to a ping before it is
	 * forgotten.
	 */
	public static final long SERVER_TIMEOUT_MILLIS = 5000L;

	private final InetSocketAddress address;
	private final boolean external;
	private long timestamp;
	private Identifier identifier;

	/**
	 * Constructs a <code>DiscoveredServer</code>.
	 * 
	 * @param address
	 *            the discovered server's address.
	 * @param external
	 *            <code>true</code> if the server is an external server,
	 *            <code>false</code> otherwise.
	 * @param identifier
	 *            the server's identifier.
	 * @throws NullPointerException
	 *             if the address, IP address, or identifier are
	 *             <code>null</code>.
	 */
	protected DiscoveredServer(InetSocketAddress address, boolean external, Identifier identifier)
			throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		} else if (identifier == null) {
			throw new NullPointerException("Identifier cannot be null");
		}
		this.address = address;
		this.external = external;
		this.timestamp = System.currentTimeMillis();
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
	 * Returns whether or not the server is an external server or was discovered
	 * on the local network.
	 * <p>
	 * If the server is on the local network yet was added to the external
	 * server discovery list, this will yield <code>true</code>.
	 * 
	 * @return <code>true</code> if the server is an external server,
	 *         <code>false</code> if the server was discovered on the local
	 *         network.
	 */
	public boolean isExternal() {
		return this.external;
	}

	/**
	 * Returns how much time has passed since the server last sent back a
	 * response in milliseconds.
	 * 
	 * @return how much time has passed since the server last sent back a
	 *         response in milliseconds.
	 */
	public long getTimestamp() {
		return System.currentTimeMillis() - timestamp;
	}

	/**
	 * Updates the last time the server sent a response back.
	 * 
	 * @param timestamp
	 *            the new discovery timestamp.
	 * @throws IllegalArgumentException
	 *             if the <code>timestamp</code> is less than than the current
	 *             discovery timestamp.
	 */
	public void setTimestamp(long timestamp) throws IllegalArgumentException {
		if (timestamp < this.timestamp) {
			throw new IllegalArgumentException("Discovery timestamp cannot be lower than the one before it");
		}
		this.timestamp = timestamp;
	}

	/**
	 * Returns whether or not the server has timed out, and should thus be
	 * forgotten.
	 * 
	 * @return <code>true</code> if the server has timed out, <code>false</code>
	 *         otherwise.
	 */
	public boolean hasTimedOut() {
		return this.getTimestamp() >= SERVER_TIMEOUT_MILLIS;
	}

	/**
	 * Returns the last identifier sent by the server.
	 * 
	 * @return the last identifier sent by the server.
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Updates the identifier.
	 * 
	 * @param identifier
	 *            the new identifier.
	 * @throws NullPointerException
	 *             if the <code>identifier</code> is <code>null</code>.
	 */
	protected void setIdentifier(Identifier identifier) throws NullPointerException {
		if (identifier == null) {
			throw new NullPointerException("Identifier cannot be null");
		}
		this.identifier = identifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, external, timestamp, identifier);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof DiscoveredServer)) {
			return false;
		}
		DiscoveredServer ds = (DiscoveredServer) o;
		return Objects.equals(address, ds.address) && Objects.equals(external, ds.external)
				&& Objects.equals(timestamp, ds.timestamp) && Objects.equals(identifier, ds.identifier);
	}

	@Override
	public String toString() {
		return "DiscoveredServer [address=" + address + ", external=" + external + ", timestamp=" + timestamp
				+ ", identifier=" + identifier + ", getTimestamp()=" + getTimestamp() + "]";
	}

}
