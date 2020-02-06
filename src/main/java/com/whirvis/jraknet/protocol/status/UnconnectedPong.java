/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 Trent Summerlin
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
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.Failable;

/**
 * An <code>UNCONNECTED_PONG</code> packet.
 * <p>
 * This packet is sent in response to {@link UnconnectedPing UNCONNECTED_PING}
 * and {@link UnconnectedPingOpenConnections UNCONNECTED_PING_OPEN_CONNECTIONS}
 * packets in order to give the client server information and show that it is
 * online.
 * 
 * @author Trent Summerlin
 * @since JRakNet 1.0.0
 */
public final class UnconnectedPong extends RakNetPacket implements Failable {

	/**
	 * The timestamp sent in the ping packet.
	 */
	public long timestamp;

	/**
	 * The server's pong ID.
	 */
	public long pongId;

	/**
	 * Whether or not the magic bytes read in the packet are valid.
	 */
	public boolean magic;

	/**
	 * The server's identifier.
	 */
	public Identifier identifier;

	/**
	 * The server's connection type.
	 */
	public ConnectionType connectionType;

	/**
	 * Whether or not the packet failed to encode/decode.
	 */
	private boolean failed;

	/**
	 * Creates an <code>UNCONNECTED_PONG</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public UnconnectedPong() {
		super(ID_UNCONNECTED_PONG);
	}

	/**
	 * Creates an <code>UNCONNECTED_PONG</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public UnconnectedPong(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		try {
			this.writeLong(timestamp);
			this.writeLong(pongId);
			this.writeMagic();
			this.writeString(identifier.build());
			this.writeConnectionType(connectionType);
		} catch (RakNetException e) {
			this.timestamp = 0;
			this.pongId = 0;
			this.magic = false;
			this.identifier = null;
			this.connectionType = null;
			this.clear();
			this.failed = true;
		}
	}

	@Override
	public void decode() {
		try {
			this.timestamp = this.readLong();
			this.pongId = this.readLong();
			this.magic = this.readMagic();
			this.identifier = new Identifier(this.readString(), this.connectionType = this.readConnectionType());
		} catch (RakNetException e) {
			this.timestamp = 0;
			this.pongId = 0;
			this.magic = false;
			this.identifier = null;
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
