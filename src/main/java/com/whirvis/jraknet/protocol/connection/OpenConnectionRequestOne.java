/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Trent Summerlin
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
package com.whirvis.jraknet.protocol.connection;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;

/**
 * An <code>OPEN_CONNECTION_REQUEST_1</code> packet.
 * <p>
 * This is the first packet sent by the client to the server during connection.
 * 
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public final class OpenConnectionRequestOne extends RakNetPacket {

	/**
	 * At the end of this packet in particular, the client pads the packet with the
	 * remaining data left according the <code>maximumTransferUnit</code>.
	 * <p>
	 * This value is equivalent to the size of the IP header (20 bytes) and the size
	 * of the UDP header (8 bytes) combined.
	 */
	private static final int MTU_PADDING = 28;

	/**
	 * Whether or not the magic bytes read in the packet are valid.
	 */
	public boolean magic;

	/**
	 * The client's network protocol version.
	 */
	public int networkProtocol;

	/**
	 * The client's maximum transfer unit size.
	 */
	public int maximumTransferUnit;

	/**
	 * Creates an <code>OPEN_CONNECTION_REQUEST_1</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public OpenConnectionRequestOne() {
		super(ID_OPEN_CONNECTION_REQUEST_1);
	}

	/**
	 * Creates an <code>OPEN_CONNECTION_REQUEST_1</code> packet to be decoded.
	 * 
	 * @param packet the original packet whose data will be read from in the
	 *               {@link #decode()} method.
	 */
	public OpenConnectionRequestOne(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeMagic();
		this.writeUnsignedByte(networkProtocol);
		this.pad(maximumTransferUnit - MTU_PADDING);
	}

	@Override
	public void decode() {
		this.magic = this.readMagic();
		this.networkProtocol = this.readUnsignedByte();
		this.maximumTransferUnit = this.remaining() + MTU_PADDING;
		this.skip(this.remaining());
	}

}
