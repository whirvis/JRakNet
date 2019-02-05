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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.protocol.message.acknowledge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Used for easy record manipulation for packets that use them.
 *
 * @author Trent "Whirvis" Summerlin
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
	 * @return the starting index of the record.
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * Sets the starting index of the record.
	 * 
	 * @param index
	 *            the new starting index.
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @return the ending index of the record or <code>-1</code> if the record
	 *         is not ranged.
	 */
	public int getEndIndex() {
		return this.endIndex;
	}

	/**
	 * Sets the ending index of the record. If the ending index is set to
	 * <code>-1</code> or lower (It will be set to <code>-1</code> automatically
	 * if the new end index is lower than the starting index), it is assumed
	 * that the record is a single record and not a ranged one.
	 * 
	 * @param endIndex
	 *            the new ending index.
	 */
	public void setEndIndex(int endIndex) {
		if (endIndex < this.index) {
			endIndex = -1;
		}
		this.endIndex = endIndex;
	}

	/**
	 * @return <code>true</code> if the record is ranged.
	 */
	public boolean isRanged() {
		return (this.endIndex > -1);
	}

	/**
	 * @return the record as an <code>int</code> array.
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

	/**
	 * @param records
	 *            the records to convert to an <code>int</code> array.
	 * @return the specified records as an <code>int</code> array.
	 */
	public static final int[] toArray(Record... records) {
		// Store all integers into ArrayList as boxed integers
		ArrayList<Integer> boxedPacketsOld = new ArrayList<Integer>();
		for (Record record : records) {
			for (int recordNum : record.toArray()) {
				boxedPacketsOld.add(recordNum);
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
	 * @param records
	 *            the records to convert to an int array.
	 * @return the specified records as an int array.
	 */
	public static final int[] toArray(List<Record> records) {
		return Record.toArray(records.toArray(new Record[records.size()]));
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
		} else if (object instanceof Number) {
			if (this.isRanged() == false) {
				Number compare = (Number) object;
				return (this.getIndex() == compare.longValue());
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "Record [index=" + index + ", endIndex=" + endIndex + "]";
	}

}
