package com.whirvis.jraknet.protocol.message;

/**
 * A <code>CUSTOM_4</code> packet.
 *
 * @author Trent Summerlin
 * @since JRakNet v2.11.0
 */
public final class CustomFourPacket extends CustomPacket {

	/**
	 * Creates a <code>CUSTOM_4</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public CustomFourPacket() {
		super(ID_CUSTOM_4);
	}

}
