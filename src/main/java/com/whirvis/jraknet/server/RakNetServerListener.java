/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
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
package com.whirvis.jraknet.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.message.EncapsulatedPacket;
import com.whirvis.jraknet.protocol.message.acknowledge.Record;

import io.netty.buffer.ByteBuf;

/**
 * Used to listen for events that occur in the {@link RakNetServer}. In order to
 * listen for events, one must use the
 * {@link RakNetServer#addListener(RakNetServerListener)} method.
 * <p>
 * Event methods are called on the same thread that called them. Typically, this
 * is the NIO event loop group that the server is using, or the server thread
 * itself. This normally does not matter, however in some cases if a listener
 * takes too long to respond (typically
 * {@value com.whirvis.jraknet.peer.RakNetPeer#PEER_TIMEOUT} milliseconds) then
 * the server can actually timeout.
 * <p>
 * To have event methods called on their own dedicated thread, annotate the
 * listening class with the {@link com.whirvis.jraknet.ThreadedListener
 * ThreadedListener} annotation.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public interface RakNetServerListener {

	/**
	 * Called when the server has been started.
	 * 
	 * @param server
	 *            the server.
	 */
	public default void onStart(RakNetServer server) {
	}

	/**
	 * Called when the server has been shutdown.
	 * 
	 * @param server
	 *            the server.
	 */
	public default void onShutdown(RakNetServer server) {
	}

	/**
	 * Called when the server receives a ping from a client.
	 * 
	 * @param server
	 *            the server.
	 * @param ping
	 *            the response that will be sent to the client.
	 */
	public default void onPing(RakNetServer server, ServerPing ping) {
	}

	/**
	 * Called when a client has connected to the server.
	 * <p>
	 * This is not the same as {@link #onLogin(RakNetServer, RakNetClientPeer)},
	 * where the client has also completed connection and login.
	 * 
	 * @param server
	 *            the server.
	 * @param address
	 *            the address of the client.
	 * @param connectionType
	 *            the connection type of the client.
	 */
	public default void onConnect(RakNetServer server, InetSocketAddress address, ConnectionType connectionType) {
	}

	/**
	 * Called when a client has logged in to the server.
	 * <p>
	 * This is not the same as
	 * {@link #onConnect(RakNetServer, InetSocketAddress, ConnectionType)},
	 * where the client has only connected to the server and has not yet logged
	 * in.
	 * 
	 * @param server
	 *            the server.
	 * @param peer
	 *            the client that logged in.
	 */
	public default void onLogin(RakNetServer server, RakNetClientPeer peer) {
	}

	/**
	 * Called when a client has disconnected from the server.
	 * 
	 * @param server
	 *            the server.
	 * @param address
	 *            the address of the client that disconnected.
	 * @param peer
	 *            the client that disconnected, this will be <code>null</code>
	 *            if the client has not yet logged in.
	 * @param reason
	 *            the reason the client disconnected.
	 */
	public default void onDisconnect(RakNetServer server, InetSocketAddress address, RakNetClientPeer peer, String reason) {
	}

	/**
	 * Called when a client is banned from the server.
	 * <p>
	 * When a client is banned from the server, they will be actively
	 * disconnected with a
	 * {@link com.whirvis.jraknet.protocol.connection.ConnectionBanned
	 * CONNECTION_BANNED} packet. This is different from having an address
	 * blocked, as all packets sent from the address will simply be ignored. The
	 * server will never automatically ban a client. However, it will
	 * automatically block an address if it is suspected of a
	 * <a href="https://en.wikipedia.org/wiki/Denial-of-service_attack">DOS</a>
	 * attack.
	 * 
	 * @param server
	 *            the server.
	 * @param address
	 *            the address of the client.
	 * @param reason
	 *            the reason the client was banned.
	 */
	public default void onBan(RakNetServer server, InetAddress address, String reason) {
	}

	/**
	 * Called when a client is unbanned from the server.
	 * 
	 * @param server
	 *            the server.
	 * @param address
	 *            the address of the client.
	 */
	public default void onUnban(RakNetServer server, InetAddress address) {
	}

	/**
	 * Called when an address is blocked by the server.
	 * <p>
	 * When an address is blocked, all packets sent from it will simply be
	 * ignored. This is different from a client being banned, as it will
	 * actively be disconnected with a
	 * {@link com.whirvis.jraknet.protocol.connection.ConnectionBanned
	 * CONNECTION_BANNED} packet. The server will never automatically ban a
	 * client. However, it will automatically block an address if it is
	 * suspected of a
	 * <a href="https://en.wikipedia.org/wiki/Denial-of-service_attack">DOS</a>
	 * attack.
	 * 
	 * @param server
	 *            the server.
	 * @param address
	 *            the address that was blocked.
	 * @param reason
	 *            the reason the address was blocked.
	 * @param time
	 *            how long the address is blocked, with
	 *            {@value BlockedAddress#PERMANENT_BLOCK} meaning the address is
	 *            permanently blocked.
	 */
	public default void onBlock(RakNetServer server, InetAddress address, String reason, long time) {
	}

	/**
	 * Called when an address has been unblocked by the server.
	 * 
	 * @param server
	 *            the server.
	 * @param address
	 *            the address that has been unblocked.
	 */
	public default void onUnblock(RakNetServer server, InetAddress address) {
	}

	/**
	 * Called when a message is acknowledged by a client.
	 * 
	 * @param server
	 *            the server.
	 * @param peer
	 *            the client that acknwoledged the packet.
	 * @param record
	 *            the acknowledged record.
	 * @param packet
	 *            the acknowledged packet.
	 */
	public default void onAcknowledge(RakNetServer server, RakNetClientPeer peer, Record record, EncapsulatedPacket packet) {
	}

	/**
	 * Called when a message is lost by a client.
	 * 
	 * @param server
	 *            the server.
	 * @param peer
	 *            the client that lost the packet.
	 * @param record
	 *            the lost record.
	 * @param packet
	 *            the lost packet.
	 */
	public default void onLoss(RakNetServer server, RakNetClientPeer peer, Record record, EncapsulatedPacket packet) {
	}

	/**
	 * Called when a packet has been received from a client and is ready to be
	 * handled.
	 * 
	 * @param server
	 *            the server.
	 * @param peer
	 *            the client that sent the packet.
	 * @param packet
	 *            the packet received from the client.
	 * @param channel
	 *            the channel the packet was sent on.
	 */
	public default void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
	}

	/**
	 * Called when a packet with an ID below <code>ID_USER_PACKET_ENUM</code>
	 * cannot be handled by the {@link RakNetClientPeer} because it is not
	 * programmed to handle it.
	 * <p>
	 * This function can be used to add missing features from the regular RakNet
	 * protocol that are absent in JRakNet if needed.
	 * 
	 * @param server
	 *            the server.
	 * @param peer
	 *            the client that sent the packet.
	 * @param packet
	 *            the unknown packet.
	 * @param channel
	 *            the channel the packet was sent on.
	 */
	public default void handleUnknownMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
	}

	/**
	 * Called when the handler receives a packet after the server has already
	 * handled it.
	 * <p>
	 * This method is useful for handling packets outside of the RakNet
	 * protocol. All packets received here have already been handled by the
	 * server.
	 * 
	 * @param server
	 *            the server.
	 * @param address
	 *            the address of the sender.
	 * @param buf
	 *            the buffer of the received packet.
	 */
	public default void handleNettyMessage(RakNetServer server, InetSocketAddress address, ByteBuf buf) {
	}

	/**
	 * Called when a handler exception has occurred.
	 * <p>
	 * These normally do not matter as long as the server handles them on its
	 * own.
	 * 
	 * @param server
	 *            the server.
	 * @param address
	 *            the address that caused the exception.
	 * @param throwable
	 *            the <code>Throwable</code> that was caught.
	 */
	public default void onHandlerException(RakNetServer server, InetSocketAddress address, Throwable throwable) {
	}

	/**
	 * Called when an exception thrown by a peer has been caught.
	 * 
	 * @param server
	 *            the server.
	 * @param peer
	 *            the peer that caused the exception.
	 * @param throwable
	 *            the <code>Throwable</code> that was caught.
	 */
	public default void onPeerException(RakNetServer server, RakNetClientPeer peer, Throwable throwable) {
	}

}
