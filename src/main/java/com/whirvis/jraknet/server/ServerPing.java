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
 * Copyright (c) 2016-2019 Trent Summerlin
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

import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.ConnectionType;

/**
 * Used primarily to box an identifier sent in the response of a server to a
 * ping sent by a client so it can be modified by the user.
 *
 * @author Trent Summerlin
 */
public class ServerPing {

	private final InetSocketAddress sender;
	private Identifier identifier;
	private final ConnectionType connectionType;

	/**
	 * Constructs a <code>ServerPing</code> with the address and
	 * <code>Identifier</code>.
	 * 
	 * @param sender
	 *            the address of the ping sender.
	 * @param identifier
	 *            the <code>Identifier</code> to respond with.
	 * @param connectionType
	 *            the connection type of the ping sender.
	 */
	public ServerPing(InetSocketAddress sender, Identifier identifier, ConnectionType connectionType) {
		this.sender = sender;
		this.identifier = identifier;
		this.connectionType = connectionType;
	}

	/**
	 * Returns the address of the pings ender.
	 * 
	 * @return the address of the ping sender.
	 */
	public InetSocketAddress getSender() {
		return this.sender;
	}

	/**
	 * Returns the <code>Identifier</code> being sent back to the sender.
	 * 
	 * @return the <code>Identifier</code> being sent back to the sender.
	 */
	public Identifier getIdentifier() {
		return this.identifier;
	}

	/**
	 * Returns the connection type of the ping sender.
	 * 
	 * @return the connection type of the ping sender.
	 */
	public final ConnectionType getConnectionType() {
		return this.connectionType;
	}

	/**
	 * Sets the <code>Identifier</code> being sent back to the sender.
	 * 
	 * @param identifier
	 *            the new <code>Identifier</code>.
	 */
	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	@Override
	public String toString() {
		return "ServerPing [sender=" + sender + ", identifier=" + identifier + ", connectionType=" + connectionType
				+ "]";
	}

}
