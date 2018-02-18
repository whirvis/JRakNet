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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.client;

import java.net.InetSocketAddress;

/**
 * Signals that a <code>RakNetClient</code> is attempting to connect to an
 * offline server.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class ServerOfflineException extends RakNetClientException {

	private static final long serialVersionUID = -3916155995964791602L;

	private final InetSocketAddress address;

	/**
	 * Constructs a <code>ServerOfflineException</code> with the specified
	 * <code>RakNetClient</code> and address.
	 * 
	 * @param client
	 *            the <code>RakNetClient</code> that threw the exception.
	 * @param address
	 *            the address of the offline server.
	 */
	public ServerOfflineException(RakNetClient client, InetSocketAddress address) {
		super(client, "Server at address " + address.toString() + " is offline");
		this.address = address;
	}

	/**
	 * @return the address of the offline server.
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

}
