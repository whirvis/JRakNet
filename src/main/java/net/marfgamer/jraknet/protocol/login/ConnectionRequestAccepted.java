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
 * Copyright (c) 2016-2018 Trent "MarfGamer" Summerlin
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
package net.marfgamer.jraknet.protocol.login;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.Failable;
import net.marfgamer.jraknet.protocol.MessageIdentifier;

public class ConnectionRequestAccepted extends RakNetPacket implements Failable {

	public InetSocketAddress clientAddress;
	public long clientTimestamp;
	public long serverTimestamp;
	private boolean failed;

	public ConnectionRequestAccepted() {
		super(MessageIdentifier.ID_CONNECTION_REQUEST_ACCEPTED);
	}

	public ConnectionRequestAccepted(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		try {
			this.writeAddress(clientAddress);
			this.writeShort(0);
			for (int i = 0; i < 10; i++) {
				this.writeAddress("0.0.0.0", 0);
			}
			this.writeLong(clientTimestamp);
			this.writeLong(serverTimestamp);
		} catch (UnknownHostException e) {
			this.failed = true;
			this.clientAddress = null;
			this.clientTimestamp = 0;
			this.serverTimestamp = 0;
			this.clear();
		}
	}

	@Override
	public void decode() {
		try {
			this.clientAddress = this.readAddress();
			this.readShort(); // Unknown use
			for (int i = 0; i < 10; i++) {
				this.readAddress(); // Unknown use
			}
			this.clientTimestamp = this.readLong();
			this.serverTimestamp = this.readLong();
		} catch (UnknownHostException e) {
			this.failed = true;
			this.clientAddress = null;
			this.clientTimestamp = 0;
			this.serverTimestamp = 0;
			this.clear();
		}
	}

	@Override
	public boolean failed() {
		return this.failed;
	}

}
