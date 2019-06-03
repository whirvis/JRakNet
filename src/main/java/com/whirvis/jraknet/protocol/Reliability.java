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
package com.whirvis.jraknet.protocol;

/**
 * Represents a RakNet reliability. Reliabilities determine how packets are
 * handled when they are being sent or received.
 * 
 * @author Trent Summerlin
 * @since JRakNet v1.0.0
 */
public enum Reliability {

	/**
	 * The packet will be sent, but it is not guaranteed that it will be
	 * received.
	 */
	UNRELIABLE(0, false, false, false, false),

	/**
	 * Same as {@link #UNRELIABLE}, however it will not be handled if a newer
	 * sequenced packet on the channel has already arrived.
	 */
	UNRELIABLE_SEQUENCED(1, false, false, true, false),

	/**
	 * The packet will be sent and is guaranteed to be received at some point.
	 */
	RELIABLE(2, true, false, false, false),

	/**
	 * Same as {@link #RELIABLE}, however it will not be handled until all
	 * packets sent before it are also received.
	 */
	RELIABLE_ORDERED(3, true, true, false, false),

	/**
	 * Same as {@link RELIABLE}, however it will not be handled if a newer
	 * sequenced packet on the channel has already arrived.
	 */
	RELIABLE_SEQUENCED(4, true, false, true, false),

	/**
	 * Same as {@link #UNRELIABLE}, however you will be notified whether the
	 * packet was lost or received through the <code>onAcknowledge()</code> and
	 * <code>onLoss()</code> methods found inside the
	 * <code>RakNetServerListener</code> and <code>RakNetClientListener</code>
	 * classes.
	 * 
	 * @see com.whirvis.jraknet.server.RakNetServerListener RakNetServerListener
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	UNRELIABLE_WITH_ACK_RECEIPT(5, false, false, false, true),

	/**
	 * Same as {@link #RELIABLE}, however you will be notified when the packet
	 * was received through the <code>onAcknowledge()</code> method through the
	 * <code>RakNetServerListener</code> and <code>RakNetClientListener</code>
	 * classes.
	 * 
	 * @see com.whirvis.jraknet.server.RakNetServerListener RakNetServerListener
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	RELIABLE_WITH_ACK_RECEIPT(6, true, false, false, true),

	/**
	 * Same as {@link #RELIABLE_ORDERED}, however you will be notified when the
	 * packet was received through the <code>onAcknowledge()</code> method
	 * through the <code>RakNetServerListener</code> and
	 * <code>RakNetClientListener</code> classes.
	 * 
	 * @see com.whirvis.jraknet.server.RakNetServerListener RakNetServerListener
	 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
	 */
	RELIABLE_ORDERED_WITH_ACK_RECEIPT(7, true, true, false, true);

	private final byte id;
	private final boolean reliable;
	private final boolean ordered;
	private final boolean sequenced;
	private final boolean requiresAck;

	/**
	 * Constructs a <code>Reliability</code>.
	 * 
	 * @param id
	 *            the ID of the reliability.
	 * @param reliable
	 *            <code>true</code> if it is reliable, <code>false</code>
	 *            otherwise.
	 * @param ordered
	 *            <code>true</code> if it is ordered, <code>false</code>
	 *            otherwise.
	 * @param sequenced
	 *            <code>true</code> if it is sequenced, <code>false</code>
	 *            otherwise.
	 * @param requiresAck
	 *            <code>true</code> if it requires an acknowledge receipt,
	 *            <code>false</code> otherwise.
	 * @throws IllegalArgumentException
	 *             if both <code>ordered</code> and <code>sequenced</code> are
	 *             <code>true</code>.
	 */
	private Reliability(int id, boolean reliable, boolean ordered, boolean sequenced, boolean requiresAck) throws IllegalArgumentException {
		this.id = (byte) id;
		this.reliable = reliable;
		this.ordered = ordered;
		this.sequenced = sequenced;
		this.requiresAck = requiresAck;
		if (ordered == true && sequenced == true) {
			throw new IllegalArgumentException("A reliability cannot be both ordered and sequenced");
		}
	}

	/**
	 * Returns the ID of the reliability.
	 * 
	 * @return the ID of the reliability.
	 */
	public byte getId() {
		return this.id;
	}

	/**
	 * Returns whether or not the reliability is reliable.
	 * 
	 * @return <code>true</code> if the reliability is reliable,
	 *         <code>false</code> otherwise.
	 */
	public boolean isReliable() {
		return this.reliable;
	}

	/**
	 * Returns whether or not the reliability is ordered.
	 * 
	 * @return <code>true</code> if the reliability is ordered,
	 *         <code>false</code> otherwise.
	 */
	public boolean isOrdered() {
		return this.ordered;
	}

	/**
	 * Returns whether not the reliability is sequenced.
	 * 
	 * @return <code>true</code> if the reliability is sequenced,
	 *         <code>false</code> otherwise.
	 */
	public boolean isSequenced() {
		return this.sequenced;
	}

	/**
	 * Returns whether or not the reliability requires acknowledgement.
	 * 
	 * @return <code>true</code> if the reliability requires acknowledgement,
	 *         <code>false</code> otherwise.
	 */
	public boolean requiresAck() {
		return this.requiresAck;
	}

	/**
	 * Returns the reliability based on its ID.
	 * 
	 * @param id
	 *            the ID of the reliability.
	 * @return the reliability with the specified ID.
	 */
	public static Reliability lookup(int id) {
		for (Reliability reliability : Reliability.values()) {
			if (reliability.getId() == id) {
				return reliability;
			}
		}
		return null;
	}

}
