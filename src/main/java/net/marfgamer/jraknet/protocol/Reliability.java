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

import net.marfgamer.jraknet.util.ArrayUtils;

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
	 * Same as <code>UNRELIABLE</code>, however you will be notified whether the
	 * packet was lost or received through <code>onAcknowledge()</code> and
	 * <code>onNotAcknowledge()</code> methods through the
	 * <code>RakNetServerListener</code> and <code>RakNetClientListener</code>
	 * classes.
	 */
	UNRELIABLE_WITH_ACK_RECEIPT(UNRELIABLE.id, false, false, false, true),

	/**
	 * Same as <code>UNRELIABLE_SEQUENCED</code>, however you will be notified
	 * whether the packet was lost or received through <code>onAcknowledge()</code>
	 * and <code>onNotAcknowledge()</code> methods through the
	 * <code>RakNetServerListener</code> and <code>RakNetClientListener</code>
	 * classes.
	 */
	UNRELIABLE_SEQUENCED_WITH_ACK_RECEIPT(UNRELIABLE_SEQUENCED.id, false, false, true, true),

	/**
	 * Same as <code>RELIABLE</code>, however you will be notified when the packet
	 * was received through the <code>onAcknowledge()</code> method through the
	 * <code>RakNetServerListener</code> and <code>RakNetClientListener</code>
	 * classes.
	 */
	RELIABLE_WITH_ACK_RECEIPT(RELIABLE.id, true, false, false, true),

	/**
	 * Same as <code>RELIABLE_SEQUENCED</code>, however you will be notified when
	 * the packet was received through the <code>onAcknowledge()</code> method
	 * through the <code>RakNetServerListener</code> and
	 * <code>RakNetClientListener</code> classes.
	 */
	RELIABLE_SEQUENCED_WITH_ACK_RECEIPT(RELIABLE_SEQUENCED.id, true, false, true, true),

	/**
	 * Same as <code>RELIABLE_ORDERED</code>, however you will be notified when the
	 * packet was received through the <code>onAcknowledge()</code> method through
	 * the <code>RakNetServerListener</code> and <code>RakNetClientListener</code>
	 * classes.
	 */
	RELIABLE_ORDERED_WITH_ACK_RECEIPT(RELIABLE_ORDERED.id, true, true, false, true);

	private final byte id;
	private final boolean reliable;
	private final boolean ordered;
	private final boolean sequenced;
	private final boolean requiresAck;

	/**
	 * Constructs a <code>Reliability</code> with the specified ID and whether or
	 * not it is reliable, ordered, sequenced, or requires an acknowledge receipt.
	 * 
	 * @param id
	 *            the ID of the reliability.
	 * @param reliable
	 *            whether or not it is reliable.
	 * @param ordered
	 *            whether or not it is ordered.
	 * @param sequenced
	 *            whether or not it is sequenced.
	 * @param requiresAck
	 *            whether or not it requires an acknowledge receipt.
	 */
	private Reliability(int id, boolean reliable, boolean ordered, boolean sequenced, boolean requiresAck) {
		this.id = (byte) id;
		this.reliable = reliable;
		this.ordered = ordered;
		this.sequenced = sequenced;
		this.requiresAck = requiresAck;

		// This would cause a logical contradiction
		if (ordered == true && sequenced == true) {
			throw new IllegalArgumentException("A reliability cannot be both ordered and sequenced");
		}
	}

	/**
	 * @return the ID of the reliability as a byte.
	 */
	public byte getId() {
		return this.id;
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

	@Override
	public String toString() {
		return ArrayUtils.toJRakNetString(this.name(), "id:" + this.id, "reliable:" + this.reliable,
				"ordered:" + this.ordered, "sequenced:" + this.sequenced, "ack:" + this.requiresAck);
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
