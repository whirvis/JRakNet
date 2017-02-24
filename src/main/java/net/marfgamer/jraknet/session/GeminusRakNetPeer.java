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
 * Copyright (c) 2016, 2017 MarfGamer
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
package net.marfgamer.jraknet.session;

import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.Reliability;

/**
 * This interface represents a server connection to a client, this is used
 * mainly to keep message sending easier and consistent between the lower and
 * higher level implementation using the session's globally unique ID.
 *
 * @author MarfGamer
 */
public interface GeminusRakNetPeer {

	/**
	 * Sends a message with the specified reliability on the specified channel
	 * to the session with the specified globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the session.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packet
	 *            the packet to send.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public void sendMessage(long guid, Reliability reliability, int channel, Packet packet);

	/**
	 * Sends the specified messages with the specified reliability on the
	 * specified channel to the session with the specified globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the session.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packets
	 *            the packets to send.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default void sendMessage(long guid, Reliability reliability, int channel, Packet... packets)
			throws InvalidChannelException {
		for (Packet packet : packets) {
			this.sendMessage(guid, reliability, channel, packet);
		}
	}

	/**
	 * Sends a message with the specified reliability on the default channel to
	 * the session with the specified globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the session.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packet
	 *            the packet to send.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default void sendMessage(long guid, Reliability reliability, Packet packet) throws InvalidChannelException {
		this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, packet);
	}

	/**
	 * Sends the specified messages with the specified reliability on the
	 * default channel to the session with the specified globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the session.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packets
	 *            the packets to send.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default void sendMessage(long guid, Reliability reliability, Packet... packets)
			throws InvalidChannelException {
		for (Packet packet : packets) {
			this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, packet);
		}
	}

	/**
	 * Sends a message identifier with the specified reliability on the
	 * specified channel to the session with the specified globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the session.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packetId
	 *            the packet ID to send.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default void sendMessage(long guid, Reliability reliability, int channel, int packetId) {
		this.sendMessage(guid, reliability, channel, new RakNetPacket(packetId));
	}

	/**
	 * Sends the specified message identifiers with the specified reliability on
	 * the specified channel to the session with the specified globally unique
	 * ID
	 * 
	 * @param guid
	 *            the globally unique ID of the session.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param channel
	 *            the channel to send the packet on.
	 * @param packetIds
	 *            the packet IDs to send.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default void sendMessage(long guid, Reliability reliability, int channel, int... packetIds) {
		for (int packetId : packetIds) {
			this.sendMessage(guid, reliability, channel, packetId);
		}
	}

	/**
	 * Sends a message identifier with the specified reliability on the default
	 * channel to the session with the specified globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the session.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packetId
	 *            the packet ID to send.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default void sendMessage(long guid, Reliability reliability, int packetId) {
		this.sendMessage(guid, reliability, new RakNetPacket(packetId));
	}

	/**
	 * Sends the specified message identifiers with the specified reliability on
	 * the default channel to the session with the specified globally unique ID.
	 * 
	 * @param guid
	 *            the globally unique ID of the session.
	 * @param reliability
	 *            the reliability of the packet.
	 * @param packetIds
	 *            the packet IDs to send.
	 * @throws InvalidChannelException
	 *             if the channel is higher than the maximum.
	 */
	public default void sendMessage(long guid, Reliability reliability, int... packetIds) {
		for (int packetId : packetIds) {
			this.sendMessage(guid, reliability, packetId);
		}
	}

}
