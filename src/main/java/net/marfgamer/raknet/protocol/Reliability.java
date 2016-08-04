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
 * Copyright (c) 2016 Trent Summerlin
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
package net.marfgamer.raknet.protocol;

/**
 * Contains all the reliability types for RakNet.
 * 
 * @author Trent Summerlin
 */
public enum Reliability {

	// For the love of god, do not CTRL+SHIFT+F this
	UNRELIABLE(0, false, false, false),
	UNRELIABLE_SEQUENCED(1, false, false, true),
	RELIABLE(2, true, false, false),
	RELIABLE_ORDERED(3, true, true, false),
	RELIABLE_SEQUENCED(4, true, false, true),
	UNRELIABLE_WITH_ACK_RECEIPT(5, false, false, false),
	RELIABLE_WITH_ACK_RECEIPT(6, true, false, false),
	RELIABLE_ORDERED_WITH_ACK_RECEIPT(7, true, true, false);

	public static interface INTERFACE {
		
		public static final Reliability UNRELIABLE = Reliability.UNRELIABLE;
		public static final Reliability UNRELIABLE_SEQUENCED = Reliability.UNRELIABLE_SEQUENCED;
		public static final Reliability RELIABLE = Reliability.RELIABLE;
		public static final Reliability RELIABLE_ORDERED = Reliability.RELIABLE_ORDERED;
		public static final Reliability RELIABLE_SEQUENCED = Reliability.RELIABLE_SEQUENCED;
		public static final Reliability UNRELIABLE_WITH_ACK_RECEIPT = Reliability.UNRELIABLE_WITH_ACK_RECEIPT;
		public static final Reliability RELIABLE_WITH_ACK_RECEIPT = Reliability.RELIABLE_WITH_ACK_RECEIPT;
		public static final Reliability RELIABLE_ORDERED_WITH_ACK_RECEIPT = Reliability.RELIABLE_ORDERED_WITH_ACK_RECEIPT;
	
	}

	private final byte reliability;
	private final boolean reliable;
	private final boolean ordered;
	private final boolean sequenced;

	private Reliability(int reliability, boolean reliable, boolean ordered, boolean sequenced) {
		this.reliability = (byte) reliability;
		this.reliable = reliable;
		this.ordered = ordered;
		this.sequenced = sequenced;
	}

	public byte asByte() {
		return this.reliability;
	}

	public boolean isReliable() {
		return this.reliable;
	}

	public boolean isOrdered() {
		return this.ordered;
	}

	public boolean isSequenced() {
		return this.sequenced;
	}

	public static Reliability lookup(byte reliability) {
		Reliability[] reliabilities = Reliability.values();
		for (Reliability sReliability : reliabilities) {
			if (sReliability.asByte() == reliability) {
				return sReliability;
			}
		}
		return null;
	}

}
