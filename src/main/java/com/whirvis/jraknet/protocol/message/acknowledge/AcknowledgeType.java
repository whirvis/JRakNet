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
package com.whirvis.jraknet.protocol.message.acknowledge;

/**
 * Used by <code>Acknowledge</code> to show what type a set of
 * <code>Record</code>s is.
 *
 * @author Whirvis T. Wheatley
 */
public enum AcknowledgeType {

	ACKNOWLEDGED(Acknowledge.ACKNOWLEDGED), NOT_ACKNOWLEDGED(Acknowledge.NOT_ACKNOWLEDGED);

	public short id;

	private AcknowledgeType(short id) {
		this.id = id;
	}

	/**
	 * @return the ID of the acknowledge type.
	 */
	public short getId() {
		return this.id;
	}

	/**
	 * @param id
	 *            the ID of the acknowledge receipt type to lookup.
	 * @return an <code>AcknowledgeType</code> based on the ID.
	 */
	public static AcknowledgeType lookup(short id) {
		for (AcknowledgeType type : AcknowledgeType.values()) {
			if (type.getId() == id) {
				return type;
			}
		}
		return null;
	}

}
