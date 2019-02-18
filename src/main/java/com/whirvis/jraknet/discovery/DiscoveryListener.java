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
package com.whirvis.jraknet.discovery;

import java.net.InetSocketAddress;

import com.whirvis.jraknet.identifier.Identifier;

/**
 * Used to listen for events that occur in the
 * {@link com.whirvis.jraknet.discovery.Discovery Discovery}. In order to listen
 * for events, one must use the
 * {@link com.whirvis.jraknet.discovery.Discovery#addListener(DiscoveryListener)
 * addListener(DiscoveryListener)} method.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0
 * @see com.whirvis.jraknet.discovery.Discovery#addListener(DiscoveryListener)
 *      addListener(DiscoveryListener)
 * @see com.whirvis.jraknet.discovery.Discovery#removeListener(DiscoveryListener)
 *      removeListener(DiscoveryListener)
 * @see com.whirvis.jraknet.discovery.Discovery Discovery
 */
public interface DiscoveryListener {

	/**
	 * Called when a server is discovered on the local network.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param external
	 *            <code>true</code> if the server is an external server,
	 *            <code>false</code> otherwise.
	 * @param identifier
	 *            the <code>Identifier</code> of the server.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public default void onServerDiscovered(InetSocketAddress address, boolean external, Identifier identifier) {
	}

	/**
	 * Called when the <code>Identifier</code> of an already discovered server
	 * changes.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param external
	 *            <code>true</code> if the server is an external server,
	 *            <code>false</code> otherwise.
	 * @param identifier
	 *            the new <code>Identifier</code>.
	 * @see com.whirvis.jraknet.identifier.Identifier Identifier
	 */
	public default void onServerIdentifierUpdate(InetSocketAddress address, boolean external, Identifier identifier) {
	}

	/**
	 * Called when a previously discovered server has been forgotten.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param external
	 *            <code>true</code> if the server is an external server,
	 *            <code>false</code> otherwise.
	 */
	public default void onServerForgotten(InetSocketAddress address, boolean external) {
	}

}
