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
 * Copyright (c) 2016-2018 Whirvis T. Wheatley
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

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;

public class Acknowledge extends RakNetPacket {

	public static final short ACKNOWLEDGED = 0xC0;
	public static final short NOT_ACKNOWLEDGED = 0xA0;

	public ArrayList<Record> records;

	public Acknowledge(short type) {
		super(type);
		if (type != ACKNOWLEDGED && type != NOT_ACKNOWLEDGED) {
			throw new IllegalArgumentException("Must be ACKNOWLEDGED or NOT_ACKNOWLEDGED");
		}
		this.records = new ArrayList<Record>();
	}

	public Acknowledge(AcknowledgeType type) {
		this(type.getId());
	}

	public Acknowledge(Packet packet) {
		super(packet);
		this.records = new ArrayList<Record>();
	}

	/**
	 * @return the type of the acknowledge packet.
	 */
	public AcknowledgeType getType() {
		return AcknowledgeType.lookup(this.getId());
	}

	@Override
	public void encode() {
		this.condenseRecords();
		this.writeUnsignedShort(records.size());
		for (Record record : records) {
			if (record.isRanged() == false) {
				this.writeUnsignedByte(0x01); // Record is not ranged
				this.writeTriadLE(record.getIndex());
			} else {
				this.writeUnsignedByte(0x00); // Record is indeed ranged
				this.writeTriadLE(record.getIndex());
				this.writeTriadLE(record.getEndIndex());
			}
		}
	}

	@Override
	public void decode() {
		int size = this.readUnsignedShort();
		for (int i = 0; i < size; i++) {
			boolean ranged = (this.readUnsignedByte() == 0x00);
			if (ranged == false) {
				records.add(new Record(this.readTriadLE()));
			} else {
				records.add(new Record(this.readTriadLE(), this.readTriadLE()));
			}
		}
		this.simplifyRecords();
	}

	/**
	 * This method condenses the records, converting all single records (ranged
	 * records are converted back to single records through the
	 * <code>toArray</code> method in <code>Record</code>) back to as many
	 * ranged records as possible and then storing the single records that could
	 * not be converted to ranged records. For example, let's say we have
	 * records <code>[1, 2, 3, 4, 5, 66, 77]</code> and the ranged records
	 * <code>[4:7, 12:17]</code>. The output will now be
	 * <code> [1:3, 4:7, 12:17, 66, 77]</code>.
	 */
	public void condenseRecords() {
		// Sort everything missing in order for ease of operation
		int[] packets = Record.toArray(records);
		Arrays.sort(packets);
		records.clear(); // Make sure this are no duplicates

		// Let us begin the sorting
		for (int i = 0; i < packets.length; i++) {
			int record = packets[i];
			int last = record;

			// Make sure we have room to check if it's ranged
			if (i + 1 < packets.length) {
				while (last + 1 == packets[i + 1]) {
					last = packets[i + 1];
					i++;
					if (i + 1 >= packets.length) {
						break;
					}
				}
			}
			int recordEnd = packets[i];

			// Finally, we add the record
			if (record == recordEnd) {
				records.add(new Record(record));
			} else {
				Record ranged = new Record(record, recordEnd);
				records.add(ranged);
			}
		}
	}

	/**
	 * This method simplifies the records, converting the single records and the
	 * ranged records to an ordered array of single records. For example, let's
	 * say we have records <code>[1, 2, 3]</code> and the ranged records
	 * <code>[4:7,
	 * 12:17]</code>. The output will now be
	 * <code>[1, 2, 3, 4, 5, 6, 7, 12, 13, 14, 15, 16,
	 * 17]</code>.
	 */
	public void simplifyRecords() {
		// Clone the list then clear it to prevent duplicates
		int[] packets = Record.toArray(records);
		records.clear();
		for (int i = 0; i < packets.length; i++) {
			records.add(new Record(packets[i]));
		}
	}

}
