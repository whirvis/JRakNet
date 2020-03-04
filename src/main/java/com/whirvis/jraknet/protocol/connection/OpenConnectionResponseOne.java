/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 Trent "Whirvis" Summerlin
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
 * An <code>OPEN_CONNECTION_RESPONSE_1</code> packet.
 * <p>
 * This packet is sent by the server to the client after receiving an
 * {@link OpenConnectionRequestOne OPEN_CONNECTION_REQUEST_1} packet.
 * 
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v1.0.0
 */
public final class OpenConnectionResponseOne extends RakNetPacket {

	/**
	 * Whether or not the magic bytes read in the packet are valid.
	 */
	public boolean magic;

	/**
	 * The server's globally unique ID.
	 */
	public long serverGuid;

	/**
	 * The server's maximum transfer unit size.
	 */
	public int maximumTransferUnit;

	/**
	 * Whether or not security should be used.
	 * <p>
	 * Since JRakNet does not have this feature implemented, <code>false</code>
	 * will always be the value used when sending this value. However, this
	 * value can be <code>true</code> if it is being set through decoding.
	 */
	public boolean useSecurity;

	/**
	 * Creates a <code>OPEN_CONNECTION_RESPONSE_1</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public OpenConnectionResponseOne() {
		super(ID_OPEN_CONNECTION_REPLY_1);
	}

	/**
	 * Creates a <code>OPEN_CONNECTION_RESPONSE_1</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public OpenConnectionResponseOne(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.useSecurity = false; // TODO: Not supported
		this.writeMagic();
		this.writeLong(serverGuid);
		this.writeBoolean(useSecurity);
		this.writeUnsignedShort(maximumTransferUnit);
	}

	@Override
	public void decode() {
		this.magic = this.readMagic();
		this.serverGuid = this.readLong();
		this.useSecurity = this.readBoolean();
		this.maximumTransferUnit = this.readUnsignedShort();
	}

}
