package net.marfgamer.raknet.protocol.acknowledge;

public class RangedRecord extends Record {

	private final int endIndex;

	public RangedRecord(int startIndex, int endIndex) {
		super(startIndex);
		this.endIndex = endIndex;
	}

	public int getStartIndex() {
		return super.getIndex();
	}

	public int getEndIndex() {
		return this.endIndex;
	}

}
