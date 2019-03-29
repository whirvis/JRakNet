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
 * Copyright (c) 2016-2019 Trent Summerlin
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
package com.whirvis.jraknet.chat;

import java.util.Objects;

import com.whirvis.jraknet.InvalidChannelException;
import com.whirvis.jraknet.RakNet;

/**
 * Represents a channel on a {@link com.whirvis.jraknet.chat.server.ChatServer
 * ChatServer}.
 *
 * @author Trent Summerlin
 * @since JRakNet v2.0.0
 */
public final class TextChannel {

	private final int channel;
	private String name;
	private final StringBuilder channelText;

	/**
	 * Creates a server channel.
	 * 
	 * @param channel
	 *            the channel.
	 * @param name
	 *            the name.
	 * @throws InvalidChannelException
	 *             if the channel is higher than {@value RakNet#MAX_CHANNELS}.
	 */
	public TextChannel(int channel, String name) throws InvalidChannelException {
		this.channel = channel;
		this.name = name;
		this.channelText = new StringBuilder();
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException(channel);
		}
	}

	/**
	 * Returns the ID of the channel.
	 * 
	 * @return the ID of the channel.
	 */
	public int getChannel() {
		return this.channel;
	}

	/**
	 * Returns the name of the channel.
	 * 
	 * @return the name of the channel.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the channel.
	 * 
	 * @param name
	 *            the new channel name.
	 * @throws NullPointerException
	 *             if the <code>name</code> is <code>null</code>.
	 */
	public void setName(String name) throws NullPointerException {
		if (name == null) {
			throw new NullPointerException("Name cannot be null");
		}
		this.name = name;
	}

	/**
	 * Adds a chat message to the server channel.
	 * 
	 * @param message
	 *            the message to add.
	 * @throws NullPointerException
	 *             if the <code>message</code> is <code>null</code>.
	 */
	public void addChatMessage(String message) throws NullPointerException {
		if (message == null) {
			throw new NullPointerException("Message cannot be null");
		}
		channelText.append(message + "\n");
	}

	/**
	 * Returns the text currently displayed on the channel.
	 * 
	 * @return the text currently displayed on the channel.
	 */
	public String getText() {
		return channelText.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(channel, name);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TextChannel) {
			TextChannel tc = (TextChannel) o;
			return Objects.equals(channel, tc.channel) && Objects.equals(name, tc.name);
		}
		return false;
	}

	@Override
	public String toString() {
		return "ServerChannel [channel=" + channel + ", name=" + name + ", channelText=" + channelText + "]";
	}

}
