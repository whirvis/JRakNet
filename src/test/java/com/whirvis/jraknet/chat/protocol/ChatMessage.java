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
package com.whirvis.jraknet.chat.protocol;

import com.whirvis.jraknet.Packet;

/**
 * A <code>CHAT_MESSAGE</code> packet.
 * <p>
 * This packet is sent by the {@link com.whirvis.jraknet.chat.client.ChatClient
 * ChatClient} to indicate it wishes to send a chat message. When sent by the
 * {@link com.whirvis.jraknet.chat.server.ChatServer ChatServer}, it is to
 * indicate to that a message has been sent.
 * 
 * @author Trent Summerlin
 * @since JRakNet v2.0.0
 */
public final class ChatMessage extends ChatPacket {

	/**
	 * The chat message text.
	 */
	public String message;

	/**
	 * Creates a <code>CHAT_MESSAGE</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public ChatMessage() {
		super(ID_CHAT_MESSAGE);
	}

	/**
	 * Creates a <code>CHAT_MESSAGE</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public ChatMessage(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeString(message);
	}

	@Override
	public void decode() {
		this.message = this.readString();
	}

}
