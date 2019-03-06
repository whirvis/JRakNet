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
package com.whirvis.jraknet.protocol.message.acknowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a packet record which is used in acknowledgement packets to
 * indicate a packet was either acknowledged (received) or not acknowledged
 * (lost in transmission).
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public class Record {

	/**
	 * The record is not ranged.
	 */
	public static final int NOT_RANGED = -1;
	
	// TODO FIX THIS!!!!

	/**
	 * Returns the sequence IDs contained within the specified records.
	 * 
	 * @param records
	 *            the records to get the sequence IDs from.
	 * @return the sequence IDs contained within the specified records.
	 */
	public static final int[] getSequenceIds(Record... records) {
		ArrayList<Integer> boxedRecordIds = new ArrayList<Integer>();
		for (Record record : records) {
			for (int recordId : record.getSequenceIds()) {
				if (!boxedRecordIds.contains(recordId)) {
					boxedRecordIds.add(recordId);
				}
			}
		}
		return boxedRecordIds.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Returns the sequence IDs contained within the specified records.
	 * 
	 * @param records
	 *            the records to get the sequence IDs from.
	 * @return the sequence IDs contained within the specified records.
	 */
	public static final int[] getSequenceIds(List<Record> records) {
		return getSequenceIds(records.toArray(new Record[records.size()]));
	}

	private int index;
	private int endIndex;
	private int[] sequenceIds;

	/**
	 * Creates a ranged record.
	 * 
	 * @param index
	 *            the starting index.
	 * @param endIndex
	 *            the ending index.
	 * 
	 * @throws IllegalArgumentException
	 *             if the <code>index</code> is less than <code>0</code>.
	 */
	public Record(int index, int endIndex) throws IllegalArgumentException {
		if (index < 0) {
			throw new IllegalArgumentException("Index must be greater than or equal to 0");
		}
		this.index = index;
		this.endIndex = endIndex;
	}

	/**
	 * Creates a single record.
	 * 
	 * @param id
	 *            the sequence ID.
	 * 
	 * @throws IllegalArgumentException
	 *             if the <code>id</code> is less than <code>0</code>.
	 */
	public Record(int id) throws IllegalArgumentException {
		this(id, NOT_RANGED);
	}

	/**
	 * Updates the sequence IDs within the record.
	 */
	private void updateSequenceIds() {
		if (!this.isRanged()) {
			this.sequenceIds = new int[] { this.getIndex() };
		} else {
			int[] ranged = new int[this.getEndIndex() - this.getIndex() + 1];
			for (int i = 0; i < ranged.length; i++) {
				ranged[i] = i + this.getIndex();
			}
			this.sequenceIds = ranged;
		}
	}

	/**
	 * Returns the starting index of the record.
	 * 
	 * @return the starting index of the record.
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * Sets the starting index of the record.
	 * 
	 * @param index
	 *            the starting index.
	 * @throws IllegalArgumentException
	 *             if the <code>index</code> is less than <code>0</code>.
	 */
	public void setIndex(int index) throws IllegalArgumentException {
		if (index < 0) {
			throw new IllegalArgumentException("Index must be greater than or equal to 0");
		}
		this.index = index;
		this.updateSequenceIds();
	}

	/**
	 * Returns the ending index of the record.
	 * 
	 * @return the ending index of the record, {@value #NOT_RANGED} if the
	 *         record is not ranged.
	 * @see #isRanged()
	 */
	public int getEndIndex() {
		return this.endIndex;
	}

	/**
	 * Sets the ending index of the record.
	 * 
	 * @param endIndex
	 *            the ending index, a value of {@value #NOT_RANGED} or lower or
	 *            to the value of the index itself indicates that the record is
	 *            not ranged.
	 */
	public void setEndIndex(int endIndex) {
		if (endIndex <= this.index) {
			endIndex = NOT_RANGED;
		}
		this.endIndex = endIndex;
		this.updateSequenceIds();
	}

	/**
	 * Returns whether or not the record is ranged.
	 * 
	 * @return <code>true</code> if the record is ranged, <code>false</code>
	 *         otherwise.
	 */
	public boolean isRanged() {
		return endIndex > NOT_RANGED;
	}

	/**
	 * Returns the sequence ID contained within this record. This is the
	 * equivalent of calling {@link #getIndex()}, however an error will be
	 * thrown if the record is ranged.
	 * 
	 * @return the sequence ID contained within this record.
	 * @throws ArrayStoreException
	 *             if the record is ranged according to the {@link #isRanged()}
	 *             method.
	 * @see #getSequenceIds()
	 */
	public int getSequenceId() throws ArrayStoreException {
		if (this.isRanged()) {
			throw new ArrayStoreException("Record is ranged, there are multiple IDs");
		}
		return this.getIndex();
	}

	/**
	 * Returns the sequence IDs contained within this record.
	 * 
	 * @return the sequence IDs contained within this record.
	 * @see #getSequenceId()
	 */
	public int[] getSequenceIds() {
		return this.sequenceIds;
	}

	@Override
	public String toString() {
		return "Record [index=" + index + ", endIndex=" + endIndex + "]";
	}

}
