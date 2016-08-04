/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Trent Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
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
package net.marfgamer.raknet.protocol.raknet;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.protocol.Message;

public class ConnectedServerHandshake extends Message {

	public InetSocketAddress clientAddress;
	public long timestamp;
	public long serverTimestamp;

	public ConnectedServerHandshake(Message packet) {
		super(packet);
	}

	public ConnectedServerHandshake() {
		super(ID_CONNECTED_SERVER_HANDSHAKE);
	}

	@Override
	public void encode() {
		this.putAddress(clientAddress);
		this.putShort(0);
		for (int i = 0; i < 10; i++) {
			this.putAddress("255.255.255.255", 19132);
		}
		this.putLong(timestamp);
		this.putLong(serverTimestamp);
	}

	@Override
	public void decode() {
		this.clientAddress = this.getAddress();
		this.getShort(); // Unknown use
		for (int i = 0; i < 10; i++) {
			this.getAddress(); // Unknown use
		}
		this.timestamp = this.getLong();
		this.serverTimestamp = this.getLong();
	}

}
