package com.whirvis.jraknet.protocol.message;

/**
 * A <code>CUSTOM_4</code> packet.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.11.0
 */
public class CustomFourPacket extends CustomPacket {

	/**
	 * Creates a <code>CUSTOM_4</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public CustomFourPacket() {
		super(ID_CUSTOM_4);
	}

}
