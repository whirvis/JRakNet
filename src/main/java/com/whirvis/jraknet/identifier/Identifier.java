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
package com.whirvis.jraknet.identifier;

import com.whirvis.jraknet.protocol.ConnectionType;

/**
 * Represents an identifier sent from a server on the local network, any class
 * extending this only has to override the <code>build()</code> method in order
 * to have their identifier be dynamic.
 *
 * @author Whirvis T. Wheatley
 */
public class Identifier {

	private final String identifier;
	private final ConnectionType connectionType;

	public Identifier(String identifier, ConnectionType connectionType) {
		this.identifier = identifier;
		this.connectionType = connectionType;
	}

	public Identifier(String identifier) {
		this.identifier = identifier;
		this.connectionType = ConnectionType.JRAKNET;
	}

	public Identifier(Identifier identifier) {
		this.identifier = identifier.identifier;
		this.connectionType = identifier.connectionType;
	}

	public Identifier() {
		this.identifier = null;
		this.connectionType = ConnectionType.JRAKNET;
	}

	/**
	 * Returns the identifier as a <code>String</code>.
	 * 
	 * @return the identifier as a <code>String</code>.
	 */
	public String build() {
		return this.identifier;
	}

	/**
	 * Returns the connection type of the sender of the identifier.
	 * 
	 * @return the connection type of the sender of the identifier.
	 */
	public final ConnectionType getConnectionType() {
		return this.connectionType;
	}

	@Override
	public final String toString() {
		return this.build();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Identifier) {
			Identifier identifier = (Identifier) object;
			if (this.build() == null && identifier.build() == null) {
				return true; // We must check this first
			}
			return this.build().equals(identifier.build());
		}
		return false;
	}

}
