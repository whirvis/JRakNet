package com.whirvis.jraknet.protocol.message;

/**
 * A <code>CUSTOM_4</code> packet.
 *
 * @author Trent Summerlin
 * @since JRakNet v2.11.0
 */
public class Custom4Packet extends CustomPacket {

	/**
	 * Creates a <code>CUSTOM_4</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public Custom4Packet() {
		super(ID_CUSTOM_4);
	}

}
