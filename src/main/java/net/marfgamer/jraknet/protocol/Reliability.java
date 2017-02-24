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
 * Copyright (c) 2016, 2017 MarfGamer
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
package net.marfgamer.jraknet.protocol;

/**
 * Contains all the reliability types for RakNet.
 * 
 * @author MarfGamer
 */
public enum Reliability {

	UNRELIABLE(0, false, false, false, false), UNRELIABLE_SEQUENCED(1, false, false, true, false),
	RELIABLE(2, true, false, false, false), RELIABLE_ORDERED(3, true, true, false, false),
	RELIABLE_SEQUENCED(4, true, false, true, false), UNRELIABLE_WITH_ACK_RECEIPT(5, false, false, false, true),
	UNRELIABLE_SEQUENCED_WITH_ACK_RECEIPT(6, false, false, true, true),
	RELIABLE_WITH_ACK_RECEIPT(7, true, false, false, true),
	RELIABLE_ORDERED_WITH_ACK_RECEIPT(8, true, true, false, true),
	RELIABLE_SEQUENCED_WITH_ACK_RECEIPT(9, true, false, true, true);

	private final byte reliability;
	private final boolean reliable;
	private final boolean ordered;
	private final boolean sequenced;
	private final boolean requiresAck;

	/**
	 * Constructs a <code>Reliability</code> with the specified reliability and
	 * whether or not it is reliable, ordered, sequenced, or requires an
	 * acknowledge receipt.
	 * 
	 * @param reliability
	 *            the reliability.
	 * @param reliable
	 *            whether or not it is reliable.
	 * @param ordered
	 *            whether or not it is ordered.
	 * @param sequenced
	 *            whether or not it is sequenced.
	 * @param requiresAck
	 *            whether or not it requires an acknowledge receipt.
	 */
	private Reliability(int reliability, boolean reliable, boolean ordered, boolean sequenced, boolean requiresAck) {
		this.reliability = (byte) reliability;
		this.reliable = reliable;
		this.ordered = ordered;
		this.sequenced = sequenced;
		this.requiresAck = requiresAck;
	}

	/**
	 * @return the reliability as a byte.
	 */
	public byte asByte() {
		return this.reliability;
	}

	/**
	 * @return true if the reliability is reliable.
	 */
	public boolean isReliable() {
		return this.reliable;
	}

	/**
	 * @return true if the reliability is ordered.
	 */
	public boolean isOrdered() {
		return this.ordered;
	}

	/**
	 * @return true if the reliability is sequenced.
	 */
	public boolean isSequenced() {
		return this.sequenced;
	}

	/**
	 * @return true if the reliability requires acknowledgement.
	 */
	public boolean requiresAck() {
		return this.requiresAck;
	}

	/**
	 * @param reliability
	 *            the ID of the reliability to lookup.
	 * @return the reliability based on it's ID.
	 */
	public static Reliability lookup(int reliability) {
		Reliability[] reliabilities = Reliability.values();
		for (Reliability sReliability : reliabilities) {
			if (sReliability.asByte() == reliability) {
				return sReliability;
			}
		}
		return null;
	}

}
