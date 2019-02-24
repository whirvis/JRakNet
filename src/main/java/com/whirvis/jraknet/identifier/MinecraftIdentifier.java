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

import com.whirvis.jraknet.RakNet;

/**
 * Represents a Minecraft™ identifier.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0
 */
public class MinecraftIdentifier extends Identifier {

	private static final String HEADER = "MCPE";
	private static final String SEPARATOR = ";";
	private static final int DATA_COUNT_LEGACY = 6;
	private static final int DATA_COUNT = 9;

	/**
	 * Returns whether or not the version tag is valid. In order for a version
	 * tag to be valid, it can only have numbers or periods. A <code>null</code>
	 * value is also valid, seeing as when the identifier is being built no
	 * version will be placed inside the identifier string.
	 * 
	 * @param versionTag
	 *            the version tag.
	 * @return <code>true</code> if the version tag is valid, <code>false</code>
	 *         otherwise.
	 */
	private static boolean verifyVersionTag(String versionTag) {
		if (versionTag != null) {
			for (char c : versionTag.toCharArray()) {
				if ((c < '0' || c > '9') && c != '.') {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns whether or not the the identifier is a Minecraft™ identifier.
	 * 
	 * @param identifier
	 *            the identifier to check.
	 * @return <code>true</code> if the identifier is a Minecraft™ identifier,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isMinecraftIdentifier(Identifier identifier) {
		if (identifier == null) {
			return false; // No identifier
		}
		return identifier.build().startsWith(HEADER);
	}

	private String serverName;
	private int serverProtocol;
	private String versionTag;
	private int onlinePlayerCount;
	private int maxPlayerCount;
	private long guid;
	private String worldName;
	private String gamemode;
	private boolean legacy;

	/**
	 * Creates a Minecraft™ identifier.
	 * 
	 * @param serverName
	 *            the server name.
	 * @param serverProtocol
	 *            the server protocol.
	 * @param versionTag
	 *            the version tag.
	 * @param onlinePlayerCount
	 *            the online player count.
	 * @param maxPlayerCount
	 *            the max player count.
	 * @param guid
	 *            the globally unique ID.
	 * @param worldName
	 *            the world name.
	 * @param gamemode
	 *            the gamemode.
	 */
	public MinecraftIdentifier(String serverName, int serverProtocol, String versionTag, int onlinePlayerCount,
			int maxPlayerCount, long guid, String worldName, String gamemode) throws IllegalArgumentException {
		this.serverName = serverName;
		this.serverProtocol = serverProtocol;
		this.versionTag = versionTag;
		this.onlinePlayerCount = onlinePlayerCount;
		this.maxPlayerCount = maxPlayerCount;
		this.guid = guid;
		this.worldName = worldName;
		this.gamemode = gamemode;
		this.legacy = false;
	}

	/**
	 * Creates a Minecraft™ identifier from an existing identifier.
	 * 
	 * @param identifier
	 *            the identifier.
	 * @throws NullPointerException
	 *             if the <code>identifier</code> or its contents are
	 *             <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>identifier</code> is not a Minecraft™ identifier
	 *             or there is not enough data present.
	 */
	public MinecraftIdentifier(Identifier identifier) throws NullPointerException, IllegalArgumentException {
		super(identifier);
		if (identifier == null) {
			throw new NullPointerException("Identifier cannot be null");
		} else if (identifier.build() == null) {
			throw new NullPointerException("Identifier contents cannot be null");
		} else if (!isMinecraftIdentifier(identifier)) {
			throw new IllegalArgumentException("Not a Minecraft identifier");
		}
		String[] data = identifier.build().split(SEPARATOR);
		if (data.length < DATA_COUNT_LEGACY) {
			throw new IllegalArgumentException("Not enough data");
		}
		for (int i = 0; i < data.length; i++) {
			data[i] = (data[i].length() > 0 ? data[i] : null);
		}
		this.serverName = data[1];
		this.serverProtocol = RakNet.parseIntPassive(data[2]);
		this.versionTag = data[3];
		this.onlinePlayerCount = RakNet.parseIntPassive(data[4]);
		this.maxPlayerCount = RakNet.parseIntPassive(data[5]);
		this.legacy = true;
		if (data.length >= DATA_COUNT) {
			this.guid = RakNet.parseLongPassive(data[6]);
			this.worldName = data[7];
			this.gamemode = data[8];
			this.legacy = false;
		}
	}

	/**
	 * Creates a Minecraft™ identifier from another identifier.
	 * 
	 * @param identifier
	 *            the identifier.
	 * @throws NullPointerException
	 *             if the <code>identifier</code> or its contents are
	 *             <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>identifier</code> is not a Minecraft identifier
	 *             or there is not enough data present.
	 */
	public MinecraftIdentifier(String identifier) throws NullPointerException, IllegalArgumentException {
		this(new Identifier(identifier));
	}

	/**
	 * Creates a blank Minecraft™ identifier.
	 */
	public MinecraftIdentifier() {
		this(null, 0, null, 0, 0, 0, null, null);
	}

	/**
	 * Returns the server name.
	 * 
	 * @return the server name.
	 */
	public String getServerName() {
		return this.serverName;
	}

	/**
	 * Returns the server protocol.
	 * 
	 * @return the server protocol.
	 */
	public int getServerProtocol() {
		return this.serverProtocol;
	}

	/**
	 * Returns the version tag.
	 * 
	 * @return the version tag.
	 */
	public String getVersionTag() {
		return this.versionTag;
	}

	/**
	 * Returns the online player count.
	 * 
	 * @return the online player count.
	 */
	public int getOnlinePlayerCount() {
		return this.onlinePlayerCount;
	}

	/**
	 * Returns the max player count.
	 * 
	 * @return the max player count.
	 */
	public int getMaxPlayerCount() {
		return this.maxPlayerCount;
	}

	/**
	 * Returns the globally unique ID.
	 * 
	 * @return the globally unique ID.
	 */
	public long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * Returns the world name.
	 * 
	 * @return the world name.
	 */
	public String getWorldName() {
		return this.worldName;
	}

	/**
	 * Returns the gamemode.
	 * 
	 * @return the gamemode.
	 */
	public String getGamemode() {
		return this.gamemode;
	}

	/**
	 * Sets the server name.
	 * 
	 * @param serverName
	 *            the new server name.
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	/**
	 * Sets the server protocol.
	 * 
	 * @param serverProtocol
	 *            the new server protocol.
	 */
	public void setServerProtocol(int serverProtocol) {
		this.serverProtocol = serverProtocol;
	}

	/**
	 * Sets the version tag.
	 * 
	 * @param versionTag
	 *            the new version tag.
	 * @throws IllegalArgumentException
	 *             if the version tag is invalid.
	 */
	public void setVersionTag(String versionTag) throws IllegalArgumentException {
		if (!verifyVersionTag(versionTag)) {
			throw new IllegalArgumentException("Invalid version tag");
		}
		this.versionTag = versionTag;
	}

	/**
	 * Sets the online player count.
	 * 
	 * @param onlinePlayerCount
	 *            the new online player count.
	 */
	public void setOnlinePlayerCount(int onlinePlayerCount) {
		this.onlinePlayerCount = onlinePlayerCount;
	}

	/**
	 * Sets the max player count.
	 * 
	 * @param maxPlayerCount
	 *            the new max player count.
	 */
	public void setMaxPlayerCount(int maxPlayerCount) {
		this.maxPlayerCount = maxPlayerCount;
	}

	/**
	 * Sets the globally unique ID.
	 * 
	 * @param guid
	 *            the new globally unique ID.
	 */
	public void setServerGloballyUniqueId(long guid) {
		this.guid = guid;
	}

	/**
	 * Sets the world name.
	 * 
	 * @param worldName
	 *            the new world name.
	 */
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	/**
	 * Sets the gamemode.
	 * 
	 * @param gamemode
	 *            the new gamemode.
	 */
	public void setGamemode(String gamemode) {
		this.gamemode = gamemode;
	}

	/**
	 * Enables/Disables the legacy builder.
	 * 
	 * @param legacy
	 *            the legacy toggle.
	 */
	public void setLegacyMode(boolean legacy) {
		this.legacy = legacy;
	}

	/**
	 * Returns whether or not the identifier is using the legacy builder.
	 * 
	 * @return <code>true</code> if the identifier is using the legacy builder,
	 *         <code>false</code> otherwise.
	 */
	public boolean isLegacyMode() {
		return this.legacy;
	}

	/**
	 * Converts the values to a Minecraft™ identifier string.
	 * 
	 * @param values
	 *            the values to write to the identifier.
	 * @return the built identifier as a <code>String</code>.
	 * @throws NullPointerException
	 *             if <code>values</code> is <code>null</code>.
	 */
	private String createBuildString(Object... values) throws NullPointerException {
		if (values == null) {
			throw new NullPointerException("Values cannot be null");
		}
		StringBuilder identifierBuilder = new StringBuilder();
		identifierBuilder.append(HEADER + SEPARATOR);
		for (int i = 0; i < values.length; i++) {
			identifierBuilder.append(values[i] != null ? values[i] : "");
			identifierBuilder.append(i + 1 < values.length ? SEPARATOR : "");
		}
		return identifierBuilder.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException
	 *             if the version tag is invalid.
	 */
	@Override
	public String build() {
		if (!verifyVersionTag(versionTag)) {
			throw new IllegalArgumentException("Invalid version tag");
		} else if (legacy == true) {
			return this.createBuildString(serverName, serverProtocol, versionTag, onlinePlayerCount, maxPlayerCount);
		}
		return this.createBuildString(serverName, serverProtocol, versionTag, onlinePlayerCount, maxPlayerCount, guid,
				worldName, gamemode);
	}

}
