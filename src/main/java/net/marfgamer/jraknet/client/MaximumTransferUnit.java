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
package net.marfgamer.jraknet.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import net.marfgamer.jraknet.util.map.IntMap;

/**
 * This class represents one of the maximum transfer units used during login
 *
 * @author MarfGamer
 */
public class MaximumTransferUnit {

	private final int maximumTransferUnit;
	private final int retries;
	private int retriesLeft;

	public MaximumTransferUnit(int maximumTransferUnit, int retries) {
		this.maximumTransferUnit = maximumTransferUnit;
		this.retries = retries;
		this.retriesLeft = retries;
	}

	/**
	 * Sorts an array of <code>MaximumTransferUnit</code>'s from highest to
	 * lowest maximum transfer units
	 * 
	 * @param units
	 *            The maximum transfer units to sort
	 * @return The sorted <code>MaximumTransferUnit</code>'s
	 */
	public static MaximumTransferUnit[] sort(MaximumTransferUnit[] units) {
		// Convert array to IntMap
		IntMap<MaximumTransferUnit> unitMap = new IntMap<MaximumTransferUnit>();
		for (MaximumTransferUnit unit : units) {
			unitMap.put(unit.getMaximumTransferUnit(), unit);
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

	/**
	 * Returns the size of the maximum transfer unit
	 * 
	 * @return The size of the maximum transfer unit
	 */
	public int getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Returns the default amount of retries before the client stops using this
	 * maximum transfer unit and lowers it
	 * 
	 * @return The default amount of retries before the client stops using this
	 *         maximum transfer unit and lowers it
	 */
	public int getRetries() {
		return this.retries;
	}

	/**
	 * Returns how many times <code>retry()</code> can be called before yielding
	 * 0 or lower
	 * 
	 * @return How many times <code>retry()</code> can be called before yielding
	 *         0 or lower without calling <code>reset()</code>
	 */
	public int getRetriesLeft() {
		return this.retriesLeft;
	}

	/**
	 * Returns how many retries there are left and then lowers it by one
	 * 
	 * @return How many retries are left
	 */
	public int retry() {
		return this.retriesLeft--;
	}

	/**
	 * Sets the amount of retries left back to it's default
	 */
	public void reset() {
		this.retriesLeft = this.retries;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + this.getMaximumTransferUnit() + ":" + this.getRetries() + ")";
	}

}
