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
package net.marfgamer.jraknet.example.chat;

import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.session.InvalidChannelException;

/**
 * Represents a channel on a <code>ChatServer</code>, used by both the
 * <code>ChatServer</code> and <code>ChatClient</code> to manage the chat rooms.
 *
 * @author Trent "MarfGamer" Summerlin
 */
public class ServerChannel {

	private final int channel;
	private String name;
	private final StringBuilder channelText;

	/**
	 * Constructs a <code>ServerChannel</code> with the specified channel and name.
	 * 
	 * @param channel
	 *            the channel.
	 * @param name
	 *            the name.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public ServerChannel(int channel, String name) throws InvalidChannelException {
		this.channel = channel;
		this.name = name;
		this.channelText = new StringBuilder();

		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}
	}

	/**
	 * @return the ID of the channel.
	 */
	public int getChannel() {
		return this.channel;
	}

	/**
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
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Adds a chat message to the server channel.
	 * 
	 * @param message
	 *            the message to add.
	 */
	public void addChatMessage(String message) {
		channelText.append(message + "\n");
	}

	/**
	 * @return the text displayed on the channel.
	 */
	public String getChannelText() {
		return channelText.toString();
	}

	@Override
	public String toString() {
		return this.name;
	}

}
