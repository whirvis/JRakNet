/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 Whirvis T. Wheatley
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
 * Signals that a <code>Throwable</code> was caught by the
 * {@link RakNetClientHandler}.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class NettyHandlerException extends RakNetClientException {

	private static final long serialVersionUID = -7405227886962804185L;

	private final RakNetClientHandler handler;
	private final InetSocketAddress address;
	private final Throwable cause;

	/**
	 * Constructs a <code>NettyHandlerException</code>.
	 * 
	 * @param client
	 *            the client that threw the exception.
	 * @param handler
	 *            the handler that caught the exception.
	 * @param address
	 *            the address of the sender that caused the exception.
	 * @param cause
	 *            the cause.
	 */
	public NettyHandlerException(RakNetClient client, RakNetClientHandler handler, InetSocketAddress address,
			Throwable cause) {
		super(client, "Exception in handler \"" + cause.getMessage() + "\"");
		this.handler = handler;
		this.address = address;
		this.cause = cause;
	}

	/**
	 * Returns the handler the client is using.
	 * 
	 * @return the handler the client is using.
	 */
	public RakNetClientHandler getHandler() {
		return this.handler;
	}

	/**
	 * Returns the address of the sender that caused the exception.
	 * 
	 * @return the address of the sender that caused the exception.
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * Returns the cause of the exception being thrown.
	 * 
	 * @return the cause of the exception being thrown.
	 */
	public Throwable getCause() {
		return this.cause;
	}

}
