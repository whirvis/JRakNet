package net.marfgamer.raknet.protocol.acknowledge;

import java.util.ArrayList;

import net.marfgamer.raknet.RakNetPacket;

public class Acknowledge extends RakNetPacket {

	public static final short ACKNOWLEDGED = 0xC0;
	public static final short NOT_ACKNOWLEDGED = 0xA0;

	public ArrayList<Record> records;

	public Acknowledge(short type) {
		super(type);
		if (type != ACKNOWLEDGED && type != NOT_ACKNOWLEDGED) {
			throw new IllegalArgumentException("Invalid ID!");
		}
		this.records = new ArrayList<Record>();
	}

	@Override
	public void encode() {
		this.writeUShort(records.size());
		for (Record record : records) {
			boolean ranged = record instanceof RangedRecord;
			System.out.println(ranged);
			if (ranged == false) {
				this.writeByte(0x01); // Record is not ranged
				this.writeUIntLE(record.getIndex());
			} else {
				throw new RuntimeException("Ranged records are currently not supported!");
				/* TODO
				 * RangedRecord rangedRecord = (RangedRecord) record;
				 * this.writeByte(0x00); // Record is ranged
				 * this.writeUIntLE(rangedRecord.getStartIndex());
				 * this.writeUIntLE(rangedRecord.getEndIndex());
				 */
			}
		}
	}

	@Override
	public void decode() {

	}

	public void addRecord(int index) {
		records.add(new Record(index));
	}

	/* TODO
	 * public void addRecord(int startIndex, int endIndex) {
	 * 		records.add(new RangedRecord(startIndex, endIndex));
	 * }
	*/

}
