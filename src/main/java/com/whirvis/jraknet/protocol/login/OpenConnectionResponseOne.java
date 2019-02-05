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
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
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
import com.whirvis.jraknet.protocol.MessageIdentifier;

public class OpenConnectionResponseOne extends RakNetPacket {

	public static final byte USE_SECURITY_BIT = 0x01;

	public boolean magic;
	public long serverGuid;
	public int maximumTransferUnit;

	/*
	 * JRakNet does not support RakNet's built in security function, it is
	 * poorly documented
	 */
	public boolean useSecurity = false;

	public OpenConnectionResponseOne(Packet packet) {
		super(packet);
	}

	public OpenConnectionResponseOne() {
		super(MessageIdentifier.ID_OPEN_CONNECTION_REPLY_1);
	}

	@Override
	public void encode() {
		this.writeMagic();
		this.writeLong(serverGuid);

		// Set security flags
		byte securityFlags = 0x00;
		securityFlags |= (useSecurity ? USE_SECURITY_BIT : 0x00);
		this.writeUnsignedByte(securityFlags);
		this.writeUnsignedShort(maximumTransferUnit);
	}

	@Override
	public void decode() {
		this.magic = this.checkMagic();
		this.serverGuid = this.readLong();

		byte securityFlags = 0x00;
		securityFlags |= this.readUnsignedByte(); // Use security
		if ((securityFlags & USE_SECURITY_BIT) == USE_SECURITY_BIT) {
			this.useSecurity = true;
		}
		this.maximumTransferUnit = this.readUnsignedShort();
	}

}
