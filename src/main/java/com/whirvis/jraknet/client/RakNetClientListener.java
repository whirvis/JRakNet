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
package com.whirvis.jraknet.client;

import java.net.InetSocketAddress;

import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.peer.server.RakNetServerPeer;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;

import io.netty.buffer.ByteBuf;

/**
 * Used to listen for events that occur in the {@link RakNetClient}. In order to
 * listen for events, one must use the
 * {@link RakNetClient#addListener(RakNetClientListener)} method.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public interface RakNetClientListener {

	/**
	 * Called when the client connects to a server.
	 * 
	 * @param client
	 *            the client.
	 * @param address
	 *            the server address.
	 */
	public default void onConnect(RakNetClient client, InetSocketAddress address) {
	}

	/**
	 * Called when the client has logged in to a server.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server.
	 */
	public default void onLogin(RakNetClient client, RakNetServerPeer session) {
	}

	/**
	 * Called when the client disconnects from the server.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server that the client disconnected from.
	 * @param reason
	 *            the reason for disconnection.
	 */
	public default void onDisconnect(RakNetClient client, RakNetServerPeer session, String reason) {
	}

	/**
	 * Called when a message is acknowledged by the server.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server that acknowledged the packet.
	 * @param record
	 *            the acknowledged record.
	 * @param packet
	 *            the acknowledged packet.
	 */
	public default void onAcknowledge(RakNetClient client, RakNetServerPeer session, Record record,
			EncapsulatedPacket packet) {
	}

	/**
	 * Called when a message is not acknowledged by the server.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server that did not acknowledged the packet.
	 * @param record
	 *            the not acknowledged record.
	 * @param packet
	 *            the not acknowledged packet.
	 */
	public default void onNotAcknowledge(RakNetClient client, RakNetServerPeer session, Record record,
			EncapsulatedPacket packet) {
	}

	/**
	 * Called when a packet has been received from the server and is ready to be
	 * handled.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server that sent the packet.
	 * @param packet
	 *            the received packet.
	 * @param channel
	 *            the channel the packet was sent on.
	 */
	public default void handleMessage(RakNetClient client, RakNetServerPeer session, RakNetPacket packet,
			int channel) {
	}

	/**
	 * Called when a packet with an ID below <code>ID_USER_PACKET_ENUM</code>
	 * cannot be handled by the session because it is not programmed to handle
	 * it. This function can be used to add missing features from the regular
	 * RakNet protocol that are absent in JRakNet if needed.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server that sent the packet.
	 * @param packet
	 *            the unknown packet.
	 * @param channel
	 *            the channel the packet was sent on.
	 */
	public default void handleUnknownMessage(RakNetClient client, RakNetServerPeer session, RakNetPacket packet,
			int channel) {
	}

	/**
	 * Called when the handler receives a packet after the server has already
	 * handled it. This method is useful for handling packets outside of the
	 * RakNet protocol. All packets received here have already been handled by
	 * the client.
	 * 
	 * @param client
	 *            the client.
	 * @param address
	 *            the address of the sender.
	 * @param buf
	 *            the buffer of the received packet.
	 */
	public default void handleNettyMessage(RakNetClient client, InetSocketAddress address, ByteBuf buf) {
	}

	/**
	 * Called when a handler exception has occurred. These normally do not
	 * matter as long as it does not come from the address of the server the
	 * client is connecting to or is connected to.
	 * 
	 * @param client
	 *            the client.
	 * @param address
	 *            the address that caused the exception.
	 * @param throwable
	 *            the <code>Throwable</code> that was caught.
	 */
	public default void onHandlerException(RakNetClient client, InetSocketAddress address, Throwable throwable) {
	}

	/**
	 * Called when a session exception has occurred.
	 * 
	 * @param client
	 *            the client.
	 * @param throwable
	 *            the <code>Throwable</code> that was caught.
	 */
	public default void onSessionException(RakNetClient client, Throwable throwable) {
	}

}
