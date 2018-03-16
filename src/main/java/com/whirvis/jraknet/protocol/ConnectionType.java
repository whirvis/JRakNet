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
 * Copyright (c) 2016-2018 Trent Summerlin
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
package com.whirvis.jraknet.protocol;

import java.util.HashMap;
import java.util.UUID;

/**
 * Used to signify which implementation of the RakNet protocol is being used by
 * a connection. Keep in mind that this functionality has <i>no</i> guarantees
 * to function completely, as it is completely dependent on the implementation
 * to implement this feature.
 * 
 * @author Trent Summerlin
 */
public class ConnectionType {

	public static final ConnectionType VANILLA = new ConnectionType(null, null, null, null);

	// Connection type data
	private final UUID uuid;
	private final String name;
	private final String language;
	private final String version;
	private final HashMap<String, String> metadata;

	public ConnectionType(UUID uuid, String name, String language, String version, HashMap<String, String> metadata) {
		this.uuid = uuid;
		this.name = name;
		this.language = language;
		this.version = version;
		this.metadata = metadata;
	}

	public ConnectionType(UUID uuid, String name, String language, String version) {
		this(uuid, name, language, version, new HashMap<String, String>());
	}

	/**
	 * @return the universally unique ID of the implementation.
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * @return the name of the implementation.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the programming language of the implementation.
	 */
	public String getLanguage() {
		return this.language;
	}

	/**
	 * @return the version of the implementation.
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * @param key
	 *            the key of the value to retrieve.
	 * @return the value associated with the specified key.
	 */
	public String getMetaData(String key) {
		return metadata.get(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConnectionType) {
			ConnectionType connectionType = (ConnectionType) obj;
			if (connectionType.getUUID() != null) {
				if (connectionType.getUUID().equals(uuid)) {
					return true;
				}
			} else if (connectionType.getUUID() == null && uuid == null) {
				return true;
			}
		}
		return false;
	}

}