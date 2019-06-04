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
package com.whirvis.jraknet.protocol;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

/**
 * Used to signify which implementation of the RakNet protocol is being used by
 * a connection. This functionality has <i>no</i> guarantee of functioning
 * completely, as it is dependent on the implementation to implement this
 * feature themselves.
 * <p>
 * As of March 1st, 2019, the only known implementations using this connection
 * type protocol are:
 * <ul>
 * <li>JRakNet by Whirvis T. Wheatley</li>
 * <li>RakLib by PocketMine-MP</li>
 * </ul>
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.9.0
 * @see com.whirvis.jraknet.identifier.Identifier Identifier
 */
public final class ConnectionType {

	public static final int MAX_METADATA_VALUES = 0xFF;
	public static final byte[] MAGIC = new byte[] { (byte) 0x03, (byte) 0x08, (byte) 0x05, (byte) 0x0B, 0x43, (byte) 0x54, (byte) 0x49 };

	/**
	 * Converts the metadata keys and values to a {@link HashMap}.
	 * 
	 * @param metadata
	 *            the metadata keys and values.
	 * @return the metadata as a {@link HashMap}.
	 * @throws IllegalArgumentException
	 *             if there is a key without a value or if there are more than
	 *             {@value #MAX_METADATA_VALUES} metadata values.
	 */
	public static HashMap<String, String> createMetaData(String... metadata) throws IllegalArgumentException {
		if (metadata.length % 2 != 0) {
			throw new IllegalArgumentException("There must be a value for every key");
		} else if (metadata.length / 2 > MAX_METADATA_VALUES) {
			throw new IllegalArgumentException("Too many metadata values");
		}
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
	public static final ConnectionType JRAKNET = new ConnectionType(UUID.fromString("504da9b2-a31c-4db6-bcc3-18e5fe2fb178"), "JRakNet", "Java", "2.11.2");

	/**
	 * A RakLib connection.
	 */
	public static final ConnectionType RAKLIB = new ConnectionType(UUID.fromString("41fd1c2f-de79-4434-8fbc-82f3f71214c6"), "RakLib", "PHP", "0.12.3");

	private final UUID uuid;
	private final String name;
	private final String language;
	private final String version;
	private final HashMap<String, String> metadata;
	private final boolean vanilla;

	/**
	 * Creates a connection type implementation descriptor.
	 * 
	 * @param uuid
	 *            the universally unique ID of the implementation
	 * @param name
	 *            the name of the implementation.
	 * @param language
	 *            the name of the programming language the implementation was
	 *            programmed in.
	 * @param version
	 *            the version of the implementation.
	 * @param metadata
	 *            the metadata of the implementation. Metadata for an
	 *            implementation can be created using the
	 *            {@link #createMetaData(String...)} method.
	 * @param vanilla
	 *            <code>true</code> if the implementation is a vanilla
	 *            implementation, <code>false</code> otherwise.
	 * @throws IllegalArgumentException
	 *             if there are more than {@value #MAX_METADATA_VALUES} metadata
	 *             values.
	 */
	private ConnectionType(UUID uuid, String name, String language, String version, HashMap<String, String> metadata, boolean vanilla) throws IllegalArgumentException {
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

	/**
	 * Creates a connection type implementation descriptor.
	 * 
	 * @param uuid
	 *            the universally unique ID of the implementation
	 * @param name
	 *            the name of the implementation.
	 * @param language
	 *            the name of the programming language the implementation was
	 *            programmed in.
	 * @param version
	 *            the version of the implementation.
	 * @param metadata
	 *            the metadata of the implementation. Metadata for an
	 *            implementation can be created using the
	 *            {@link #createMetaData(String...)} method.
	 */
	public ConnectionType(UUID uuid, String name, String language, String version, HashMap<String, String> metadata) {
		this(uuid, name, language, version, metadata, false);
	}

	/**
	 * Creates a connection type implementation descriptor.
	 * 
	 * @param uuid
	 *            the universally unique ID of the implementation
	 * @param name
	 *            the name of the implementation.
	 * @param language
	 *            the programming language the implementation.
	 * @param version
	 *            the version of the implementation.
	 */
	public ConnectionType(UUID uuid, String name, String language, String version) {
		this(uuid, name, language, version, new HashMap<String, String>());
	}

	/**
	 * Returns the universally unique ID of the implementation.
	 * 
	 * @return the universally unique ID of the implementation.
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Returns the name of the implementation.
	 * 
	 * @return the name of the implementation.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the programming language of the implementation.
	 * 
	 * @return the programming language of the implementation.
	 */
	public String getLanguage() {
		return this.language;
	}

	/**
	 * Returns the version of the implementation.
	 * 
	 * @return the version of the implementation.
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Returns the value of the metadata with the specified key.
	 * 
	 * @param key
	 *            the key of the value to retrieve.
	 * @return the value associated with the key, <code>null</code> if there is
	 *         none.
	 */
	public String getMetaData(String key) {
		return metadata.get(key);
	}

	/**
	 * Returns a cloned copy of the metadata.
	 * 
	 * @return a cloned copy of the metadata.
	 */
	@SuppressWarnings("unchecked") // Clone type is known
	public HashMap<String, String> getMetaData() {
		return (HashMap<String, String>) metadata.clone();
	}

	/**
	 * Returns whether or not the connection type is a {@link #VANILLA}
	 * implementation.
	 * 
	 * @return <code>true</code> if the connection type is a {@link #VANILLA}
	 *         implementation, <code>false</code> otherwise.
	 */
	public boolean isVanilla() {
		return this.vanilla;
	}

	/**
	 * Returns whether or not this implementation and the specified
	 * implementation are the same implementation based on the UUID.
	 * <p>
	 * If the UUID of both implementations are <code>null</code> then
	 * <code>false</code> will be returned since we have no logical way of
	 * telling if the two implementations are actually the same as there are no
	 * UUIDs to compare.
	 * 
	 * @param connectionType
	 *            the connection type.
	 * @return <code>true</code> if both implementations are the same,
	 *         <code>false</code> otherwise.
	 */
	public boolean is(ConnectionType connectionType) {
		if (connectionType == null) {
			return false; // No implementation
		} else if (connectionType.uuid == null || uuid == null) {
			return false; // No UUID
		}
		return uuid.equals(connectionType.uuid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, name, language, version, metadata, vanilla);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof ConnectionType)) {
			return false;
		}
		ConnectionType ct = (ConnectionType) o;
		return Objects.equals(uuid, ct.uuid) && Objects.equals(name, ct.name) && Objects.equals(language, ct.language) && Objects.equals(version, ct.version)
				&& Objects.equals(metadata, ct.metadata) && Objects.equals(vanilla, ct.vanilla);
	}

	@Override
	public String toString() {
		return "ConnectionType [uuid=" + uuid + ", name=" + name + ", language=" + language + ", version=" + version + ", metadata=" + metadata + ", vanilla=" + vanilla + "]";
	}

}