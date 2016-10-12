package net.marfgamer.raknet.protocol.message;

public class MessageIndex {

	private final int index;

	public MessageIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof MessageIndex) {
			MessageIndex compare = (MessageIndex) object;
			return this.getIndex() == compare.getIndex();
		}
		return false;
	}

	@Override
	public String toString() {
		return Integer.toString(this.getIndex());
	}

}
