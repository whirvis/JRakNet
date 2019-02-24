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
package com.whirvis.jraknet.protocol.login;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;

/**
 * A <code>CONNECTION_REQUEST</code> packet.
 * <p>
 * This is the first packet sent by the client during login after initial
 * connection has succeeded.
 * 
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public class ConnectionRequest extends RakNetPacket {

	/**
	 * The client's globally unique ID.
	 */
	public long clientGuid;

	/**
	 * The client's timestamp.
	 */
	public long timestamp;

	/**
	 * Whether or not security should be used. Since JRakNet does not have this
	 * feature implemented, <code>false</code> will always be the value used
	 * when sending this value. However, this value can be <code>true</code> if
	 * it is being set through decoding.
	 */
	public boolean useSecurity;

	/**
	 * Creates a <code>CONNECTION_REQUEST</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public ConnectionRequest() {
		super(ID_CONNECTION_REQUEST);
	}

	/**
	 * Creates a <code>CONNECTION_REQUEST</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public ConnectionRequest(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.useSecurity = false; // TODO: Not supported
		this.writeLong(clientGuid);
		this.writeLong(timestamp);
		this.writeBoolean(useSecurity);
	}

	@Override
	public void decode() {
		this.clientGuid = this.readLong();
		this.timestamp = this.readLong();
		this.useSecurity = this.readBoolean();
	}

}
