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
 * Copyright (c) 2016 MarfGamer
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
 * This exception is thrown whenever the <code>RakNetClientHandler</code>
 * catches an exception caused by the server the client is attempting to connect
 * to
 *
 * @author MarfGamer
 */
public class NettyHandlerException extends RakNetClientException {

	private static final long serialVersionUID = -7405227886962804185L;

	private final RakNetClientHandler handler;
	private final Throwable cause;

	public NettyHandlerException(RakNetClient client, RakNetClientHandler handler, Throwable cause) {
		super(client, "Exception in handler \"" + cause.getMessage() + "\"");
		this.handler = handler;
		this.cause = cause;
	}

	/**
	 * Returns the handler the client is using
	 * 
	 * @return The handler the client is using
	 */
	public RakNetClientHandler getHandler() {
		return this.handler;
	}

	/**
	 * Returns the exception that was caught by the handler
	 *
	 * @return The exception that was caught by the handler
	 */
	public Throwable getCause() {
		return this.cause;
	}

}
