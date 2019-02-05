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
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.session;

import com.whirvis.jraknet.map.IntMap;

/**
 * Represents the current status of a connection in a
 * <code>RakNetSession</code>.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class RakNetState {

	private static final IntMap<RakNetState> registeredStates = new IntMap<RakNetState>();

	/**
	 * The session is disconnected.
	 */
	public static final RakNetState DISCONNECTED = new RakNetState(0);

	/**
	 * The session is handshaking.
	 */
	public static final RakNetState HANDSHAKING = new RakNetState(1);

	/**
	 * The session is connected.
	 */
	public static final RakNetState CONNECTED = new RakNetState(2);

	private final int order;

	/**
	 * Constructs a <code>RakNetState</code> with the order.
	 * 
	 * @param order
	 *            the order of the <code>RakNetState</code>.
	 */
	private RakNetState(int order) {
		this.order = order;
		registeredStates.put(order, this);
	}

	/**
	 * Returns the order the state is in.
	 * 
	 * @return the order the state is in.
	 */
	public int getOrder() {
		return this.order;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RakNetState) {
			RakNetState stateObj = (RakNetState) obj;
			return this.getOrder() >= stateObj.getOrder();
		}
		return false;
	}

	/**
	 * Returns the state based on its order.
	 * 
	 * @param order
	 *            the order of the state.
	 * @return the state based on its order.
	 */
	public static RakNetState getState(int order) {
		return registeredStates.get(order);
	}

	@Override
	public String toString() {
		return "RakNetState [order=" + order + "]";
	}

}
