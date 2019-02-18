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
package com.whirvis.jraknet.server;

/**
 * An address that the server has blocked.
 *
 * @author Whirvis T. Wheatley
 * @see com.whirvis.jraknet.server.RakNetServerHandler RakNetServerHandler
 * @see com.whirvis.jrkanet.server.RakNetServer RakNetServer
 */
public class BlockedAddress {

	/**
	 * The address is blocked permanently.
	 */
	public static final long PERMANENT_BLOCK = -1L;

	private final long startTime;
	private final long time;

	/**
	 * Creates a blocked address.
	 * 
	 * @param time
	 *            the amount of time until the client is unblocked in
	 *            milliseconds.
	 */
	public BlockedAddress(long time) {
		if (time <= 0 && time != PERMANENT_BLOCK) {
			throw new IllegalArgumentException(
					"Block time must be greater than zero or equal to negative one (for infinite executions)");
		}
		this.startTime = System.currentTimeMillis();
		this.time = time;
	}

	/**
	 * Returns the time the address was first blocked. This value is the time
	 * that the original blocked address object was created, according to
	 * {@link System#currentTimeMillis() System.currentTimeMillis()}.
	 * 
	 * @return the time the address was first blocked.
	 * @see System#currentTimeMillis()
	 */
	public long getStartTime() {
		return this.startTime;
	}

	/**
	 * Returns the amount of time the address has been blocked.
	 * 
	 * @return the amount of time the address has been blocked.
	 */
	public long getTime() {
		return this.time;
	}

	/**
	 * Returns whether or not the blocked address should be unblocked.
	 * 
	 * @return <code>true</code> if the address should be unblocked,
	 *         <code>false</code> otherwise.
	 */
	public boolean shouldUnblock() {
		if (time == PERMANENT_BLOCK) {
			return false; // The address has been permanently blocked
		}
		return System.currentTimeMillis() - startTime >= time;
	}

	@Override
	public String toString() {
		return "BlockedAddress [startTime=" + startTime + ", time=" + time + "]";
	}

}
