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
package net.marfgamer.jraknet.protocol;

/**
 * Contains all the reliability types for RakNet.
 * 
 * @author Whirvis "MarfGamer" Ardenaur
 */
public enum Reliability {

	/**
	 * The packet packet will be sent, but it is not guaranteed that it will be
	 * received.
	 */
	UNRELIABLE(0, false, false, false, false),

	/**
	 * Same as <code>UNRELIABLE</code>, however it will not be handled if a newer
	 * sequenced packet on the channel has already arrived.
	 */
	UNRELIABLE_SEQUENCED(1, false, false, true, false),

	/**
	 * The packet will be sent and is guaranteed to be received at some point.
	 */
	RELIABLE(2, true, false, false, false),

	/**
	 * Same as <code>RELIABLE</code>, however it will not be handled until all
	 * packets sent before it are also received.
	 */
	RELIABLE_ORDERED(3, true, true, false, false),

	/**
	 * Same as <code>RELIABLE</code>, however it will not be handled if a newer
	 * sequenced packet on the channel has already arrived.
	 */
	RELIABLE_SEQUENCED(4, true, false, true, false),

	/**
	 * TODO
	 */
	UNRELIABLE_WITH_ACK_RECEIPT(5, false, false, false, true),

	/**
	 * TODO
	 */
	RELIABLE_WITH_ACK_RECEIPT(6, true, false, false, true),

	/**
	 * TODO
	 */
	RELIABLE_ORDERED_WITH_ACK_RECEIPT(7, true, true, false, true);

	private final byte reliability;
	private final boolean reliable;
	private final boolean ordered;
	private final boolean sequenced;
	private final boolean requiresAck;

	/**
	 * Constructs a <code>Reliability</code> with the specified reliability and
	 * whether or not it is reliable, ordered, sequenced, or requires an acknowledge
	 * receipt.
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
	 * @return the ID of the reliability as a byte.
	 */
	public byte getId() {
		return this.reliability;
	}

	/**
	 * @return <code>true</code> if the reliability is reliable.
	 */
	public boolean isReliable() {
		return this.reliable;
	}

	/**
	 * @return <code>true</code> if the reliability is ordered.
	 */
	public boolean isOrdered() {
		return this.ordered;
	}

	/**
	 * @return <code>true</code> if the reliability is sequenced.
	 */
	public boolean isSequenced() {
		return this.sequenced;
	}

	/**
	 * @return <code>true</code> if the reliability requires acknowledgement.
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
			if (sReliability.getId() == reliability) {
				return sReliability;
			}
		}
		return null;
	}

}
