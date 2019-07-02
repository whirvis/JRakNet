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
package com.whirvis.jraknet.client.peer;

import static com.whirvis.jraknet.RakNetPacket.*;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.PacketBufferException;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.client.MaximumTransferUnit;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.peer.RakNetServerPeer;
import com.whirvis.jraknet.protocol.ConnectionType;
import com.whirvis.jraknet.protocol.connection.ConnectionBanned;
import com.whirvis.jraknet.protocol.connection.IncompatibleProtocolVersion;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestTwo;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseTwo;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;

/**
 * Used by the {@link RakNetClient} to create a {@link RakNetServerPeer}.
 *
 * @author Trent Summerlin
 * @since JRakNet v2.0.0
 */
public final class PeerFactory {

	/**
	 * The factory is not currently in the process of doing anything.
	 */
	private static final int STATE_IDLE = -1;

	/**
	 * The factory needs to send a {@link OpenConnectionRequestOne
	 * OPEN_CONNECTION_REQUEST_1} packet and get a {@link OpenConnectionRequestTwo
	 * OPEN_CONNECTION_RESPONSE_1} packet in order to proceed to the next state.
	 */
	private static final int STATE_FIRST_CONNECTION_REQUEST = 0;

	/**
	 * The factory needs to send a {@link OpenConnectionRequestTwo
	 * OPEN_CONNECTION_REQUEST_2} packet and get a {@link OpenConnectionResponseTwo
	 * OPEN_CONNECTION_RESPONSE_2} packet in response to finish peer creation.
	 */
	private static final int STATE_SECOND_CONNECTION_REQUEST = 1;

	/**
	 * The peer has been assembled.
	 */
	private static final int STATE_PEER_ASSEMBLED = 2;

	private int factoryState;
	private final Logger log;
	private final RakNetClient client;
	private final InetSocketAddress address;
	private final Bootstrap bootstrap;
	private final Channel channel;
	private final int initialMaximumTransferUnit;
	private final int maximumMaximumTransferUnit;
	private Throwable throwable;
	private long serverGuid;
	private int maximumTransferUnit;
	private ConnectionType connectionType;

	/**
	 * Creates a peer factory.
	 * 
	 * @param client                     the client connecting to the server.
	 * @param address                    the address of the server.
	 * @param bootstrap                  the bootstrap the <code>channel</code>
	 *                                   belongs to. Once a maximum transfer unit
	 *                                   has been decided upon, its
	 *                                   {@link ChannelOption#SO_SNDBUF} and
	 *                                   {@link ChannelOption#SO_RCVBUF} will be set
	 *                                   to it.
	 * @param channel                    the channel to use when creating the peer.
	 * @param initialMaximumTransferUnit the initial maximum transfer unit size.
	 * @param maximumMaximumTransferUnit the maximum transfer unit with the highest
	 *                                   size.
	 * @throws NullPointerException if the <code>client</code>, <code>address</code>
	 *                              or IP address are <code>null</code>.
	 */
	public PeerFactory(RakNetClient client, InetSocketAddress address, Bootstrap bootstrap, Channel channel,
			int initialMaximumTransferUnit, int maximumMaximumTransferUnit) throws NullPointerException {
		if (client == null) {
			throw new NullPointerException("Client cannot be null");
		} else if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		this.factoryState = STATE_IDLE;
		this.log = LogManager.getLogger(
				PeerFactory.class.getSimpleName() + "-" + Long.toHexString(client.getGloballyUniqueId()).toUpperCase());
		this.client = client;
		this.address = address;
		this.bootstrap = bootstrap;
		this.channel = channel;
		this.initialMaximumTransferUnit = initialMaximumTransferUnit;
		this.maximumMaximumTransferUnit = maximumMaximumTransferUnit;
	}

	/**
	 * Returns the address of the server that the peer is being assembled for.
	 * 
	 * @return the address of the server that the peer is being assembled for.
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * Called when an exception is caused by the server that the peer is being
	 * assembled for. This will cause the
	 * {@link #startAssembly(MaximumTransferUnit...)} method to throw the
	 * <code>Throwable</code> specified here.
	 * 
	 * @param throwable the <code>Throwable</code> the server caused to be thrown.
	 * @throws NullPointerException  if the <code>throwable</code> is
	 *                               <code>null</code>.
	 * @throws IllegalStateException if the peer is not currently being assembled,
	 *                               or has already been assembled.
	 */
	public void exceptionCaught(Throwable throwable) throws NullPointerException, IllegalStateException {
		if (throwable == null) {
			throw new NullPointerException("Throwable cannot be null");
		} else if (factoryState <= STATE_IDLE) {
			throw new IllegalStateException("Peer is not currently being assembled");
		} else if (factoryState >= STATE_PEER_ASSEMBLED) {
			throw new IllegalStateException("Peer has already been assembled");
		}
		this.throwable = throwable;
	}

	/**
	 * Starts peer assembly.
	 * <p>
	 * This will block the thread. However, packets will still be received by Netty.
	 * When a packet has been received, it should be sent back to the factory using
	 * the {@link #assemble(RakNetPacket)} method.
	 * 
	 * @param units the maximum transfer units the client will attempt to use with
	 *              the server.
	 * @throws NullPointerException  if the <code>units</code> are
	 *                               <code>null</code>.
	 * @throws IllegalStateException if the peer has already been assembled or is
	 *                               currently being assembled.
	 * @throws PeerFactoryException  if an error occurs when assembling the peer. It
	 *                               is possible for this method to throw a
	 *                               <code>PeerFactoryException</code> through the
	 *                               {@link #exceptionCaught(Throwable)} method. Any
	 *                               exception caught in the
	 *                               {@link #assemble(RakNetPacket)} method will be
	 *                               routed back and thrown here through this
	 *                               method.
	 * @throws PacketBufferException if encoding or decoding one of the packets
	 *                               fails.
	 */
	public void startAssembly(MaximumTransferUnit... units)
			throws NullPointerException, IllegalStateException, PeerFactoryException, PacketBufferException {
		if (units == null) {
			throw new NullPointerException("Maximum transfer units cannot be null");
		} else if (factoryState >= STATE_PEER_ASSEMBLED) {
			throw new IllegalStateException("Peer has already been assembled");
		} else if (factoryState > STATE_IDLE) {
			throw new IllegalStateException("Peer is already being assembled");
		}
		this.factoryState = STATE_FIRST_CONNECTION_REQUEST;

		// Send open connection request one with a decreasing MTU
		int availableAttempts = 0;
		for (MaximumTransferUnit unit : units) {
			availableAttempts += unit.getRetries();
			while (unit.retry() > 0 && factoryState < STATE_SECOND_CONNECTION_REQUEST && throwable == null) {
				OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
				connectionRequestOne.maximumTransferUnit = unit.getSize();
				connectionRequestOne.networkProtocol = client.getProtocolVersion();
				connectionRequestOne.encode();
				client.sendNettyMessage(connectionRequestOne, address);
				log.debug("Attemped connection request one with maximum transfer unit size " + unit.getSize() + " ("
						+ (unit.getSize() * 8) + " bits)");
				RakNet.sleep(500);
			}
		}

		// If the state did not update then the server is offline
		if (factoryState < STATE_SECOND_CONNECTION_REQUEST && throwable == null) {
			throw new ServerOfflineException(client, address);
		}

		// Send open connection request two until a response is received
		while (availableAttempts-- > 0 && factoryState < STATE_PEER_ASSEMBLED && throwable == null) {
			OpenConnectionRequestTwo connectionRequestTwo = new OpenConnectionRequestTwo();
			connectionRequestTwo.clientGuid = client.getGloballyUniqueId();
			connectionRequestTwo.serverAddress = this.address;
			connectionRequestTwo.maximumTransferUnit = this.maximumTransferUnit;
			connectionRequestTwo.encode();
			if (!connectionRequestTwo.failed()) {
				client.sendNettyMessage(connectionRequestTwo, address);
				log.debug("Attempted connection request two");
				RakNet.sleep(500);
			} else {
				throw new PacketBufferException(connectionRequestTwo);
			}
		}

		// If the state did not update then the server has gone offline
		if (factoryState < STATE_PEER_ASSEMBLED && throwable == null) {
			throw new ServerOfflineException(client, address);
		} else if (throwable != null) {
			if (throwable instanceof PeerFactoryException) {
				throw (PeerFactoryException) throwable;
			} else if (throwable instanceof PacketBufferException) {
				throw (PacketBufferException) throwable;
			} else {
				throw new PeerFactoryException(client, throwable);
			}
		}
	}

	/**
	 * Further assembles the peer creation by handling the specified packet.
	 * 
	 * @param packet the packet to handle.
	 * @return the created peer, <code>null</code> if the peer is not yet finished
	 *         assembling.
	 * @throws NullPointerException  if the <code>packet</code> is
	 *                               <code>null</code>.
	 * @throws IllegalStateException if the peer is not currently being assembled or
	 *                               if the peer has already been assembled.
	 */
	public RakNetServerPeer assemble(RakNetPacket packet) throws NullPointerException, IllegalStateException {
		if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		} else if (factoryState <= STATE_IDLE) {
			throw new IllegalStateException("Peer is not currently being assembled");
		} else if (factoryState >= STATE_PEER_ASSEMBLED) {
			throw new IllegalStateException("Peer has already been assembled");
		} else {
			try {
				if (packet.getId() == ID_OPEN_CONNECTION_REPLY_1 && factoryState == STATE_FIRST_CONNECTION_REQUEST) {
					OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne(packet);
					connectionResponseOne.decode();
					if (connectionResponseOne.magic == false) {
						throw new InvalidMagicException(client);
					} else if (connectionResponseOne.maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
						throw new InvalidMaximumTransferUnitException(client,
								connectionResponseOne.maximumTransferUnit);
					}

					/*
					 * If the maximum transfer unit of the server is smaller than that of the
					 * client, then use that one. Otherwise, use the highest valid maximum transfer
					 * unit of the client.
					 */
					this.maximumTransferUnit = Math.min(connectionResponseOne.maximumTransferUnit,
							maximumMaximumTransferUnit);
					this.serverGuid = connectionResponseOne.serverGuid;
					this.factoryState = STATE_SECOND_CONNECTION_REQUEST;
					log.debug("Applied maximum transfer unit " + maximumTransferUnit + " and globally unique ID "
							+ serverGuid + " from " + getName(packet.getId()) + " packet");
				} else if (packet.getId() == ID_OPEN_CONNECTION_REPLY_2
						&& factoryState == STATE_SECOND_CONNECTION_REQUEST) {
					OpenConnectionResponseTwo connectionResponseTwo = new OpenConnectionResponseTwo(packet);
					connectionResponseTwo.decode();
					if (connectionResponseTwo.failed()) {
						throw new PacketBufferException(connectionResponseTwo);
					} else if (connectionResponseTwo.magic == false) {
						throw new InvalidMagicException(client);
					} else if (connectionResponseTwo.serverGuid != serverGuid) {
						throw new InconsistentGuidException(client);
					} else if (connectionResponseTwo.maximumTransferUnit > maximumMaximumTransferUnit
							|| connectionResponseTwo.maximumTransferUnit < RakNet.MINIMUM_MTU_SIZE) {
						throw new InvalidMaximumTransferUnitException(client, maximumTransferUnit);
					} else if (connectionResponseTwo.maximumTransferUnit > maximumTransferUnit) {
						log.warn("Server responded with higher maximum transfer unit than agreed upon earlier");
					}
					bootstrap.option(ChannelOption.SO_SNDBUF, maximumTransferUnit)
							.option(ChannelOption.SO_RCVBUF, maximumTransferUnit)
							.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(maximumTransferUnit));

					// Create peer
					this.maximumTransferUnit = connectionResponseTwo.maximumTransferUnit;
					this.connectionType = connectionResponseTwo.connectionType;
					this.factoryState = STATE_PEER_ASSEMBLED;
					client.callEvent(listener -> listener.onConnect(client, address, connectionType));
					log.debug(
							"Created server peer using globally unique ID " + Long.toHexString(serverGuid).toUpperCase()
									+ " and maximum transfer unit with size of " + maximumTransferUnit + " bytes ("
									+ (maximumTransferUnit * 8) + " bits) for server address " + address);
					return new RakNetServerPeer(client, address, serverGuid, maximumTransferUnit, connectionType,
							channel);
				} else if (packet.getId() == ID_ALREADY_CONNECTED) {
					throw new AlreadyConnectedException(client, address);
				} else if (packet.getId() == ID_NO_FREE_INCOMING_CONNECTIONS) {
					throw new NoFreeIncomingConnectionsException(client, address);
				} else if (packet.getId() == ID_CONNECTION_BANNED) {
					ConnectionBanned connectionBanned = new ConnectionBanned(packet);
					connectionBanned.decode();
					if (connectionBanned.magic != true) {
						throw new InvalidMagicException(client);
					} else if (connectionBanned.serverGuid == serverGuid) {
						throw new ConnectionBannedException(client, address);
					}
				} else if (packet.getId() == ID_INCOMPATIBLE_PROTOCOL_VERSION) {
					IncompatibleProtocolVersion incompatibleProtocol = new IncompatibleProtocolVersion(packet);
					incompatibleProtocol.decode();
					if (incompatibleProtocol.serverGuid == serverGuid) {
						throw new IncompatibleProtocolException(client, address, client.getProtocolVersion(),
								incompatibleProtocol.networkProtocol);
					}
				}
			} catch (PeerFactoryException | PacketBufferException e) {
				this.exceptionCaught(e);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "PeerFactory [factoryState=" + factoryState + ", client=" + client + ", address=" + address
				+ ", initialMaximumTransferUnit=" + initialMaximumTransferUnit + ", maximumMaximumTransferUnit="
				+ maximumMaximumTransferUnit + ", serverGuid=" + serverGuid + ", maximumTransferUnit="
				+ maximumTransferUnit + ", connectionType=" + connectionType + "]";
	}

}
