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
package com.whirvis.jraknet.identifier;

import java.util.Objects;

import com.whirvis.jraknet.protocol.ConnectionType;

/**
 * Represents an identifier sent from a server in response to a client ping. Any
 * classes that extends this class must override the {@link #build()} method in
 * order to make use of the identifier capabilities.
 *
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v1.0.0
 */
public class Identifier implements Cloneable {

	private final String identifier;
	private final ConnectionType connectionType;

	/**
	 * Creates an identifier.
	 * 
	 * @param identifier
	 *            the identifier text.
	 * @param connectionType
	 *            the protocol implementation that sent the identifier.
	 */
	public Identifier(String identifier, ConnectionType connectionType) {
		this.identifier = identifier;
		this.connectionType = connectionType;
	}

	/**
	 * Creates an identifier with the connection type defaulting to the
	 * {@link com.whirvis.jraknet.protocol.ConnectionType#JRAKNET
	 * ConnectionType.JRAKNET} connection type.
	 * 
	 * @param identifier
	 *            the identifier text.
	 */
	public Identifier(String identifier) {
		this.identifier = identifier;
		this.connectionType = ConnectionType.JRAKNET;
	}

	/**
	 * Creates an identifier from another identifier.
	 * 
	 * @param identifier
	 *            the identifier to grab the information from.
	 */
	public Identifier(Identifier identifier) {
		this.identifier = identifier.identifier;
		this.connectionType = identifier.connectionType;
	}

	/**
	 * Creates an identifier with the identifier text being set to
	 * <code>null</code> and the connection type defaulting to the
	 * {@link com.whirvis.jraknet.protocol.ConnectionType#JRAKNET
	 * ConnectionType.JRAKNET} connection type.
	 */
	public Identifier() {
		this.identifier = null;
		this.connectionType = ConnectionType.JRAKNET;
	}

	/**
	 * Returns the identifier text.
	 * 
	 * @return the identifier text.
	 */
	public String build() {
		return this.identifier;
	}

	/**
	 * Returns the connection type of the sender/creator of the identifier.
	 * 
	 * @return the connection type of the sender/creator of the identifier.
	 */
	public final ConnectionType getConnectionType() {
		return this.connectionType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier, connectionType);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof Identifier)) {
			return false;
		}
		Identifier i = (Identifier) o;
		return Objects.equals(identifier, i.identifier) && Objects.equals(connectionType, i.connectionType);
	}

	@Override
	public final String toString() {
		return this.build();
	}

	@Override
	public Object clone() {
		return new Identifier(this.build(), connectionType);
	}

}
