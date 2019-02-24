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
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;
import com.whirvis.jraknet.session.RakNetServerSession;

import io.netty.buffer.ByteBuf;

/**
 * Used to listen for events that occur in the {@link akNetClient}. In order to
 * listen for events, one must use the
 * {@link RakNetClient#addListener(RakNetClientListener)} method.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0
 */
public interface RakNetClientListener {

	/**
	 * Called when the client successfully connects to a server.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the session assigned to the server.
	 */
	public default void onConnect(RakNetClient client, RakNetServerSession session) {
	}

	/**
	 * Called when the client disconnects from the server.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server the client disconnected from.
	 * @param reason
	 *            the reason for disconnection.
	 */
	public default void onDisconnect(RakNetClient client, RakNetServerSession session, String reason) {
	}

	/**
	 * Called when the client has been shutdown.
	 * 
	 * @param client
	 *            the client.
	 * @param reason
	 *            the reason for shutdown.
	 */
	public default void onClientShutdown(RakNetClient client, String reason) {
	}

	/**
	 * Called when a message is received by the server.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server that received the packet.
	 * @param record
	 *            the received record.
	 * @param packet
	 *            the received packet.
	 */
	public default void onAcknowledge(RakNetClient client, RakNetServerSession session, Record record,
			EncapsulatedPacket packet) {
	}

	/**
	 * Called when a message is not received by the server.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server that lost the packet.
	 * @param record
	 *            the lost record.
	 * @param packet
	 *            the lost packet.
	 */
	public default void onNotAcknowledge(RakNetClient client, RakNetServerSession session, Record record,
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
	 *            the packet received from the server.
	 * @param channel
	 *            the channel the packet was sent on.
	 */
	public default void handleMessage(RakNetClient client, RakNetServerSession session, RakNetPacket packet,
			int channel) {
	}

	/**
	 * Called when a packet with an ID below the user enumeration
	 * (<code>ID_USER_PACKET_ENUM</code>) cannot be handled by the session
	 * because it is not programmed to handle it. This function can be used to
	 * add missing features from the regular RakNet protocol that are absent in
	 * JRakNet if needed.
	 * 
	 * @param client
	 *            the client.
	 * @param session
	 *            the server that sent the packet.
	 * @param packet
	 *            the packet received from the server.
	 * @param channel
	 *            the channel the packet was sent on.
	 */
	public default void handleUnknownMessage(RakNetClient client, RakNetServerSession session, RakNetPacket packet,
			int channel) {
	}

	/**
	 * Called when the handler receives a packet after the server has already
	 * handled it, this method is useful for handling packets outside of the
	 * RakNet protocol. However, be weary when using this as packets meant for
	 * the server will have already been handled by the client; and it is not a
	 * good idea to try to manipulate JRakNet's RakNet protocol implementation
	 * using this method.
	 * 
	 * @param client
	 *            the client.
	 * @param buf
	 *            the packet buffer.
	 * @param address
	 *            the address of the sender.
	 */
	public default void handleNettyMessage(RakNetClient client, ByteBuf buf, InetSocketAddress address) {
	}

	/**
	 * Called when a handler exception has occurred, these normally do not
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
