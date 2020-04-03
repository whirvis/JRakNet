/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 "Whirvis" Trent Summerlin
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
package com.whirvis.jraknet;

/**
 * Signals that a packet channel in a sent or received packet is higher than the
 * limit of {@value RakNet#CHANNEL_COUNT}.
 *
 * @author "Whirvis" Trent Summerlin
 * @since JRakNet v1.0.0
 */
public final class InvalidChannelException extends IllegalArgumentException {

	private static final long serialVersionUID = -8690545139286694469L;

	private final int channel;

	/**
	 * Constructs an <code>InvalidChannelException</code>.
	 * 
	 * @param channel
	 *            the invalid channel that was used.
	 */
	public InvalidChannelException(int channel) {
		super("Invalid channel " + channel + ", must be lower than " + RakNet.CHANNEL_COUNT);
		this.channel = channel;
	}

	/**
	 * Returns the invalid channel that was used.
	 * 
	 * @return the invalid channel that was used.
	 */
	public int getChannel() {
		return this.channel;
	}

}
