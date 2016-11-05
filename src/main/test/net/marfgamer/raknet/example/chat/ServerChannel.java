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
 * Copyright (c) 2016 MarfGamer
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
package net.marfgamer.raknet.example.chat;

/**
 * Represents a channel on a <code>ChatServer</code>, used by both the
 * <code>ChatServer</code> and <code>ChatClient</code> to manage the chat rooms
 *
 * @author MarfGamer
 */
public class ServerChannel {

	private final int channel;
	private String name;
	private final StringBuilder channelText;

	public ServerChannel(int channel, String name) {
		this.channel = channel;
		this.name = name;
		this.channelText = new StringBuilder();
	}

	/**
	 * Returns the ID of the channel
	 * 
	 * @return The ID of the channel
	 */
	public int getChannel() {
		return this.channel;
	}

	/**
	 * Returns the name of the channel
	 * 
	 * @return The name of the channel
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the channel
	 * 
	 * @param name
	 *            - The new channel name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Adds a chat message to the server channel
	 * 
	 * @param text
	 *            - The message to append
	 */
	public void appendText(String text) {
		channelText.append(text + "\n");
	}

	/**
	 * Returns the text displayed on the channel
	 * 
	 * @return The text displayed on the channel
	 */
	public String getChannelText() {
		return channelText.toString();
	}

	@Override
	public String toString() {
		return this.name;
	}

}