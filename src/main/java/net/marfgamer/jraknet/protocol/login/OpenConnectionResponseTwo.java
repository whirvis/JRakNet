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
 * Copyright (c) 2016, 2017 Trent "MarfGamer" Summerlin
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

import static net.marfgamer.jraknet.protocol.MessageIdentifier.JRAKNET_MAGIC;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.Failable;
import net.marfgamer.jraknet.protocol.MessageIdentifier;

public class OpenConnectionResponseTwo extends RakNetPacket implements Failable {

	public boolean magic;
	public long serverGuid;
	public InetSocketAddress clientAddress;
	public int maximumTransferUnit;
	public boolean encryptionEnabled;
	public boolean isJraknet;
	private boolean failed;

	public OpenConnectionResponseTwo(Packet packet) {
		super(packet);
	}

	public OpenConnectionResponseTwo() {
		super(MessageIdentifier.ID_OPEN_CONNECTION_REPLY_2);
	}

	@Override
	public void encode() {
		this.isJraknet = true;
		try {
			this.writeMagic();
			this.writeLong(serverGuid);
			this.writeAddress(clientAddress);
			this.writeUShort(maximumTransferUnit);
			this.writeBoolean(encryptionEnabled);
			this.write(JRAKNET_MAGIC);
		} catch (UnknownHostException e) {
			this.failed = true;
			this.magic = false;
			this.serverGuid = 0;
			this.clientAddress = null;
			this.maximumTransferUnit = 0;
			this.encryptionEnabled = false;
			this.isJraknet = false;
			this.clear();
		}
	}

	@Override
	public void decode() {
		try {
			this.magic = this.checkMagic();
			this.serverGuid = this.readLong();
			this.clientAddress = this.readAddress();
			this.maximumTransferUnit = this.readUShort();
			this.encryptionEnabled = this.readBoolean();
			if (this.remaining() >= JRAKNET_MAGIC.length) {
				byte[] jraknetMagic = this.read(JRAKNET_MAGIC.length);
				this.isJraknet = Arrays.equals(jraknetMagic, JRAKNET_MAGIC);
			}
		} catch (UnknownHostException e) {
			this.failed = true;
			this.magic = false;
			this.serverGuid = 0;
			this.clientAddress = null;
			this.maximumTransferUnit = 0;
			this.encryptionEnabled = false;
			this.isJraknet = false;
			this.clear();
		}
	}

	@Override
	public boolean failed() {
		return this.failed;
	}

}
