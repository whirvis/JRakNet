/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 "Whirvis" Trent Summerlin
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
import java.util.Objects;

import com.whirvis.jraknet.map.IntMap;

/**
 * Represents a packet record which is used in acknowledgement packets to
 * indicate a packet was either acknowledged (received) or not acknowledged
 * (lost in transmission).
 * 
 * @author "Whirvis" Trent Summerlin
 * @since JRakNet v1.0.0
 */
public final class Record {

	/**
	 * The record is not ranged.
	 */
	public static final int NOT_RANGED = -1;

	/**
	 * Returns the sequence IDs contained within the specified records.
	 * 
	 * @param records
	 *            the records to get the sequence IDs from.
	 * @return the sequence IDs contained within the specified records.
	 */
	public static int[] getSequenceIds(Record... records) {
		// Get sequence IDs from records
		ArrayList<Integer> sequenceIdsList = new ArrayList<Integer>();
		for (Record record : records) {
			for (int recordId : record.getSequenceIds()) {
				if (!sequenceIdsList.contains(recordId)) {
					sequenceIdsList.add(recordId);
				}
			}
		}

		// Convert boxed values to sorted native array
		int[] sequenceIds = new int[sequenceIdsList.size()];
		for (int i = 0; i < sequenceIds.length; i++) {
			sequenceIds[i] = sequenceIdsList.get(i);
		}
		Arrays.sort(sequenceIds);
		return sequenceIds;
	}

	/**
	 * Returns the sequence IDs contained within the specified records.
	 * 
	 * @param records
	 *            the records to get the sequence IDs from.
	 * @return the sequence IDs contained within the specified records.
	 */
	public static int[] getSequenceIds(List<Record> records) {
		return getSequenceIds(records.toArray(new Record[records.size()]));
	}

	/**
	 * Simplifies the specified sequence IDs into a <code>Record[]</code> with
	 * all sequence IDs having their own dedicated record to make handling them
	 * easier.
	 * 
	 * @param sequenceIds
	 *            the sequence IDs to simplify.
	 * @return the simplified records
	 */
	public static Record[] simplify(int... sequenceIds) {
		IntMap<Record> simplified = new IntMap<Record>();
		for (int i = 0; i < sequenceIds.length; i++) {
			if (!simplified.containsKey(sequenceIds[i])) {
				simplified.put(sequenceIds[i], new Record(sequenceIds[i]));
			}
		}
		return simplified.values().toArray(new Record[simplified.size()]);
	}

	/**
	 * Simplifies the specified records into a <code>Record[]</code> with all
	 * sequence IDs within the records having their own dedicated record to make
	 * handling them easier.
	 * 
	 * @param records
	 *            the records to simplify.
	 * @return the simplified records
	 */
	public static Record[] simplify(Record... records) {
		return simplify(getSequenceIds(records));
	}

	/**
	 * Simplifies the specified records into a <code>Record[]</code> with all
	 * sequence IDs within the records having their own dedicated record to make
	 * handling them easier.
	 * 
	 * @param records
	 *            the records to simplify.
	 * @return the simplified records
	 */
	public static Record[] simplify(List<Record> records) {
		return simplify(records.toArray(new Record[records.size()]));
	}

	/**
	 * Condenses the specified records into a <code>Record[]</code> with all
	 * ranges of sequence IDs being in ranged records to save memory.
	 * 
	 * @param records
	 *            the records to condense.
	 * @return the condensed records.
	 */
	public static Record[] condense(Record... records) {
		/*
		 * Get sequence IDs and sort them in ascending order. This is crucial in
		 * order for condensing to occur.
		 */
		int[] sequenceIds = Record.getSequenceIds(records);
		Arrays.sort(sequenceIds);

		// Condense records
		ArrayList<Record> condensed = new ArrayList<Record>();
		for (int i = 0; i < sequenceIds.length; i++) {
			int startIndex = sequenceIds[i];
			int endIndex = startIndex;
			if (i + 1 < sequenceIds.length) {
				while (endIndex + 1 == sequenceIds[i + 1]) {
					endIndex = sequenceIds[++i]; // This value is sequential
					if (i + 1 >= sequenceIds.length) {
						break;
					}
				}
			}
			condensed.add(new Record(startIndex, endIndex == startIndex ? -1 : endIndex));
		}
		return condensed.toArray(new Record[condensed.size()]);
	}

	/**
	 * Condenses the specified records into a <code>Record[]</code> with all
	 * ranges of sequence IDs being in ranged records to save memory.
	 * 
	 * @param records
	 *            the records to condense.
	 * @return the condensed records.
	 */
	public static Record[] condense(List<Record> records) {
		return condense(records.toArray(new Record[records.size()]));
	}

	/**
	 * Condenses the specified sequence IDs into a <code>Record[]</code> with
	 * all ranges of sequence IDs being in ranged records to save memory.
	 * 
	 * @param sequenceIds
	 *            the sequence IDs to condense.
	 * @return the condensed records.
	 */
	public static Record[] condense(int... sequenceIds) {
		Record[] records = new Record[sequenceIds.length];
		for (int i = 0; i < records.length; i++) {
			records[i] = new Record(sequenceIds[i]);
		}
		return condense(records);
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
	 * @throws IllegalArgumentException
	 *             if the <code>index</code> is negative.
	 */
	public Record(int index, int endIndex) throws IllegalArgumentException {
		if (index < 0) {
			throw new IllegalArgumentException("Index cannot be negative");
		}
		this.index = index;
		this.endIndex = endIndex;
		this.updateSequenceIds();
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
	 *             if the <code>index</code> is negative.
	 */
	public void setIndex(int index) throws IllegalArgumentException {
		if (index < 0) {
			throw new IllegalArgumentException("Index cannot be negative");
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
	public int hashCode() {
		return Objects.hash(index, endIndex);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof Record)) {
			return false;
		}
		Record r = (Record) o;
		return Objects.equals(index, r.index) && Objects.equals(endIndex, r.endIndex);
	}

	@Override
	public String toString() {
		return "Record [index=" + index + ", endIndex=" + endIndex + "]";
	}

}
