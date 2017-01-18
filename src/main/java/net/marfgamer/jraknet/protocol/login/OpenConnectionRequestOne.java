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
 * Copyright (c) 2016, 2017 MarfGamer
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

import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.MessageIdentifier;

public class OpenConnectionRequestOne extends RakNetPacket {

    public static final int MTU_PADDING = 18; // 1 for ID, 1 for protocol
					      // version, 16 for magic

    public boolean magic;
    public int protocolVersion;
    public int maximumTransferUnit;

    public OpenConnectionRequestOne(Packet packet) {
	super(packet);
    }

    public OpenConnectionRequestOne() {
	super(MessageIdentifier.ID_OPEN_CONNECTION_REQUEST_1);
    }

    @Override
    public void encode() {
	this.writeMagic();
	this.writeUByte(protocolVersion);
	this.pad(maximumTransferUnit - MTU_PADDING);
    }

    @Override
    public void decode() {
	this.magic = this.checkMagic();
	this.protocolVersion = this.readUByte();
	this.maximumTransferUnit = (this.remaining() + MTU_PADDING);
	this.read(this.remaining()); // Go ahead and get rid of those bytes
    }

}
