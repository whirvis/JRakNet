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
package com.whirvis.jraknet.client;

/**
 * Signals that a <code>RakNetClient</code> does not share the same protocol as
 * the server it is attempting to connect to.
 *
 * @author Whirvis T. Wheatley
 */
public class IncompatibleProtocolException extends RakNetClientException {

	private static final long serialVersionUID = -3390229698349252537L;

	private final int clientProtocol;
	private final int serverProtocol;

	/**
	 * Constructs an <code>IncompatibleProtocolException</code> with the
	 * specified <code>RakNetClient</code>, client protocol, and server
	 * protocol.
	 * 
	 * @param client
	 *            the <code>RakNetClient</code> that threw the exception.
	 * @param clientProtocol
	 *            the client's protocol
	 * @param serverProtocol
	 *            the server's protocol
	 */
	public IncompatibleProtocolException(RakNetClient client, int clientProtocol, int serverProtocol) {
		super(client, (clientProtocol < serverProtocol ? "Outdated client" : "Outdated server"));
		this.clientProtocol = clientProtocol;
		this.serverProtocol = serverProtocol;
	}

	/**
	 * @return the protocol the client is on.
	 */
	public int getClientProtocol() {
		return this.clientProtocol;
	}

	/**
	 * @return the protocol the server is on.
	 */
	public int getServerProtocol() {
		return this.serverProtocol;
	}

}
