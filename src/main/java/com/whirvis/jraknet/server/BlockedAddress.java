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
 * Represents an address that the server has blocked and stores how much time
 * until they are unblocked.
 *
 * @author Whirvis T. Wheatley
 */
public class BlockedAddress {

	public static final long PERMANENT_BLOCK = -1L;

	private final long startTime;
	private final long time;

	/**
	 * Constructs a <code>BlockedClient</code> with the start time and
	 * the amount of time that the client is blocked.
	 * 
	 * @param startTime
	 *            the time the address was first blocked.
	 * @param time
	 *            the amount of time until the client is unblocked.
	 */
	public BlockedAddress(long startTime, long time) {
		this.startTime = startTime;
		this.time = time;
	}

	/**
	 * Returns the time the address was first blocked.
	 * 
	 * @return the time the address was first blocked.
	 */
	public long getStartTime() {
		return this.startTime;
	}

	/**
	 * Returns how long until the address is unblocked.
	 * 
	 * @return how long until the address is unblocked.
	 */
	public long getTime() {
		return this.time;
	}

	@Override
	public String toString() {
		return "BlockedAddress [startTime=" + startTime + ", time=" + time + "]";
	}

}
