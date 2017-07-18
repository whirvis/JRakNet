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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet.protocol.status;

import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.identifier.Identifier;
import net.marfgamer.jraknet.protocol.MessageIdentifier;

public class UnconnectedPong extends RakNetPacket {

	public long pingId;
	public long pongId;
	public boolean magic;
	public Identifier identifier;

	public UnconnectedPong() {
		super(MessageIdentifier.ID_UNCONNECTED_PONG);
	}

	public UnconnectedPong(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeLong(pingId);
		this.writeLong(pongId);
		this.writeMagic();
		this.writeString(identifier.build());
	}

	@Override
	public void decode() {
		this.pingId = this.readLong();
		this.pongId = this.readLong();
		this.magic = this.checkMagic();

		this.identifier = new Identifier(this.readString());
	}

}
