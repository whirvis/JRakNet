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
package com.whirvis.jraknet.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.map.IntMap;

/**
 * Used by the {@link com.whirvis.jraknet.client.RakNetClient RakNetClient}
 * during login to track how and when it should modify its maximum transfer unit
 * in the login process.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0
 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
 */
public class MaximumTransferUnit {

	private static final Logger LOG = LogManager.getLogger(MaximumTransferUnit.class);

	/**
	 * Sorts an array of maximum transfer units from the highest to lowest
	 * maximum transfer units based on their maximum transfer unit size.
	 * 
	 * @param units
	 *            the maximum transfer units to sort.
	 * @return the sorted maximum transfer units.
	 * @throws NullPointerException
	 *             if the maximum transfer units to sort are <code>null</code>
	 *             or if any of the maximum transfer units in the array of
	 *             maximum transfer units are <code>null</code>.
	 */
	public static MaximumTransferUnit[] sort(MaximumTransferUnit... units) throws NullPointerException {
		if (units == null) {
			throw new NullPointerException("Units cannot be null");
		} else if (units.length <= 0) {
			return units; // Nothing to sort
		}

		// Convert array to IntMap
		IntMap<MaximumTransferUnit> unitMap = new IntMap<MaximumTransferUnit>();
		for (MaximumTransferUnit unit : units) {
			if (unit == null) {
				throw new NullPointerException("Maximum transfer unit cannot be null");
			}
			unitMap.put(unit.getSize(), unit);
		}

		// Sort IntMap
		ArrayList<MaximumTransferUnit> unitList = new ArrayList<MaximumTransferUnit>();
		NavigableMap<Integer, MaximumTransferUnit> unitTreeMap = new TreeMap<Integer, MaximumTransferUnit>(unitMap)
				.descendingMap();
		Set<Entry<Integer, MaximumTransferUnit>> unitSet = unitTreeMap.entrySet();
		Iterator<Entry<Integer, MaximumTransferUnit>> unitI = unitSet.iterator();
		while (unitI.hasNext()) {
			Entry<Integer, MaximumTransferUnit> unitEntry = unitI.next();
			unitList.add(unitEntry.getValue());
		}
		return unitList.toArray(new MaximumTransferUnit[unitList.size()]);
	}

	private final int size;
	private final int retries;
	private int retriesLeft;

	/**
	 * Creates a maximum transfer unit.
	 * 
	 * @param size
	 *            the size of the maximum transfer unit in bytes.
	 * @param retries
	 *            the amount of time the client should try to use it before
	 *            going to the next lowest maximum transfer unit size.
	 * @throws IllegalArgumentException
	 *             if the size is less than or equal to zero, the size odd (not
	 *             divisible by two), or the retry count is less than or equal
	 *             to zero.
	 */
	public MaximumTransferUnit(int size, int retries) throws IllegalArgumentException {
		if (size <= 0) {
			throw new IllegalArgumentException("Size must be greater than zero");
		} else if (size % 2 != 0) {
			throw new IllegalArgumentException("Size cannot be odd");
		} else if (retries <= 0) {
			throw new IllegalArgumentException("Retry count must be greater than zero");
		}
		this.size = size;
		this.retries = retries;
		this.retriesLeft = retries;
	}

	/**
	 * Returns the size of the maximum transfer unit in bytes.
	 * 
	 * @return the size of the maximum transfer unit in bytes.
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * Returns the default amount of retries before the client stops using this
	 * maximum transfer unit size and uses the next lowest one.
	 * 
	 * @return the default amount of retries before the client stops using this
	 *         maximum transfer unit size and uses the next lowest one.
	 */
	public int getRetries() {
		return this.retries;
	}

	/**
	 * Returns the amount of times {@link #retry()} can be called before
	 * {@link #reset()} needs to be called.
	 * 
	 * @return the amount of times {@link #retry()} can be called before
	 *         {@link #reset()} needs to be called.
	 * @see #retry()
	 * @see #reset()
	 */
	public int getRetriesLeft() {
		return this.retriesLeft;
	}

	/**
	 * Lowers the amount of retries left.
	 * 
	 * @return the amount of retries left.
	 * @throws IllegalStateException
	 *             if the amount of retries left is zero.
	 * @see #reset()
	 */
	public int retry() {
		if (retriesLeft < 0) {
			throw new IllegalStateException(
					"No more retries left, use reset() in order to reuse a maximum transfer unit");
		}
		LOG.debug("Retried maximum transfer unit with size of " + size + " bytes (" + (size * 8) + " bits)");
		return this.retriesLeft--;
	}

	/**
	 * Sets the amount of retries left back to the default. This is necessary in
	 * order to be able to reuse a maximum transfer unit once it has been
	 * depleted of its retries left.
	 * 
	 * @see #retry()
	 */
	public void reset() {
		LOG.debug("Reset maximum transfer unit with size of " + size + " bytes (" + (size * 8) + " bits)");
		this.retriesLeft = this.retries;
	}

	@Override
	public String toString() {
		return "MaximumTransferUnit [size=" + size + ", retries=" + retries + ", retriesLeft=" + retriesLeft + "]";
	}

}
