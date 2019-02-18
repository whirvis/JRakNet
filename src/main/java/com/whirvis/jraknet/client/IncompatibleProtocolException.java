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
package com.whirvis.jraknet.client;

import java.net.InetSocketAddress;

/**
 * Signals that a RakNet client is attempting to connect to a server with an
 * incompatible protocol.
 *
 * @author Trent Summerlin
 * @since JRakNet v2.0
 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
 * @see com.whirvis.jraknet.client.RakNetClientException RakNetClientException
 */
public class IncompatibleProtocolException extends RakNetClientException {

	private static final long serialVersionUID = -3390229698349252537L;

	private final InetSocketAddress address;
	private final int clientProtocol;
	private final int serverProtocol;

	/**
	 * Constructs an <code>IncompatibleProtocolException</code>.
	 * 
	 * @param client
	 *            the client that threw the exception.
	 * @param address
	 *            the address of the server with the incompatible protocol.
	 * @param clientProtocol
	 *            the client protocol
	 * @param serverProtocol
	 *            the server protocol
	 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
	 */
	public IncompatibleProtocolException(RakNetClient client, InetSocketAddress address, int clientProtocol,
			int serverProtocol) {
		super(client, (clientProtocol < serverProtocol ? "Outdated client" : "Outdated server"));
		this.address = address;
		this.clientProtocol = clientProtocol;
		this.serverProtocol = serverProtocol;
	}

	/**
	 * Returns the address of the server with the incompatible protocol.
	 * 
	 * @return the address of the server with the incompatible protocol.
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * Returns the protocol the client is running on.
	 * 
	 * @return the protocol the client is running on.
	 */
	public int getClientProtocol() {
		return this.clientProtocol;
	}

	/**
	 * Returns the protocol the server is running on.
	 * 
	 * @return the protocol the server is running on.
	 */
	public int getServerProtocol() {
		return this.serverProtocol;
	}

}
