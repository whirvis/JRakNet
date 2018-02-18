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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
 * Signals that a <code>Throwable</code> was caught by the
 * <code>RakNetClientHandler</code>.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class NettyHandlerException extends RakNetClientException {

	private static final long serialVersionUID = -7405227886962804185L;

	private final RakNetClientHandler handler;
	private final Throwable cause;

	/**
	 * Constructs a <code>NettyHandlerException</code> with the specified
	 * <code>RakNetClient</code>, <code>RakNetClientHandler</code>, and
	 * <code>Throwable</code> that caused this exception to be thrown.
	 * 
	 * @param client
	 *            the <code>RakNetClient</code> that threw the exception.
	 * @param handler
	 *            the <code>RakNetClientHandler</code> that caught the exception.
	 * @param cause
	 *            the <code>Throwable</code> that was caught by the handler.
	 */
	public NettyHandlerException(RakNetClient client, RakNetClientHandler handler, Throwable cause) {
		super(client, "Exception in handler \"" + cause.getMessage() + "\"");
		this.handler = handler;
		this.cause = cause;
	}

	/**
	 * @return the <code>RakNetHandler</code> the client is using.
	 */
	public RakNetClientHandler getHandler() {
		return this.handler;
	}

	/**
	 * @return the <code>Throwable</code> that was caught by the handler.
	 */
	public Throwable getThrowableCause() {
		return this.cause;
	}

}
