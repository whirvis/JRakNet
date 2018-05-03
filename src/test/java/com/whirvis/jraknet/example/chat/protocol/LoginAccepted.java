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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.example.chat.protocol;

import java.util.UUID;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.example.chat.ChatMessageIdentifier;
import com.whirvis.jraknet.example.chat.ServerChannel;

public class LoginAccepted extends ChatPacket {

	public UUID userId;
	public String serverName;
	public String serverMotd;
	public ServerChannel[] channels;

	public LoginAccepted() {
		super(ChatMessageIdentifier.ID_LOGIN_ACCEPTED);
	}

	public LoginAccepted(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeUUID(userId);
		this.writeString(serverName);
		this.writeString(serverMotd);
		this.writeInt(channels.length);
		for (int i = 0; i < channels.length; i++) {
			this.writeUnsignedByte(channels[i].getChannel());
			this.writeString(channels[i].getName());
		}
	}

	@Override
	public void decode() {
		this.userId = this.readUUID();
		this.serverName = this.readString();
		this.serverMotd = this.readString();
		this.channels = new ServerChannel[this.readInt()];
		for (int i = 0; i < channels.length; i++) {
			short channel = this.readUnsignedByte();
			String channelName = this.readString();
			channels[i] = new ServerChannel(channel, channelName);
		}
	}

}
