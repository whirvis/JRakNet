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
package com.whirvis.jraknet.server;

import java.net.InetSocketAddress;
import java.util.Objects;

import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.ConnectionType;

/**
 * Contains information about a server ping such as who sent the ping and what
 * the server will respond back with.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public final class ServerPing {

	private final InetSocketAddress sender;
	private final ConnectionType connectionType;
	private Identifier identifier;

	/**
	 * Creates a server ping.
	 * 
	 * @param sender
	 *            the address of the ping sender.
	 * @param connectionType
	 *            the connection type of the ping sender.
	 * @param identifier
	 *            the identifier to respond with.
	 */
	public ServerPing(InetSocketAddress sender, ConnectionType connectionType, Identifier identifier) {
		this.sender = sender;
		this.connectionType = connectionType;
		this.identifier = identifier;
	}

	/**
	 * Returns the address of the ping sender.
	 * 
	 * @return the address of the ping sender.
	 */
	public InetSocketAddress getSender() {
		return this.sender;
	}

	/**
	 * Returns the connection type of the ping sender.
	 * 
	 * @return the connection type of the ping sender.
	 */
	public ConnectionType getConnectionType() {
		return this.connectionType;
	}

	/**
	 * Returns the identifier being sent back to the sender.
	 * 
	 * @return the identifier being sent back to the sender.
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Sets the identifier being sent back to the sender.
	 * 
	 * @param identifier
	 *            the new identifier.
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sender, connectionType, identifier);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof ServerPing)) {
			return false;
		}
		ServerPing sp = (ServerPing) o;
		return Objects.equals(sender, sp.sender) && Objects.equals(connectionType, sp.connectionType)
				&& Objects.equals(identifier, sp.identifier);
	}

	@Override
	public String toString() {
		return "ServerPing [sender=" + sender + ", identifier=" + identifier + ", connectionType=" + connectionType
				+ "]";
	}

}
