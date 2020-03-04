/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.protocol.login;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.Failable;

/**
 * A <code>NEW_INCOMING_CONNECTION</code> packet.
 * <p>
 * This is sent by the client after receiving the
 * {@link ConnectionRequestAccepted CONNECTION_REQUEST_ACCEPTED} packet.
 * 
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v1.0.0
 */
public final class NewIncomingConnection extends RakNetPacket implements Failable {

	/**
	 * The server address.
	 */
	public InetSocketAddress serverAddress;

	/**
	 * The RakNet system addresses.
	 */
	public InetSocketAddress[] systemAddresses;

	/**
	 * The server timestamp.
	 */
	public long serverTimestamp;

	/**
	 * The client timestamp.
	 */
	public long clientTimestamp;

	/**
	 * Whether or not the packet failed to encode/decode.
	 */
	private boolean failed;

	/**
	 * Creates a <code>NEW_INCOMING_CONNECTION</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public NewIncomingConnection() {
		super(ID_NEW_INCOMING_CONNECTION);
		this.systemAddresses = new InetSocketAddress[RakNet.getSystemAddressCount()];
		for (int i = 0; i < systemAddresses.length; i++) {
			systemAddresses[i] = RakNet.SYSTEM_ADDRESS;
		}
	}

	/**
	 * Creates a <code>NEW_INCOMING_CONNECTION</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public NewIncomingConnection(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		try {
			this.writeAddress(serverAddress);
			for (int i = 0; i < systemAddresses.length; i++) {
				this.writeAddress(systemAddresses[i]);
			}
			this.writeLong(serverTimestamp);
			this.writeLong(clientTimestamp);
		} catch (UnknownHostException e) {
			this.failed = true;
			this.serverAddress = null;
			this.serverTimestamp = 0;
			this.clientTimestamp = 0;
			this.clear();
		}
	}

	@Override
	public void decode() {
		try {
			this.serverAddress = this.readAddress();
			this.systemAddresses = new InetSocketAddress[RakNet.getSystemAddressCount()];
			for (int i = 0; i < systemAddresses.length; i++) {
				if (this.remaining() > Long.SIZE + Long.SIZE) {
					this.systemAddresses[i] = this.readAddress();
				} else {
					this.systemAddresses[i] = RakNet.SYSTEM_ADDRESS;
				}
			}
			this.serverTimestamp = this.readLong();
			this.clientTimestamp = this.readLong();
		} catch (UnknownHostException e) {
			this.failed = true;
			this.serverAddress = null;
			this.serverTimestamp = 0;
			this.clientTimestamp = 0;
			this.clear();
		}
	}

	@Override
	public boolean failed() {
		return this.failed;
	}

}
