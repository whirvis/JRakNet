package net.marfgamer.raknet.protocol.acknowledge;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class Record {

	public static final RecordComparator COMPARATOR = new RecordComparator();

	private final int index;
	private final int endIndex;

	public Record(int index, int endIndex) {
		this.index = index;
		this.endIndex = endIndex;
	}

	public Record(int index) {
		this(index, -1);
	}

	public int getIndex() {
		return this.index;
	}

	public int getEndIndex() {
		return this.endIndex;
	}

	public boolean isRanged() {
		return (this.endIndex > -1);
	}

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

	public static final int[] toArray(List<Record> records) {
		return Record.toArray(records.toArray(new Record[records.size()]));
	}

}
