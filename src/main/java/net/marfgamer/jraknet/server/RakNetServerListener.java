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
package net.marfgamer.jraknet.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.protocol.Reliability;
import net.marfgamer.jraknet.protocol.message.acknowledge.Record;
import net.marfgamer.jraknet.session.RakNetClientSession;

/**
 * This interface is used by the server to let the user know when specific
 * events are triggered<br>
 * <br>
 * Note: Do <b>NOT</b> use <code>Thread.sleep(long)</code> in any of these
 * methods, as it will cause the server to timeout!
 *
 * @author MarfGamer
 */
public interface RakNetServerListener {

	/**
	 * Called when the server has been successfully started
	 */
	public default void onServerStart() {
	}

	/**
	 * Called when the server receives a ping from a client
	 * 
	 * @param ping
	 *            The response that will be sent to the client
	 */
	public default void handlePing(ServerPing ping) {
	}

	/**
	 * Called when a client has connected to the server but has not logged yet
	 * in
	 * 
	 * @param address
	 *            The address of the client
	 */
	public default void onClientPreConnect(InetSocketAddress address) {
	}

	/**
	 * Called when a client that has connected to the server fails to log in
	 * 
	 * @param address
	 *            The address of the client
	 * @param reason
	 *            The reason the client failed to login
	 */
	public default void onClientPreDisconnect(InetSocketAddress address, String reason) {
	}

	/**
	 * Called when a client has connected and logged in to the server
	 * 
	 * @param session
	 *            The session assigned to the client
	 */
	public default void onClientConnect(RakNetClientSession session) {
	}

	/**
	 * Called when a client has disconnected from the server
	 * 
	 * @param session
	 *            The client that disconnected
	 * @param reason
	 *            The reason the client disconnected
	 */
	public default void onClientDisconnect(RakNetClientSession session, String reason) {
	}

	/**
	 * Called when a message sent with _REQUIRES_ACK_RECEIPT is received by a
	 * client
	 * 
	 * @param session
	 *            The client that received the packet
	 * @param record
	 *            The record of the packet
	 * @param reliability
	 *            The reliability of the packet
	 * @param channel
	 *            The channel of the packet
	 * @param packet
	 *            The received packet
	 */
	public default void onAcknowledge(RakNetClientSession session, Record record, Reliability reliability, int channel,
			RakNetPacket packet) {
	}

	/**
	 * Called when a message sent with _REQUIRES_ACK_RECEIPT is not received by
	 * a client
	 * 
	 * @param session
	 *            The client that lost the packet
	 * @param record
	 *            The record of the packet
	 * @param reliability
	 *            The reliability of the packet
	 * @param channel
	 *            The channel of the packet
	 * @param packet
	 *            The lost packet
	 */
	public default void onNotAcknowledge(RakNetClientSession session, Record record, Reliability reliability,
			int channel, RakNetPacket packet) {
	}

	/**
	 * Called when a packet has been received from a client and is ready to be
	 * handled
	 * 
	 * @param session
	 *            The client that sent the packet
	 * @param packet
	 *            The packet received from the client
	 * @param channel
	 *            The channel the packet was sent on
	 */
	public default void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
	}

	/**
	 * Called when a handler exception has occurred, these normally do not
	 * matter as long as the server handles them on it's own
	 * 
	 * @param address
	 *            The address that caused the exception
	 * @param throwable
	 *            The throwable exception that was caught
	 */
	public default void onHandlerException(InetSocketAddress address, Throwable throwable) {
		throwable.printStackTrace();
	}

	/**
	 * Called when an address is blocked by the server
	 * 
	 * @param address
	 *            The address that was blocked
	 * @param time
	 *            How long the address is blocked for (Note: -1 is permanent)
	 */
	public default void onAddressBlocked(InetAddress address, long time) {
	}

	/**
	 * Called when an address has been unblocked by the server
	 * 
	 * @param address
	 *            The address that has been unblocked
	 */
	public default void onAddressUnblocked(InetAddress address) {
	}

	/**
	 * Called when the server has been shutdown
	 */
	public default void onServerShutdown() {
	}

	/**
	 * Called when an exception is caught in the external thread the server is
	 * running on, this method is only called when the server is started through
	 * <code>startThreaded()</code>
	 * 
	 * @param throwable
	 *            The throwable exception that was caught
	 */
	public default void onThreadException(Throwable throwable) {
		throwable.printStackTrace();
	}

}
