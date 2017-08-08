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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet.client;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.identifier.Identifier;
import net.marfgamer.jraknet.protocol.message.EncapsulatedPacket;
import net.marfgamer.jraknet.protocol.message.acknowledge.Record;
import net.marfgamer.jraknet.session.RakNetServerSession;

/**
 * This interface is used by the <code>RakNetClient</code> to let the user know
 * when specific events are triggered. Do <i>not</i> use any sleep methods that
 * will cause the client thread to pause, as it will cause a timeout.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public interface RakNetClientListener {

	/**
	 * Called when the client successfully connects to a server.
	 * 
	 * @param session
	 *            the session assigned to the server.
	 */
	public default void onConnect(RakNetServerSession session) {
	}

	/**
	 * Called when the client disconnects from the server.
	 * 
	 * @param session
	 *            the server the client disconnected from.
	 * @param reason
	 *            the reason for disconnection.
	 */
	public default void onDisconnect(RakNetServerSession session, String reason) {
	}

	/**
	 * Called when the client has been shutdown.
	 */
	public default void onClientShutdown() {
	}

	/**
	 * Called when a server is discovered on the local network.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param identifier
	 *            the <code>Identifier</code> of the server.
	 */
	public default void onServerDiscovered(InetSocketAddress address, Identifier identifier) {
	}

	/**
	 * Called when the <code>Identifier</code> of an already discovered server
	 * changes.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param identifier
	 *            the new <code>Identifier</code>.
	 */
	public default void onServerIdentifierUpdate(InetSocketAddress address, Identifier identifier) {
	}

	/**
	 * Called when a previously discovered server has been forgotten by the client.
	 * 
	 * @param address
	 *            the address of the server.
	 */
	public default void onServerForgotten(InetSocketAddress address) {
	}

	/**
	 * Called when an external server is added to the client's external server list.
	 * 
	 * @param address
	 *            the address of the server.
	 */
	public default void onExternalServerAdded(InetSocketAddress address) {
	}

	/**
	 * Called when the identifier of an external server changes.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param identifier
	 *            the new identifier.
	 */
	public default void onExternalServerIdentifierUpdate(InetSocketAddress address, Identifier identifier) {
	}

	/**
	 * Called when an external server is removed from the client's external server
	 * list.
	 * 
	 * @param address
	 *            the address of the server.
	 */
	public default void onExternalServerRemoved(InetSocketAddress address) {
	}

	/**
	 * Called when a message is received by the server.
	 * 
	 * @param session
	 *            the server that received the packet.
	 * @param record
	 *            the received record.
	 * @param the
	 *            received packet.
	 */
	public default void onAcknowledge(RakNetServerSession session, Record record, EncapsulatedPacket packet) {
	}

	/**
	 * Called when a message is not received by the server.
	 * 
	 * @param session
	 *            the server that lost the packet.
	 * @param record
	 *            the lost record.
	 * @param packet
	 *            the lost packet.
	 */
	public default void onNotAcknowledge(RakNetServerSession session, Record record, EncapsulatedPacket packet) {
	}

	/**
	 * Called when a packet has been received from the server and is ready to be
	 * handled.
	 * 
	 * @param session
	 *            the server that sent the packet.
	 * @param packet
	 *            the packet received from the server.
	 * @param channel
	 *            the channel the packet was sent on.
	 */
	public default void handleMessage(RakNetServerSession session, RakNetPacket packet, int channel) {
	}

	/**
	 * Called when the handler receives a packet after the server has already
	 * handled it, this method is useful for handling packets outside of the RakNet
	 * protocol. However, be weary when using this as packets meant for the server
	 * will have already been handled by the client; and it is not a good idea to
	 * try to manipulate JRakNet's RakNet protocol implementation using this method.
	 * 
	 * @param buf
	 *            the packet buffer.
	 * @param address
	 *            the address of the sender.
	 */
	public default void handleNettyMessage(ByteBuf buf, InetSocketAddress address) {
	}

	/**
	 * Called when a handler exception has occurred, these normally do not matter as
	 * long as it does not come the address of the server the client is connecting
	 * or is connected to.
	 * 
	 * @param address
	 *            the address that caused the exception.
	 * @param throwable
	 *            the <code>Throwable</code> that was caught.
	 */
	public default void onHandlerException(InetSocketAddress address, Throwable throwable) {
		throwable.printStackTrace();
	}

	/**
	 * Called when an exception is caught in the external thread the client is
	 * running on, this method is only called when the client is started through
	 * <code>connectThreaded()</code>.
	 * 
	 * @param throwable
	 *            the <code>Throwable</code> that was caught.
	 */
	public default void onThreadException(Throwable throwable) {
		throwable.printStackTrace();
	}

}
