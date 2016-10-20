package net.marfgamer.raknet.protocol.message.acknowledge;

import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.protocol.MessageIdentifier;

public class AcknowledgeReceipt extends RakNetPacket {

	public static final short RECEIPT_ACKNOWLEDGED = MessageIdentifier.ID_SND_RECEIPT_ACKED;
	public static final short RECEIPT_LOSS = MessageIdentifier.ID_SND_RECEIPT_LOSS;

	public int record;

	public AcknowledgeReceipt(int type) {
		super(type);
		if (type != RECEIPT_ACKNOWLEDGED && type != RECEIPT_LOSS) {
			throw new IllegalArgumentException("Must be ACKNOWLEDGED or NOT_ACKNOWLEDGED!");
		}
	}

	public AcknowledgeReceipt(AcknowledgeReceiptType type) {
		this(type.getId());
	}

	public AcknowledgeReceipt(Packet packet) {
		super(packet);
	}

	public AcknowledgeReceiptType getType() {
		return AcknowledgeReceiptType.lookup(this.getId());
	}

	@Override
	public void encode() {
		this.writeInt(record);
	}

	@Override
	public void decode() {
		this.record = this.readInt();
	}

}
