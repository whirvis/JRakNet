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
package net.marfgamer.jraknet.example.chat;

/**
 * The list of packet ID's used for the chat example protocol
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class ChatMessageIdentifier {

	/**
	 * Sent from the client to the server during login.<br>
	 * <br>
	 * <code>
	 * String: username
	 * </code>
	 */
	public static final int ID_LOGIN_REQUEST = 0x86;

	/**
	 * Sent from the server to let the client know the login was accepted.<br>
	 * <br>
	 * <code>
	 * UUID: generated ID for the client <br>
	 * int: channelCount <br>
	 * &emsp;unsigned byte: channel <br>
	 * &emsp;String: channel <br>
	 * int: userCount<br>
	 * &emsp;String: username<br>
	 * String: message of the day
	 * </code>
	 */
	public static final int ID_LOGIN_ACCEPTED = 0x87;

	/**
	 * Sent from the server to let the client know the login failed.<br>
	 * <br>
	 * <code>
	 * String: reason for connection failure
	 * </code>
	 */
	public static final int ID_LOGIN_FAILURE = 0x88;

	/**
	 * Sent from the client and server to send a chat message.<br>
	 * <br>
	 * <code>
	 * String: message
	 * </code>
	 */
	public static final int ID_CHAT_MESSAGE = 0x89;

	/**
	 * Sent from the client to request a new username.<br>
	 * <br>
	 * <code>
	 * String: username
	 * </code>
	 */
	public static final int ID_UPDATE_USERNAME_REQUEST = 0x90;

	/**
	 * Sent from the server to let the client know it's new username was accepted.
	 */
	public static final int ID_UPDATE_USERNAME_ACCEPTED = 0x91;

	/**
	 * Sent from the server to let the client know it's new username was denied.
	 */
	public static final int ID_UPDATE_USERNAME_FAILURE = 0x92;

	/**
	 * Adds a channel to the list of current channels.<br>
	 * <br>
	 * <code>
	 * unsigned byte: channel
	 * <br>
	 * String: channel name
	 * </code>
	 */
	public static final int ID_ADD_CHANNEL = 0x93;

	/**
	 * Sent from the server to tell the client the channel has been renamed.<br>
	 * <br>
	 * <code>
	 * unsigned byte: channel
	 * <br>
	 * String: new channel name
	 * </code>
	 */
	public static final int ID_RENAME_CHANNEL = 0x94;

	/**
	 * Removes a channel from the list of current channels.<br>
	 * <br>
	 * <code>
	 * unsigned byte: channel
	 * </code>
	 */
	public static final int ID_REMOVE_CHANNEL = 0x95;

	/**
	 * Sent by the server to let the client know it's been kicked.<br>
	 * <br>
	 * <code>
	 * String: reason for kick
	 * </code>
	 */
	public static final int ID_KICK = 0x96;

}
