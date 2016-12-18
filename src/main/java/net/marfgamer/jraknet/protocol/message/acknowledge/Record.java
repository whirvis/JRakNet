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
 * Copyright (c) 2016 MarfGamer
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
package net.marfgamer.jraknet.protocol.message.acknowledge;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a record in RakNet, used mainly for easy record manipulation for
 * packets that use them
 *
 * @author MarfGamer
 */
public class Record {

	private int index;
	private int endIndex;

	public Record(int index, int endIndex) {
		this.index = index;
		this.endIndex = endIndex;
	}

	public Record(int index) {
		this(index, -1);
	}

	/**
	 * Returns the starting index of the record
	 * 
	 * @return The starting index of the record
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * Sets the starting index of the record
	 * 
	 * @param index
	 *            The new starting index
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Returns the ending index of the record <br>
	 * <br>
	 * <b>Note:</b> It is very possible for this method to return -1, to make
	 * sure no errors occur make sure it is ranged by calling
	 * <code>isRanged()</code>
	 * 
	 * @return The ending index of the record
	 */
	public int getEndIndex() {
		return this.endIndex;
	}

	/**
	 * Sets the ending index of the record <br>
	 * <br>
	 * <b>Note:</b> If the ending index is set to -1 or lower (It will be set to
	 * -1 automatically if the new end index is lower than the starting index),
	 * it is assumed that the record is a single record and not a ranged one
	 * 
	 * @param endIndex
	 *            The new ending index
	 */
	public void setEndIndex(int endIndex) {
		if (endIndex < this.index) {
			endIndex = -1;
		}
		this.endIndex = endIndex;
	}

	/**
	 * Returns whether or not the record is ranged
	 * 
	 * @return Whether or not the record is ranged
	 */
	public boolean isRanged() {
		return (this.endIndex > -1);
	}

	/**
	 * Returns the record as an integer array
	 * 
	 * @return The record as an integer array
	 */
	public int[] toArray() {
		if (this.isRanged() == false) {
			return new int[] { this.getIndex() };
		} else {
			int[] ranged = new int[this.getEndIndex() - this.getIndex() + 1];
			ranged[0] = this.getIndex();
			for (int i = 1; i < ranged.length - 1; i++) {
				ranged[i] = i + this.getIndex();
			}
			ranged[ranged.length - 1] = this.getEndIndex();
			return ranged;
		}
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Record) {
			Record compare = (Record) object;
			if (this.isRanged() == true) {
				return this.getIndex() == compare.getIndex() && this.getEndIndex() == compare.getEndIndex();
			} else {
				return this.getIndex() == compare.getIndex();
			}
		} else if (object instanceof Byte || object instanceof Short || object instanceof Integer
				|| object instanceof Long) {
			if (this.isRanged() == false) {
				return (this.getIndex() == (long) object);
			}
		}
		return false;
	}

	@Override
	public String toString() {
		if (this.isRanged() == true) {
			return (this.getIndex() + ":" + this.getEndIndex());
		} else {
			return Integer.toString(this.getIndex());
		}
	}

	/**
	 * Returns the specified records an an integer array
	 * 
	 * @param records
	 *            The records to convert to an integer array
	 * @return The specified records as an integer array
	 */
	public static final int[] toArray(Record... records) {
		// Store all integers into ArrayList as boxed integers
		ArrayList<Integer> boxedPacketsOld = new ArrayList<Integer>();
		for (Record record : records) {
			for (int recordNum : record.toArray()) {
				boxedPacketsOld.add(new Integer(recordNum));
			}
		}

		// Do so again but remove duplicates
		ArrayList<Integer> boxedPackets = new ArrayList<Integer>();
		for (Integer boxedPacket : boxedPacketsOld) {
			if (!boxedPackets.contains(boxedPacket)) {
				boxedPackets.add(boxedPacket);
			}
		}

		// Convert boxed integers to native integers
		int[] nativePackets = new int[boxedPackets.size()];
		for (int i = 0; i < nativePackets.length; i++) {
			nativePackets[i] = boxedPackets.get(i).intValue();
		}
		Arrays.sort(nativePackets);
		return nativePackets;
	}

	/**
	 * Returns the specified records an an integer array
	 * 
	 * @param records
	 *            The records to convert to an integer array
	 * @return The specified records as an integer array
	 */
	public static final int[] toArray(List<Record> records) {
		return Record.toArray(records.toArray(new Record[records.size()]));
	}

}
