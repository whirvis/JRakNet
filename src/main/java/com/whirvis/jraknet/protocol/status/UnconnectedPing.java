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
package com.whirvis.jraknet.protocol.status;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.Failable;

/**
 * An <code>UNCONNECTED_PING</code> packet.
 * <p>
 * This packet is sent by clients either by broadcasting to the local network or
 * sending directly to servers in order to get their status and descriptor, also
 * known as an {@link com.whirvis.jraknet.identifier.Identifier Identifier}.
 * 
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public class UnconnectedPing extends RakNetPacket implements Failable {

	/**
	 * The timestamp of the sender.
	 */
	public long timestamp;

	/**
	 * Whether or not the magic bytes read in the packet are valid.
	 */
	public boolean magic;

	/**
	 * The client's ping ID.
	 */
	public long pingId;

	/**
	 * The client's connection type.
	 */
	public ConnectionType connectionType;

	/**
	 * Whether or not the packet failed to encode/decode.
	 */
	private boolean failed;

	/**
	 * Creates an <code>UNCONNECTED_PING</code> packet to be encoded.
	 * 
	 * @param requiresOpenConnections
	 *            <code>true</code> if the server should only respond if it has
	 *            open connections available, <code>false</code> if the server
	 *            should unconditionally respond.
	 * @see #encode()
	 */
	protected UnconnectedPing(boolean requiresOpenConnections) {
		super((requiresOpenConnections ? ID_UNCONNECTED_PING_OPEN_CONNECTIONS : ID_UNCONNECTED_PING));
	}

	/**
	 * Creates an <code>UNCONNECTED_PING</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public UnconnectedPing() {
		this(false);
	}

	/**
	 * Creates an <code>UNCONNECTED_PING</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public UnconnectedPing(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		try {
			this.writeLong(timestamp);
			this.writeMagic();
			this.writeLong(pingId);
			this.writeConnectionType(connectionType);
		} catch (RakNetException e) {
			this.timestamp = 0;
			this.magic = false;
			this.pingId = 0;
			this.connectionType = null;
			this.clear();
			this.failed = true;
		}
	}

	@Override
	public void decode() {
		try {
			this.timestamp = this.readLong();
			this.magic = this.readMagic();
			this.pingId = this.readLong();
			this.connectionType = this.readConnectionType();
		} catch (RakNetException e) {
			this.timestamp = 0;
			this.magic = false;
			this.pingId = 0;
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
