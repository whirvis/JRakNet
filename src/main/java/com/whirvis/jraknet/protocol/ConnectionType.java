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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.protocol;

import java.util.HashMap;
import java.util.UUID;

/**
 * Used to signify which implementation of the RakNet protocol is being used by
 * a connection. Keep in mind that this functionality has <i>no</i> guarantees
 * to function completely, as it is completely dependent on the implementation
 * to implement this feature.
 * 
 * @author Trent "Whirvis" Summerlin
 */
public class ConnectionType {

	// Maximum value of unsigned byte
	public static final int MAX_METADATA_VALUES = 0xFF;

	/**
	 * Converts the specified metadata keys and values to a
	 * <code>HashMap</code>.
	 * 
	 * @param metadata
	 *            the metadata keys and values.
	 * @return the metadata as a <code>HashMap</code>.
	 */
	public static HashMap<String, String> createMetaData(String... metadata) {
		// Validate input
		if (metadata.length % 2 != 0) {
			throw new IllegalArgumentException("There must be a value for every key");
		} else if (metadata.length / 2 > MAX_METADATA_VALUES) {
			throw new IllegalArgumentException("Too many metadata values");
		}

		// Generate HashMap
		HashMap<String, String> metadataMap = new HashMap<String, String>();
		for (int i = 0; i < metadata.length; i += 2) {
			metadataMap.put(metadata[i], metadata[i + 1]);
		}
		return metadataMap;
	}

	/**
	 * A connection from a vanilla client or an unknown implementation.
	 */
	public static final ConnectionType VANILLA = new ConnectionType(null, "Vanilla", null, null, null, true);

	/**
	 * A JRakNet connection.
	 */
	public static final ConnectionType JRAKNET = new ConnectionType(
			UUID.fromString("504da9b2-a31c-4db6-bcc3-18e5fe2fb178"), "JRakNet", "Java", "2.9.2");

	// Connection type header magic
	public static final byte[] MAGIC = new byte[] { (byte) 0x03, (byte) 0x08, (byte) 0x05, (byte) 0x0B, 0x43,
			(byte) 0x54, (byte) 0x49 };

	// Connection type data
	private final UUID uuid;
	private final String name;
	private final String language;
	private final String version;
	private final HashMap<String, String> metadata;
	private final boolean vanilla;

	private ConnectionType(UUID uuid, String name, String language, String version, HashMap<String, String> metadata,
			boolean vanilla) {
		this.uuid = uuid;
		this.name = name;
		this.language = language;
		this.version = version;
		this.metadata = metadata = (metadata != null ? metadata : new HashMap<String, String>());
		if (metadata.size() > MAX_METADATA_VALUES) {
			throw new IllegalArgumentException("Too many metadata values");
		}
		this.vanilla = vanilla;
	}

	public ConnectionType(UUID uuid, String name, String language, String version, HashMap<String, String> metadata) {
		this(uuid, name, language, version, metadata, false);
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

	/**
	 * @return a copy of the metadata HashMap of the connection type.
	 */
	@SuppressWarnings("unchecked") // We know what type the clone is
	public HashMap<String, String> getMetaData() {
		return (HashMap<String, String>) metadata.clone();
	}

	/**
	 * @return whether or not the connection type is of the <code>VANILLA</code>
	 *         implementation.
	 */
	public boolean isVanilla() {
		return this.vanilla;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConnectionType) {
			ConnectionType connectionType = (ConnectionType) obj;
			if (connectionType.getUUID() != null) {
				if (connectionType.getUUID().equals(uuid)) {
					return true;
				}
			}
		}
		return false;
	}

}