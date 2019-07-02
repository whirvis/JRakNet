/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
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
package com.whirvis.jraknet.chat.protocol;

import com.whirvis.jraknet.Packet;

/**
 * A <code>LOGIN_REQUEST</code> packet.
 * <p>
 * This packet is sent by the {@link com.whirvis.jraknet.chat.client.ChatClient
 * ChatClient} when it wishes to connect to a
 * {@link com.whirvis.jraknet.chat.server.ChatServer ChatServer}.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class LoginRequest extends ChatPacket {

	/**
	 * The username.
	 */
	public String username;

	/**
	 * Creats a <code>LOGIN_REQUEST</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public LoginRequest() {
		super(ID_LOGIN_REQUEST);
	}

	/**
	 * Creates an <code>LOGIN_REQUEST</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public LoginRequest(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeString(username);
	}

	@Override
	public void decode() {
		this.username = this.readString();
	}

}
