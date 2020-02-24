/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 "Whirvis" Trent Summerlin
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
package com.whirvis.jraknet;

import com.whirvis.jraknet.RakNetPacket;

/**
 * Signals that a packet critical to the current task at hand failed to encode
 * or decode correctly.
 *
 * @author "Whirvis" Trent Summerlin
 * @since JRakNet v1.0.0
 */
public final class PacketBufferException extends RakNetException {

	private static final long serialVersionUID = -3730545025991834599L;

	private final RakNetPacket packet;

	/**
	 * Constructs a <code>PacketBufferException</code>.
	 * 
	 * @param packet
	 *            the packet that failed to encode/decode.
	 */
	public PacketBufferException(RakNetPacket packet) {
		super(RakNetPacket.getName(packet.getId()) + " packet failed to encode/decode");
		this.packet = packet;
	}

	/**
	 * Returns the packet that failed to encode/decode.
	 * 
	 * @return the packet that failed to encode/decode.
	 */
	public RakNetPacket getPacket() {
		return this.packet;
	}

}
