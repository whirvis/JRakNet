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
package com.whirvis.jraknet.client.peer;

import com.whirvis.jraknet.client.RakNetClient;

/**
 * Signals that the server has requested the {@link RakNetClient} to use an
 * invalid maximum transfer unit.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.11.0
 */
public final class InvalidMaximumTransferUnitException extends PeerFactoryException {

	private static final long serialVersionUID = 1247149239806409526L;

	private final int maximumTransferUnitSize;

	/**
	 * Constructs a <code>InvalidMaximumTransferUnitException</code>.
	 * 
	 * @param client
	 *            the client that created the peer that threw the exception.
	 * @param maximumTransferUnitSize
	 *            the invalid maximum transfer unit size.
	 */
	public InvalidMaximumTransferUnitException(RakNetClient client, int maximumTransferUnitSize) {
		super(client, "Invalid maximum transfer unit size " + maximumTransferUnitSize);
		this.maximumTransferUnitSize = maximumTransferUnitSize;
	}

	/**
	 * Returns the invalid maximum transfer unit size the server requested the
	 * {@link RakNetClient} to use.
	 * 
	 * @return the invalid maximum transfer unit size the server requested the
	 *         {@link RakNetClient} to use.
	 */
	public int getMaximumTransferUnitSize() {
		return this.maximumTransferUnitSize;
	}

}
