package net.marfgamer.raknet.protocol.message.acknowledge;

public enum AcknowledgeReceiptType {

	ACKNOWLEDGED(AcknowledgeReceipt.RECEIPT_ACKNOWLEDGED), LOSS(AcknowledgeReceipt.RECEIPT_LOSS);

	public short id;

	private AcknowledgeReceiptType(short id) {
		this.id = id;
	}

	public short getId() {
		return this.id;
	}

	public static AcknowledgeReceiptType lookup(short id) {
		for (AcknowledgeReceiptType type : AcknowledgeReceiptType.values()) {
			if (type.getId() == id) {
				return type;
			}
		}
		return null;
	}

}
