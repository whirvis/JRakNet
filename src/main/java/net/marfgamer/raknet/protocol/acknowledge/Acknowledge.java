package net.marfgamer.raknet.protocol.acknowledge;

import java.util.ArrayList;
import java.util.Arrays;

import net.marfgamer.raknet.RakNetPacket;

public class Acknowledge extends RakNetPacket {

	public static final short ACKNOWLEDGED = 0xC0;
	public static final short NOT_ACKNOWLEDGED = 0xA0;

	public ArrayList<Record> records;

	public Acknowledge(short type) {
		super(type);
		if (type != ACKNOWLEDGED && type != NOT_ACKNOWLEDGED) {
			throw new IllegalArgumentException("Must be ACKNOWLEDGED or NOT_ACKNOWLEDGED!");
		}
		this.records = new ArrayList<Record>();
	}

	@Override
	public void encode() {
		this.condenseRecords();
		this.writeUShort(records.size());
		for (Record record : records) {
			if (record.isRanged() == false) {
				this.writeByte(0x01); // Record is not ranged
				this.writeUIntLE(record.getIndex());
			} else {
				this.writeByte(0x00); // Record is indeed ranged
				this.writeUIntLE(record.getIndex());
				this.writeUIntLE(record.getEndIndex());
			}
		}
	}

	@Override
	public void decode() {
		int size = this.readUShort();
		for (int i = 0; i < size; i++) {
			boolean ranged = (this.readUByte() == 0x00);
			if (ranged == false) {
				records.add(new Record(this.readUIntLE()));
			} else {
				records.add(new Record(this.readUIntLE(), this.readUIntLE()));
			}
		}
		this.simplifyRecords();
	}

	/**
	 * This method condenses the records, converting all single records (ranged
	 * records are converted back to single records through the
	 * <code>toArray</code> method in <code>Record</code>) back to as many
	 * ranged records as possible and then storing the single records that could
	 * not be converted to ranged records.
	 * 
	 * Example: Let's say we have records <code>[1, 2, 3, 4, 5, 66, 77]</code>
	 * and the ranged records <code>[4:7, 12:17]</code>. The output will now be
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
	 * ranged records to an ordered array of single records. <br>
	 * <br>
	 * Example: Let's say we have records
	 * <code>[1, 2, 3]<code> and the ranged records <code>[4:7,
	 * 12:17]</code>. The output will now be
	 * <code>[1, 2, 3, 4, 5, 6, 7, 12, 13, 14, 15, 16,
	 * 17]</code>.
	 * 
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
