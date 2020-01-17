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
package com.whirvis.jraknet.server;

/**
 * An address that the server has blocked.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public final class BlockedAddress {

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
	 * @throws IllegalArgumentException
	 *             if the <code>time</code> is less than <code>0</code> and is
	 *             not equal to {@value #PERMANENT_BLOCK}.
	 */
	public BlockedAddress(long time) throws IllegalArgumentException {
		if (time <= 0 && time != PERMANENT_BLOCK) {
			throw new IllegalArgumentException(
					"Block time must be greater than 0 or equal to " + PERMANENT_BLOCK + " for a permanent block");
		}
		this.startTime = System.currentTimeMillis();
		this.time = time;
	}

	/**
	 * Returns the time the address was first blocked. This value is the time
	 * that the original blocked address object was created, according to
	 * {@link System#currentTimeMillis()}.
	 * 
	 * @return the time the address was first blocked.
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
		if (time <= PERMANENT_BLOCK) {
			return false; // The address has been permanently blocked
		}
		return System.currentTimeMillis() - startTime >= time;
	}

	@Override
	public String toString() {
		return "BlockedAddress [startTime=" + startTime + ", time=" + time + "]";
	}

}
