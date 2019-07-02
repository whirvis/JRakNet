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
package com.whirvis.jraknet.peer;

/**
 * Represents the current status of a connection in a {@link RakNetPeer}.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public enum RakNetState {

	/*
	 * The registration states use bitflags to simplify things. The first bit
	 * signifies if the peer is connected, the second bit indicates if the peer is
	 * handshaking, and the thirt bit indicates if the peer is logged in.
	 */

	/**
	 * The peer is connected.
	 * <p>
	 * This is the starting value of the state for all peers as it is assumed a peer
	 * has connected by the time it is created.
	 */
	CONNECTED(0b001, 0b001),

	/**
	 * The peer is handshaking.
	 */
	HANDSHAKING(0b011, 0b010),

	/**
	 * The peer is logged in.
	 */
	LOGGED_IN(0b101, 0b101),

	/**
	 * The peer is disconnected.
	 */
	DISCONNECTED(0b000, 0b000);

	private final int flags;
	private final int defining;

	/**
	 * Constructs a <code>RakNetState</code> with the specified ID.
	 * 
	 * @param flags    the flags of the state.
	 * @param defining the defining flags of the state. If the <code>flags</code> of
	 *                 another state AND'ed with the <code>defining</code> flags of
	 *                 this state are greater than <code>0</code>, then the other
	 *                 state is a derivative of this state.
	 */
	private RakNetState(int flags, int defining) {
		this.flags = flags;
		this.defining = defining;
	}

	/**
	 * Returns whether or not this state is a derivative of the specified state.
	 * 
	 * @param state the state.
	 * @return <code>true</code> if the state is a derivative of this state,
	 *         <code>false</code> otherwise.
	 * @throws UnsupportedOperationException if the defining flags of the
	 *                                       <code>state</code> are
	 *                                       <code>null (0)</code>.
	 */
	public boolean isDerivative(RakNetState state) throws UnsupportedOperationException {
		if (state == null) {
			return false;
		} else if (state.defining == 0) {
			throw new UnsupportedOperationException("Defining flags are null (0)");
		}
		return (flags & state.defining) > 0;
	}

}
