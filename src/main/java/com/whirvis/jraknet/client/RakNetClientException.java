/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 Trent Summerlin
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

import com.whirvis.jraknet.RakNetException;

/**
 * Signals that an error has occurred in a {@link RakNetClient}
 *
 * @author Trent Summerlin
 * @since JRakNet v2.0.0
 */
public class RakNetClientException extends RakNetException {

	private static final long serialVersionUID = 2441122006497992080L;

	private final RakNetClient client;

	/**
	 * Constructs a <code>RakNetClientException</code>.
	 * 
	 * @param client
	 *            the client that threw the exception.
	 * @param error
	 *            the detail message.
	 */
	public RakNetClientException(RakNetClient client, String error) {
		super(error);
		this.client = client;
	}

	/**
	 * Constructs a <code>RakNetClientException</code>.
	 * 
	 * @param client
	 *            the client that threw the exception.
	 * @param error
	 *            the <code>Throwable</code> that was thrown.
	 */
	public RakNetClientException(RakNetClient client, Throwable error) {
		super(error);
		this.client = client;
	}

	/**
	 * Returns the client that threw the exception.
	 * 
	 * @return the client that threw the exception.
	 */
	public final RakNetClient getClient() {
		return this.client;
	}

}
