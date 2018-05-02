/*
 *       _   _____            _      _   _          _
 *      | | |  __ \          | |    | \ | |        | |
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2018 Whirvis T. Wheatley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.whirvis.jraknet.client;

import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.util.RakNetUtils;

/**
 * Signals that a packet critical to the <code>RakNetClient</code> failed to
 * encode or decode correctly.
 *
 * @author Whirvis T. Wheatley
 */
public class PacketBufferException extends RakNetClientException {

	private static final long serialVersionUID = -3730545025991834599L;

	private final RakNetPacket packet;

	/**
	 * Constructs a <code>PacketBufferException</code> with the specified
	 * <code>RakNetClient</code> and <code>RakNetPacket</code>.
	 * 
	 * @param client
	 *            the <code>RakNetClient</code> that threw the exception.
	 * @param packet
	 *            the <code>RakNetPacket</code> that failed to encode/decode.
	 */
	public PacketBufferException(RakNetClient client, RakNetPacket packet) {
		super(client, "Packet with ID " + RakNetUtils.toHexStringId(packet) + " failed to encode/decode");
		this.packet = packet;
	}

	/**
	 * @return the packet that failed to encode/decode.
	 */
	public RakNetPacket getPacket() {
		return this.packet;
	}

	@Override
	public String getLocalizedMessage() {
		return "Packet failed to encode/decode";
	}

}
