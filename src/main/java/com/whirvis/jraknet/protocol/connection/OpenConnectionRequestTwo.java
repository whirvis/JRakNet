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

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.Failable;

/**
 * An <code>OPEN_CONNECTION_REQUEST_2</code> packet.
 * <p>
 * This packet is sent by the client to the server after receiving a
 * {@link OpenConnectionResponseOne OPEN_CONNECTION_RESPONSE_1} packet.
 * 
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public class OpenConnectionRequestTwo extends RakNetPacket implements Failable {

	/**
	 * Whether or not the magic bytes read in the packet are valid.
	 */
	public boolean magic;

	/**
	 * The address of the server that the client wishes to connect to.
	 */
	public InetSocketAddress address;

	/**
	 * The maximum transfer unit size the client and the server have agreed
	 * upon.
	 */
	public int maximumTransferUnit;

	/**
	 * The client's globally unique ID.
	 */
	public long clientGuid;

	/**
	 * The client connection type.
	 */
	public ConnectionType connectionType;

	/**
	 * Whether or not the packet failed to encode/decode.
	 */
	private boolean failed;

	/**
	 * Creates an <code>OPEN_CONNECTION_REQUEST_2</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public OpenConnectionRequestTwo() {
		super(ID_OPEN_CONNECTION_REQUEST_2);
	}

	/**
	 * Creates an <code>OPEN_CONNECTION_REQUEST_2</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public OpenConnectionRequestTwo(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		try {
			this.writeMagic();
			this.writeAddress(address);
			this.writeUnsignedShort(maximumTransferUnit);
			this.writeLong(clientGuid);
			this.writeConnectionType(connectionType);
		} catch (UnknownHostException | RakNetException e) {
			this.magic = false;
			this.address = null;
			this.maximumTransferUnit = 0;
			this.clientGuid = 0;
			this.connectionType = null;
			this.clear();
			this.failed = true;
		}
	}

	@Override
	public void decode() {
		try {
			this.magic = this.checkMagic();
			this.address = this.readAddress();
			this.maximumTransferUnit = this.readUnsignedShort();
			this.clientGuid = this.readLong();
			this.connectionType = this.readConnectionType();
		} catch (UnknownHostException | RakNetException e) {
			this.magic = false;
			this.address = null;
			this.maximumTransferUnit = 0;
			this.clientGuid = 0;
			this.connectionType = null;
			this.clear();
			this.failed = true;
		}
	}

	@Override
	public boolean failed() {
		return this.failed;
	}

}
