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
 * Copyright (c) 2016 MarfGamer
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
package net.marfgamer.raknet.identifier;

import net.marfgamer.raknet.util.RakNetUtils;

/**
 * This class represents an identifier from Minecraft: Pocket Edition servers
 *
 * @author MarfGamer
 */
public class MCPEIdentifier extends Identifier {

	private static final char[] VERSION_TAG_ALPHABET = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'.' };
	private static final String HEADER = "MCPE";
	private static final String SEPERATOR = ";";
	private static final int DATA_COUNT_LEGACY = 6;
	private static final int DATA_COUNT = 9;

	/**
	 * Returns whether or not the specified version tag is valid
	 * 
	 * @param versionTag
	 *            The version tag to validate
	 * @return Whether or not the version tag is valid
	 */
	private static boolean verifyVersionTag(String versionTag) {
		for (char vtc : versionTag.toCharArray()) {
			boolean valid = false;

			for (char vtac : VERSION_TAG_ALPHABET) {
				if (vtac == vtc) {
					valid = true;
					break;
				}
			}

			if (valid == false) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns whether or not the specified identifier is a Minecraft: Pocket
	 * Edition identifier
	 * 
	 * @param identifier
	 *            The identifier to check
	 * @return Whether or not the specified identifier is a Miencraft: Pocket
	 *         Edition identifier
	 */
	public static boolean isMCPEIdentifier(Identifier identifier) {
		return identifier.build().startsWith(HEADER);
	}

	private String serverName;
	private int serverProtocol;
	private String versionTag;
	private int onlinePlayerCount;
	private int maxPlayerCount;
	private long timestamp; // Not really sure what this is
	private String worldName;
	private String gamemode;
	private boolean legacy;

	public MCPEIdentifier(String serverName, int serverProtocol, String versionTag, int onlinePlayerCount,
			int maxPlayerCount, long timestamp, String worldName, String gamemode) {
		this.serverName = serverName;
		this.serverProtocol = serverProtocol;
		this.versionTag = versionTag;
		this.onlinePlayerCount = onlinePlayerCount;
		this.maxPlayerCount = maxPlayerCount;
		this.timestamp = timestamp;
		this.worldName = worldName;
		this.gamemode = gamemode;
		this.legacy = false;

		if (this.versionTag != null) {
			if (verifyVersionTag(this.versionTag) == false) {
				throw new IllegalArgumentException("Invalid version tag!");
			}
		}
	}

	public MCPEIdentifier(Identifier identifier) {
		String[] data = identifier.build().split(SEPERATOR);
		if (data.length >= DATA_COUNT_LEGACY) {
			if (data[0].equals(HEADER) == false) {
				throw new IllegalArgumentException("Invalid header!");
			}

			this.serverName = data[1];
			this.serverProtocol = RakNetUtils.parseIntPassive(data[2]);
			this.versionTag = data[3];
			this.onlinePlayerCount = RakNetUtils.parseIntPassive(data[4]);
			this.maxPlayerCount = RakNetUtils.parseIntPassive(data[5]);
			this.legacy = true;

			if (data.length >= DATA_COUNT) {
				this.timestamp = RakNetUtils.parseLongPassive(data[6]);
				this.worldName = data[7];
				this.gamemode = data[8];
				this.legacy = false;
			}

			if (verifyVersionTag(this.versionTag) == false) {
				throw new IllegalArgumentException("Invalid version tag!");
			}
		}
	}

	public MCPEIdentifier(String identifier) {
		this(new Identifier(identifier));
	}

	public MCPEIdentifier() {
		this("", -1, "", -1, -1, -1, "", "");
	}

	/**
	 * Returns the server name
	 * 
	 * @return The server name
	 */
	public String getServerName() {
		return this.serverName;
	}

	/**
	 * Returns the server protocol
	 * 
	 * @return The server protocol
	 */
	public int getServerProtocol() {
		return this.serverProtocol;
	}

	/**
	 * Returns the version tag
	 * 
	 * @return The version tag
	 */
	public String getVersionTag() {
		return this.versionTag;
	}

	/**
	 * Returns the online player count
	 * 
	 * @return The online player count
	 */
	public int getOnlinePlayerCount() {
		return this.onlinePlayerCount;
	}

	/**
	 * Returns the max player count
	 * 
	 * @return The max player count
	 */
	public int getMaxPlayerCount() {
		return this.maxPlayerCount;
	}

	/**
	 * Returns the timestamp
	 * 
	 * @return The timestamp
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Returns the world name
	 * 
	 * @return The world name
	 */
	public String getWorldName() {
		return this.worldName;
	}

	/**
	 * Returns the gamemode
	 * 
	 * @return The gamemode
	 */
	public String getGamemode() {
		return this.gamemode;
	}

	/**
	 * Sets the server name
	 * 
	 * @param serverName
	 *            The new server name
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	/**
	 * Sets the server protocol
	 * 
	 * @param serverProtocol
	 *            The new server protocol
	 */
	public void setServerProtocol(int serverProtocol) {
		this.serverProtocol = serverProtocol;
	}

	/**
	 * Sets the version tag
	 * 
	 * @param versionTag
	 *            The new version tag
	 * @return Whether or not the version tag was set
	 */
	public boolean setVersionTag(String versionTag) {
		if (verifyVersionTag(versionTag)) {
			this.versionTag = versionTag;
			return true;
		}
		return false;
	}

	/**
	 * Sets the online player count
	 * 
	 * @param onlinePlayerCount
	 *            The new online player count
	 */
	public void setOnlinePlayerCount(int onlinePlayerCount) {
		this.onlinePlayerCount = onlinePlayerCount;
	}

	/**
	 * Sets the max player count
	 * 
	 * @param maxPlayerCount
	 *            The new max player count
	 */
	public void setMaxPlayerCount(int maxPlayerCount) {
		this.maxPlayerCount = maxPlayerCount;
	}

	/**
	 * Sets the timestamp
	 * 
	 * @param timestamp
	 *            The new timestamp
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Sets the world name
	 * 
	 * @param worldName
	 *            The new world name
	 */
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	/**
	 * Sets the gamemode
	 * 
	 * @param gamemode
	 *            The new gamemode
	 */
	public void setGamemode(String gamemode) {
		this.gamemode = gamemode;
	}

	/**
	 * Enables/Disables the legacy builder
	 * 
	 * @param legacy
	 *            The legacy toggle
	 */
	public void setLegacy(boolean legacy) {
		this.legacy = legacy;
	}

	/**
	 * Returns whether or not the identifier is legacy
	 * 
	 * @return Whether or not the identifier is legacy
	 */
	public boolean isLegacy() {
		return this.legacy;
	}

	@Override
	public String build() {
		if (this.legacy == true) {
			return (HEADER + SEPERATOR + serverName + SEPERATOR + serverProtocol + SEPERATOR + versionTag + SEPERATOR
					+ onlinePlayerCount + SEPERATOR + maxPlayerCount);
		} else {
			return (HEADER + SEPERATOR + serverName + SEPERATOR + serverProtocol + SEPERATOR + versionTag + SEPERATOR
					+ onlinePlayerCount + SEPERATOR + maxPlayerCount + SEPERATOR + timestamp + SEPERATOR + worldName
					+ SEPERATOR + gamemode);
		}
	}

}
