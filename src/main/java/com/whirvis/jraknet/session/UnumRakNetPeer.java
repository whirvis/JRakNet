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
package com.whirvis.jraknet.session;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;

/**
 * This interface represents a client connection to a server, this is used
 * mainly to keep message sending easier and consistent between the lower and
 * higher level implementation.
 *
 * @author Whirvis T. Wheatley
 */
public interface UnumRakNetPeer {

	/**
	 * Sends a message with the reliability on the channel
	 * and returns a copy of the generated encapsulated packet that will be used
	 * when it is actually sent.
	 * 
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packet
	 *            the packet to send.
	 * @return the generated encapsulated packet.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public EncapsulatedPacket sendMessage(Reliability reliability, int channel, Packet packet);

	/**
	 * Sends the messages with the reliability on the
	 * channel and returns copies of the generated encapsulated
	 * packets that will be used when it is actually sent.
	 * 
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packets
	 *            the packets to send.
	 * @return the generated encapsulated packets.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default EncapsulatedPacket[] sendMessage(Reliability reliability, int channel, Packet... packets)
			throws InvalidChannelException {
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[packets.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(reliability, channel, packets[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends a message with the reliability on the default channel and
	 * returns a copy of the generated encapsulated packet that will be used
	 * when it is actually sent.
	 * 
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packet
	 *            the packet to send.
	 * @return the generated encapsulated packet.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default EncapsulatedPacket sendMessage(Reliability reliability, Packet packet)
			throws InvalidChannelException {
		return this.sendMessage(reliability, RakNet.DEFAULT_CHANNEL, packet);
	}

	/**
	 * Sends the messages with the reliability on the
	 * default channel and returns copies of the generated encapsulated packets
	 * that will be used when it is actually sent.
	 * 
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packets
	 *            the packets to send.
	 * @return the generated encapsulated packets.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default EncapsulatedPacket[] sendMessage(Reliability reliability, Packet... packets)
			throws InvalidChannelException {
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[packets.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(reliability, RakNet.DEFAULT_CHANNEL, packets[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends a message identifier with the reliability on the
	 * channel and returns a copy of the generated encapsulated packet
	 * that will be used when it is actually sent.
	 * 
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packetId
	 *            the packet ID to send.
	 * @return the generated encapsulated packet.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default EncapsulatedPacket sendMessage(Reliability reliability, int channel, int packetId) {
		return this.sendMessage(reliability, channel, new RakNetPacket(packetId));
	}

	/**
	 * Sends the message identifiers with the reliability on
	 * the channel and returns copies of the generated encapsulated
	 * packets that will be used when it is actually sent.
	 * 
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packetIds
	 *            the packet IDs to send.
	 * @return the generated encapsulated packets.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default EncapsulatedPacket[] sendMessage(Reliability reliability, int channel, int... packetIds) {
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[packetIds.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(reliability, channel, packetIds[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends a message identifier with the reliability on the default
	 * channel and returns a copy of the generated encapsulated packet that will
	 * be used when it is actually sent.
	 * 
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packetId
	 *            the packet ID to send.
	 * @return the generated encapsulated packet.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default EncapsulatedPacket sendMessage(Reliability reliability, int packetId) {
		return this.sendMessage(reliability, new RakNetPacket(packetId));
	}

	/**
	 * Sends the message identifiers with the reliability on
	 * the default channel and returns copies of the generated encapsulated
	 * packets that will be used when it is actually sent.
	 * 
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packetIds
	 *            the packet IDs to send.
	 * @return the generated encapsulated packets.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default EncapsulatedPacket[] sendMessage(Reliability reliability, int... packetIds) {
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[packetIds.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(reliability, packetIds[i]);
		}
		return encapsulated;
	}

}
