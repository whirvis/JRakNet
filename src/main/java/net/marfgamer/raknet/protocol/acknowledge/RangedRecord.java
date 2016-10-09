package net.marfgamer.raknet.protocol.acknowledge;

public class RangedRecord extends Record {

	private final int endIndex;
	private final int[] indexArray;

	public RangedRecord(int startIndex, int endIndex) {
		super(startIndex);
		this.endIndex = endIndex;

		// Make sure we can make an array
		if (startIndex > endIndex) {
			throw new IllegalArgumentException("startIndex cannot be greater than endIndex!");
		}

		// Create array
		this.indexArray = new int[endIndex - startIndex + 1];
		indexArray[0] = startIndex;
		for (int i = 1; i < indexArray.length - 1; i++) {
			indexArray[i] = startIndex + i;
		}
		indexArray[indexArray.length - 1] = endIndex;
	}

	public int getStartIndex() {
		return super.getIndex();
	}

	public int getEndIndex() {
		return this.endIndex;
	}

	public int[] toArray() {
		return this.indexArray;
	}

}
