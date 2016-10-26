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
 * Copyright (c) 2016 MarfGamer
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
package net.marfgamer.raknet.client;

import java.net.InetSocketAddress;

import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.identifier.Identifier;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.message.acknowledge.Record;
import net.marfgamer.raknet.session.RakNetServerSession;

/**
 * This interface is used by the client to let the user know when specific
 * events are triggered
 *
 * @author MarfGamer
 */
public interface RakNetClientListener {

	/**
	 * Called when a server is discovered on the local network
	 * 
	 * @param address
	 *            - The address of the server
	 * @param identifier
	 *            - The identifier of the server
	 */
	public default void onServerDiscovered(InetSocketAddress address, Identifier identifier) {
	}

	/**
	 * Called when the identifier of an already discovered server changes
	 * 
	 * @param address
	 *            - The address of the server
	 * @param identifier
	 *            - The new identifier
	 */
	public default void onServerIdentifierUpdate(InetSocketAddress address, Identifier identifier) {
	}

	/**
	 * Called when a previously discovered server has been forgotten by the
	 * client
	 * 
	 * @param address
	 *            - The address of the server
	 */
	public default void onServerForgotten(InetSocketAddress address) {
	}

	/**
	 * Called when the client fails to connect to a server
	 * 
	 * @param address
	 *            - The address of the server
	 * @param cause
	 *            - The cause for failure
	 */
	public default void onConnectionFailure(InetSocketAddress address, RakNetException cause) {
	}

	/**
	 * Called when the client successfully connects to a server
	 * 
	 * @param session
	 *            - The session assigned to the server
	 */
	public default void onConnect(RakNetServerSession session) {
	}

	/**
	 * Called when the client disconnects from the server
	 * 
	 * @param session
	 *            - The server the client disconnected from
	 * @param reason
	 *            - The reason for disconnection
	 */
	public default void onDisconnect(RakNetServerSession session, String reason) {
	}

	/**
	 * Called when a message sent with _REQUIRES_ACK_RECEIPT is acknowledged by
	 * the server
	 * 
	 * @param session
	 *            - The server that acknowledged the packet
	 * @param record
	 *            - The record of the acknowledged packet
	 * @param reliability
	 *            - The reliability of the acknowledged packet
	 * @param channel
	 *            - The channel of the acknowledged packet
	 * @param packet
	 *            - The acknowledged packet
	 */
	public default void onAcknowledge(RakNetServerSession session, Record record, Reliability reliability, int channel,
			RakNetPacket packet) {
	}

	/**
	 * Called when a handler exception has occurred, these normally do not
	 * matter as long as it does not come the address of the server the client
	 * is connecting or is connected to
	 * 
	 * @param address
	 *            - The address that caused the exception
	 * @param throwable
	 *            - The throwable exception that was caught
	 */
	public default void onHandlerException(InetSocketAddress address, Throwable throwable) {
	}

	/**
	 * Called when a packet has been received from the server and is ready to be
	 * handled
	 * 
	 * @param session
	 *            - The server that sent the packet
	 * @param packet
	 *            - The packet received from the server
	 * @param channel
	 *            - The channel the packet was sent on
	 */
	public default void handlePacket(RakNetServerSession session, RakNetPacket packet, int channel) {
	}

}
