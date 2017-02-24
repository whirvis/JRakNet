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
package net.marfgamer.jraknet.session;

/**
 * Represents the current status of a connection in a
 * <code>RakNetSession</code>.
 *
 * @author MarfGamer
 */
public enum RakNetState {

	/**
	 * the session is disconnected.
	 */
	DISCONNECTED(0),

	/**
	 * the session is handshaking.
	 */
	HANDSHAKING(1),

	/**
	 * the session is connected.
	 */
	CONNECTED(2);

	private final int order;

	/**
	 * Constructs a <code>RakNetState</code> with the specified order.
	 * 
	 * @param order
	 *            the order of the <code>RakNetState</code>.
	 */
	private RakNetState(int order) {
		this.order = order;
	}

	/**
	 * @return the order the state is in as an int value.
	 */
	public int getOrder() {
		return this.order;
	}

	/**
	 * @param order
	 *            the order of the state.
	 * @return the state by it's numerical order.
	 */
	public static RakNetState getState(int order) {
		for (RakNetState state : RakNetState.values()) {
			if (state.getOrder() == order) {
				return state;
			}
		}
		return null;
	}

}
