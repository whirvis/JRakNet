/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
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
package com.whirvis.jraknet.peer;

import com.whirvis.jraknet.InvalidChannelException;
import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;

import io.netty.buffer.ByteBuf;

/**
 * Represents an object that has the ability to send messages to a
 * {@link RakNetClientPeer}.
 * 
 * @author Trent Summerlin
 * @since JRakNet v2.11.0
 */
public interface RakNetClientPeerMessenger {

	/**
	 * Returns the globally unique ID of the specified peer.
	 * 
	 * @param peer the peer.
	 * @return the globally unique ID of the specified peer.
	 * @throws NullPointerException     if the <code>peer</code> is
	 *                                  <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 */
	public long getGuid(RakNetClientPeer peer) throws NullPointerException, IllegalArgumentException;

	/**
	 * Sends a message to the specified peer.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param channel     the channel to send the packet on.
	 * @param packet      the packet to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException    if the <code>reliability</code> or
	 *                                 <code>packet</code> are <code>null</code>.
	 * @throws InvalidChannelException if the channel is higher than or equal to
	 *                                 {@value RakNet#CHANNEL_COUNT}.
	 */
	public EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, Packet packet)
			throws NullPointerException, InvalidChannelException;

	/**
	 * Sends a message to the specified peer.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param channel     the channel to send the packet on.
	 * @param packet      the packet to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or
	 *                                  <code>packet</code> are <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 * @throws InvalidChannelException  if the channel is higher than or equal to
	 *                                  {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			Packet packet) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, packet);
	}

	/**
	 * Sends messages to the specified peer.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the packets.
	 * @param channel     the channel to send the packets on.
	 * @param packets     the packets to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException    if the <code>reliability</code> or
	 *                                 <code>packets</code> are <code>null</code>.
	 * @throws InvalidChannelException if the channel is higher than or equal to
	 *                                 {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, int channel, Packet... packets)
			throws NullPointerException, InvalidChannelException {
		if (packets == null) {
			throw new NullPointerException("Packets cannot be null");
		}
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[packets.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(guid, reliability, channel, packets[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends messages to the specified peer.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the packets.
	 * @param channel     the channel to send the packets on.
	 * @param packets     the packets to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or
	 *                                  <code>packets</code> are <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 * @throws InvalidChannelException  if the channel is higher than or equal to
	 *                                  {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			Packet... packets) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, packets);
	}

	/**
	 * Sends a message to the specified peer on the default channel.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param packet      the packet to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException if the <code>reliability</code> or
	 *                              <code>packet</code> are <code>null</code>.
	 */
	public default EncapsulatedPacket sendMessage(long guid, Reliability reliability, Packet packet)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, packet);
	}

	/**
	 * Sends a message to the specified peer on the default channel.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param packet      the packet to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or
	 *                                  <code>packet</code> are <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 */
	public default EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, Packet packet)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, packet);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the packets.
	 * @param packets     the packets to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException if the <code>reliability</code> or
	 *                              <code>packets</code> are <code>null</code>.
	 */
	public default EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, Packet... packets)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, packets);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the packets.
	 * @param packets     the packets to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or
	 *                                  <code>packets</code> are <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 */
	public default EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, Packet... packets)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, packets);
	}

	/**
	 * Sends a message to the specified peer.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param channel     the channel to send the packet on.
	 * @param buf         the buffer to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException    if the <code>reliability</code> or
	 *                                 <code>buf</code> are <code>null</code>.
	 * @throws InvalidChannelException if the channel is higher than or equal to
	 *                                 {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, ByteBuf buf)
			throws NullPointerException, InvalidChannelException {
		return this.sendMessage(guid, reliability, channel, new Packet(buf));
	}

	/**
	 * Sends a message to the specified peer.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param channel     the channel to send the packet on.
	 * @param buf         the buffer to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or <code>buf</code>
	 *                                  are <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 * @throws InvalidChannelException  if the channel is higher than or equal to
	 *                                  {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			ByteBuf buf) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, buf);
	}

	/**
	 * Sends messages to the specified peer.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the packets.
	 * @param channel     the channel to send the packets on.
	 * @param bufs        the buffers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException    if the <code>reliability</code> or
	 *                                 <code>bufs</code> are <code>null</code>.
	 * @throws InvalidChannelException if the channel is higher than or equal to
	 *                                 {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, int channel, ByteBuf... bufs)
			throws NullPointerException, InvalidChannelException {
		if (bufs == null) {
			throw new NullPointerException("Buffers cannot be null");
		}
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[bufs.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(guid, reliability, channel, bufs[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends messages to the specified peer.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the packets.
	 * @param channel     the channel to send the packets on.
	 * @param bufs        the buffers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or
	 *                                  <code>bufs</code> are <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 * @throws InvalidChannelException  if the channel is higher than or equal to
	 *                                  {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			ByteBuf... bufs) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, bufs);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param buf         the buffer to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException if the <code>reliability</code> or
	 *                              <code>buf</code> are <code>null</code>.
	 */
	public default EncapsulatedPacket sendMessage(long guid, Reliability reliability, ByteBuf buf)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, buf);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param buf         the buffer to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or <code>buf</code>
	 *                                  are <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 */
	public default EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, ByteBuf buf)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, buf);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param bufs        the buffers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException if the <code>reliability</code> or
	 *                              <code>bufs</code> are <code>null</code>.
	 */
	public default EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, ByteBuf... bufs)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, bufs);
	}

	/**
	 * Sends messages to the specified peer on the default channel.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the packet.
	 * @param bufs        the buffers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or
	 *                                  <code>bufs</code> are <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 */
	public default EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, ByteBuf... bufs)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, bufs);
	}

	/**
	 * Sends a message identifier to the specified peer.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the message identifier.
	 * @param channel     the channel to send the message identifier on.
	 * @param packetId    the message identifier to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException    if the <code>reliability</code> is
	 *                                 <code>null</code>.
	 * @throws InvalidChannelException if the channel is higher than or equal to
	 *                                 {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket sendMessage(long guid, Reliability reliability, int channel, int packetId)
			throws NullPointerException, InvalidChannelException {
		return this.sendMessage(guid, reliability, channel, new RakNetPacket(packetId));
	}

	/**
	 * Sends a message identifier to the specified peer.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the message identifier.
	 * @param channel     the channel to send the message identifier on.
	 * @param packetId    the message identifier to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the<code>peer</code> or
	 *                                  <code>reliability</code> are
	 *                                  <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 * @throws InvalidChannelException  if the channel is higher than or equal to
	 *                                  {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			int packetId) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, packetId);
	}

	/**
	 * Sends message identifiers to the specified peer.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the message identifiers.
	 * @param channel     the channel to send the message identifiers on.
	 * @param packetIds   the message identifiers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException    if the <code>reliability</code> or
	 *                                 <code>packetIds</code> are <code>null</code>.
	 * @throws InvalidChannelException if the channel is higher than or equal to
	 *                                 {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, int channel, int... packetIds)
			throws NullPointerException, InvalidChannelException {
		if (packetIds == null) {
			throw new NullPointerException("Packet IDs cannot be null");
		}
		EncapsulatedPacket[] encapsulated = new EncapsulatedPacket[packetIds.length];
		for (int i = 0; i < encapsulated.length; i++) {
			encapsulated[i] = this.sendMessage(guid, reliability, channel, packetIds[i]);
		}
		return encapsulated;
	}

	/**
	 * Sends message identifiers to the specified peer.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the message identifiers.
	 * @param channel     the channel to send the message identifiers on.
	 * @param packetIds   the message identifiers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or
	 *                                  <code>packetIds</code> are
	 *                                  <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 * @throws InvalidChannelException  if the channel is higher than or equal to
	 *                                  {@value RakNet#CHANNEL_COUNT}.
	 */
	public default EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, int channel,
			int... packetIds) throws NullPointerException, IllegalArgumentException, InvalidChannelException {
		return this.sendMessage(this.getGuid(peer), reliability, channel, packetIds);
	}

	/**
	 * Sends a message identifier to the specified peer on the default channel.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the message identifier.
	 * @param packetId    the message identifier to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException if the <code>reliability</code> is
	 *                              <code>null</code>.
	 */
	public default EncapsulatedPacket sendMessage(long guid, Reliability reliability, int packetId)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, new RakNetPacket(packetId));
	}

	/**
	 * Sends a message identifier to the specified peer on the default channel.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the message identifier.
	 * @param packetId    the message identifier to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> is
	 *                                  <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 */
	public default EncapsulatedPacket sendMessage(RakNetClientPeer peer, Reliability reliability, int packetId)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, packetId);
	}

	/**
	 * Sends message identifiers to the specified peer on the default channel.
	 * 
	 * @param guid        the globally unique ID of the peer to send the packet to.
	 * @param reliability the reliability of the message identifiers.
	 * @param packetIds   the message identifiers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException if the <code>reliability</code> or
	 *                              <code>packetIds</code> are <code>null</code>.
	 */
	public default EncapsulatedPacket[] sendMessage(long guid, Reliability reliability, int... packetIds)
			throws NullPointerException {
		return this.sendMessage(guid, reliability, RakNet.DEFAULT_CHANNEL, packetIds);
	}

	/**
	 * Sends message identifiers to the specified peer on the default channel.
	 * 
	 * @param peer        the peer to send the packet to.
	 * @param reliability the reliability of the message identifiers.
	 * @param packetIds   the message identifiers to send.
	 * @return the generated encapsulated packet, <code>null</code> if no packet was
	 *         sent due to the non existence of the peer with the <code>guid</code>.
	 *         This is normally not important, however it can be used for packet
	 *         acknowledged and not acknowledged events if the reliability is of the
	 *         {@link Reliability#UNRELIABLE_WITH_ACK_RECEIPT WITH_ACK_RECEIPT}
	 *         type.
	 * @throws NullPointerException     if the <code>peer</code>,
	 *                                  <code>reliability</code> or
	 *                                  <code>packetIds</code> are
	 *                                  <code>null</code>.
	 * @throws IllegalArgumentException if the <code>peer</code> is not of the
	 *                                  server.
	 */
	public default EncapsulatedPacket[] sendMessage(RakNetClientPeer peer, Reliability reliability, int... packetIds)
			throws NullPointerException, IllegalArgumentException {
		return this.sendMessage(this.getGuid(peer), reliability, packetIds);
	}

}
