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
 * Copyright (c) 2016, 2017 MarfGamer
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
package net.marfgamer.jraknet.client;

import net.marfgamer.jraknet.RakNetClientException;

/**
 * This exception is thrown when the client does not share the same protocol as
 * the server it is attempting to connect to
 *
 * @author MarfGamer
 */
public class IncompatibleProtocolException extends RakNetClientException {

	private static final long serialVersionUID = -3390229698349252537L;

	private final int clientProtocol;
	private final int serverProtocol;

	public IncompatibleProtocolException(RakNetClient client, int clientProtocol, int serverProtocol) {
		super(client, (clientProtocol < serverProtocol ? "Outdated client" : "Outdated server"));
		this.clientProtocol = clientProtocol;
		this.serverProtocol = serverProtocol;
	}

	/**
	 * Returns the protocol the client is on
	 * 
	 * @return The protocol the client is on
	 */
	public int getClientProtocol() {
		return this.clientProtocol;
	}

	/**
	 * Returns the protocol the server is on
	 * 
	 * @return The protocol the server is on
	 */
	public int getServerProtocol() {
		return this.serverProtocol;
	}

}
