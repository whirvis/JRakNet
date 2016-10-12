package net.marfgamer.raknet.protocol.acknowledge;

public enum AcknowledgeType {

	ACKNOWLEDGED(Acknowledge.ACKNOWLEDGED), NOT_ACKNOWLEDGED(Acknowledge.NOT_ACKNOWLEDGED);

	public short id;

	private AcknowledgeType(short id) {
		this.id = id;
	}

	public short getId() {
		return this.id;
	}

	public static AcknowledgeType lookup(short id) {
		for (AcknowledgeType type : AcknowledgeType.values()) {
			if (type.getId() == id) {
				return type;
			}
		}
		return null;
	}

}
