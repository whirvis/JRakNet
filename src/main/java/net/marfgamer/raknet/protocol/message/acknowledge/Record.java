package net.marfgamer.raknet.protocol.message.acknowledge;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class Record {

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

	public static void main(String[] args) {
		Record r = new Record(14);
		System.out.println(r.equals(14L));
	}

}
