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
import com.whirvis.jraknet.RakNetPacket;

/**
 * A generic chat packet that has the ability to ge the ID of the packet along
 * with encoding and decoding.
 *
 * @author Trent Summerlin
 * @since JRakNet v2.0.0
 */
public class ChatPacket extends RakNetPacket {

	/**
	 * The ID of the {@link LoginRequest LOGIN_REQUEST} packet.
	 */
	public static final int ID_LOGIN_REQUEST = 0x86;

	/**
	 * The ID of the {@link LoginAccepted LOGIN_ACCEPTED} packet.
	 */
	public static final int ID_LOGIN_ACCEPTED = 0x87;

	/**
	 * The ID of the {@link LoginFailure LOGIN_FAILURE} packet.
	 */
	public static final int ID_LOGIN_FAILURE = 0x88;

	/**
	 * The ID of the {@link ChatMessage CHAT_MESSAGE} packet.
	 */
	public static final int ID_CHAT_MESSAGE = 0x89;

	/**
	 * The ID of the {@link UpdateUsernameRequest UPDATE_USERNAME_REQUEST}
	 * packet.
	 */
	public static final int ID_UPDATE_USERNAME_REQUEST = 0x90;

	/**
	 * The ID of the <code>UPDATE_USERNAME_ACCEPTED</code> packet.
	 */
	public static final int ID_UPDATE_USERNAME_ACCEPTED = 0x91;

	/**
	 * The ID of the <code>UPDATE_USERNAME_FAILURE</code> packet.
	 */
	public static final int ID_UPDATE_USERNAME_FAILURE = 0x92;

	/**
	 * The ID of the {@link AddChannel ADD_CHANNEL} packet.
	 */
	public static final int ID_ADD_CHANNEL = 0x93;

	/**
	 * The ID of the {@link RenameChannel RENAME_CHANNEL} packet.
	 */
	public static final int ID_RENAME_CHANNEL = 0x94;

	/**
	 * The ID of the {@link RemoveChannel REMOVE_CHANNEL} packet.
	 */
	public static final int ID_REMOVE_CHANNEL = 0x95;

	/**
	 * The ID of the {@link Kick KICK} packet.
	 */
	public static final int ID_KICK = 0x96;

	/**
	 * Creates a chat packet.
	 * 
	 * @param id
	 *            The ID of the packet.
	 * @throws IllegalArgumentException
	 *             if the <code>id</code> is not in between
	 *             <code>{@value RakNetPacket#ID_USER_PACKET_ENUM}-255</code>.
	 */
	protected ChatPacket(int id) throws IllegalArgumentException {
		super(id);
		if (id < RakNetPacket.ID_USER_PACKET_ENUM) {
			throw new IllegalArgumentException("Packet ID must be in between " + ID_USER_PACKET_ENUM + "-255");
		}
	}

	/**
	 * Creates a chat packet.
	 * 
	 * @param packet
	 *            the packet to read from and write to.
	 */
	protected ChatPacket(Packet packet) {
		super(packet);
	}

}
