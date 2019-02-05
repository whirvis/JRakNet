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
 * Used by the <code>RakNetClient</code> during login to track how and when it
 * should modify its maximum transfer unit in the login process.
 *
 * @author Whirvis T. Wheatley
 */
public class MaximumTransferUnit {

	private static final Logger LOG = LogManager.getLogger(MaximumTransferUnit.class);

	private final int size;
	private final int retries;
	private int retriesLeft;

	/**
	 * Constructs a <code>MaximumTransferUnit</code> with the maximum
	 * transfer unit and amount of retries before it should stop being used.
	 * 
	 * @param size
	 *            the size of the maximum transfer unit in bytes.
	 * @param retries
	 *            the amount of retries before it should stop being used.
	 */
	public MaximumTransferUnit(int size, int retries) {
		this.size = size;
		this.retries = retries;
		this.retriesLeft = retries;
	}

	/**
	 * Returns the size of the maximum transfer unit.
	 * 
	 * @return the size of the maximum transfer unit.
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * Returns the default amount of retries before the client stops using this
	 * <code>MaximumTransferUnit</code> and uses the next lowest one.
	 * 
	 * @return the default amount of retries before the client stops using this
	 *         <code>MaximumTransferUnit</code> and uses the next lowest one.
	 */
	public int getRetries() {
		return this.retries;
	}

	/**
	 * Returns the amount of times <code>retry()</code> can be called before
	 * yielding zero or lower without calling <code>reset()</code>
	 * 
	 * @return how many times <code>retry()</code> can be called before yielding
	 *         zero or lower without calling <code>reset()</code>.
	 */
	public int getRetriesLeft() {
		return this.retriesLeft;
	}

	/**
	 * Lowers the amount of retries left.
	 * 
	 * @return the amount of retries left.
	 */
	public int retry() {
		LOG.debug("Retried transfer unit with size of " + size + " bytes (" + (size * 8) + " bits)");
		return this.retriesLeft--;
	}

	/**
	 * Sets the amount of retries left back to the default.
	 */
	public void reset() {
		LOG.debug("Reset transfer unit with size of " + size + " bytes (" + (size * 8) + " bits)");
		this.retriesLeft = this.retries;
	}

	/**
	 * Sorts an array of <code>MaximumTransferUnit</code>'s from highest to
	 * lowest maximum transfer units.
	 * 
	 * @param units
	 *            the <code>MaximumTransferUnit</code>s to sort.
	 * @return the sorted <code>MaximumTransferUnit</code>s.
	 */
	public static MaximumTransferUnit[] sort(MaximumTransferUnit[] units) {
		if (units == null) {
			return new MaximumTransferUnit[0];
		}

		// Convert array to IntMap
		IntMap<MaximumTransferUnit> unitMap = new IntMap<MaximumTransferUnit>();
		for (MaximumTransferUnit unit : units) {
			if (unit == null) {
				throw new NullPointerException("Invalid maximum transfer unit");
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

	@Override
	public String toString() {
		return "MaximumTransferUnit [size=" + size + ", retries=" + retries + ", retriesLeft=" + retriesLeft + "]";
	}

}
