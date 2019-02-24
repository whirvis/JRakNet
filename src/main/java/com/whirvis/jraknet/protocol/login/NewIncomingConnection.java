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
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
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
package com.whirvis.jraknet.protocol.login;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.Failable;

/**
 * A <code>NEW_INCOMING_CONNECTION</code> packet.
 * <p>
 * This is sent by the client after receiving the
 * {@link ConnectionRequestAccepted CONNECTION_REQUEST_ACCEPTED} packet.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public class NewIncomingConnection extends RakNetPacket implements Failable {

	// TODO: Figure out what the unknown addresses are used for

	/**
	 * The server address.
	 */
	public InetSocketAddress serverAddress;

	/**
	 * The server timestamp.
	 */
	public long serverTimestamp;

	/**
	 * The client timestamp.
	 */
	public long clientTimestamp;

	/**
	 * Whether or not the packet failed to encode/decode.
	 */
	private boolean failed;

	/**
	 * Creates a <code>NEW_INCOMING_CONNECTION</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public NewIncomingConnection() {
		super(ID_NEW_INCOMING_CONNECTION);
	}

	/**
	 * Creates a <code>NEW_INCOMING_CONNECTION</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public NewIncomingConnection(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		try {
			this.writeAddress(serverAddress);
			for (int i = 0; i < 10; i++) {
				this.writeAddress("0.0.0.0", 0);
			}
			this.writeLong(serverTimestamp);
			this.writeLong(clientTimestamp);
		} catch (UnknownHostException e) {
			this.failed = true;
			this.serverAddress = null;
			this.serverTimestamp = 0;
			this.clientTimestamp = 0;
			this.clear();
		}
	}

	@Override
	public void decode() {
		try {
			this.serverAddress = this.readAddress();
			for (int i = 0; i < 10; i++) {
				this.readAddress(); // Ignore, unknown use
			}
			this.serverTimestamp = this.readLong();
			this.clientTimestamp = this.readLong();
		} catch (UnknownHostException e) {
			this.failed = true;
			this.serverAddress = null;
			this.serverTimestamp = 0;
			this.clientTimestamp = 0;
			this.clear();
		}
	}

	@Override
	public boolean failed() {
		return this.failed;
	}

}
