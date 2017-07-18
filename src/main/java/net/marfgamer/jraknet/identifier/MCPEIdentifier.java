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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet.identifier;

import net.marfgamer.jraknet.util.RakNetUtils;

/**
 * Represents an identifier from a Minecraft: Pocket Edition server.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class MCPEIdentifier extends Identifier {

	private static final char[] VERSION_TAG_ALPHABET = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'.' };
	private static final String HEADER = "MCPE";
	private static final String SEPERATOR = ";";
	private static final int DATA_COUNT_LEGACY = 6;
	private static final int DATA_COUNT = 9;

	/**
	 * @param versionTag
	 *            the version tag to validate.
	 * @return true if the version tag is valid.
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
	 * @param identifier
	 *            the identifier to check.
	 * @return true if the specified identifier is a Minecraft: Pocket Edition
	 *         identifier.
	 */
	public static boolean isMCPEIdentifier(Identifier identifier) {
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
	 * Constructs an <code>MCPEIdentifier</code> with the specified server name,
	 * server protocol, version tag, online player count, max player count,
	 * globally unique ID, world name, and gamemode.
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
	public MCPEIdentifier(String serverName, int serverProtocol, String versionTag, int onlinePlayerCount,
			int maxPlayerCount, long guid, String worldName, String gamemode) {
		this.serverName = serverName;
		this.serverProtocol = serverProtocol;
		this.versionTag = versionTag;
		this.onlinePlayerCount = onlinePlayerCount;
		this.maxPlayerCount = maxPlayerCount;
		this.guid = guid;
		this.worldName = worldName;
		this.gamemode = gamemode;
		this.legacy = false;

		if (this.versionTag != null) {
			if (verifyVersionTag(this.versionTag) == false) {
				throw new IllegalArgumentException("Invalid version tag");
			}
		}
	}

	/**
	 * Constructs a <code>MCPEIdentifer</code> by parsing the specified
	 * <code>Identifier</code>.
	 * 
	 * @param identifier
	 *            the <code>Identifier</code> to parse.
	 */
	public MCPEIdentifier(Identifier identifier) {
		String[] data = identifier.build().split(SEPERATOR);
		if (data.length >= DATA_COUNT_LEGACY) {
			if (data[0].equals(HEADER) == false) {
				throw new IllegalArgumentException("Invalid header");
			}

			this.serverName = data[1];
			this.serverProtocol = RakNetUtils.parseIntPassive(data[2]);
			this.versionTag = data[3];
			this.onlinePlayerCount = RakNetUtils.parseIntPassive(data[4]);
			this.maxPlayerCount = RakNetUtils.parseIntPassive(data[5]);
			this.legacy = true;

			if (data.length >= DATA_COUNT) {
				this.guid = RakNetUtils.parseLongPassive(data[6]);
				this.worldName = data[7];
				this.gamemode = data[8];
				this.legacy = false;
			}

			if (verifyVersionTag(this.versionTag) == false) {
				throw new IllegalArgumentException("Invalid version tag");
			}
		}
	}

	/**
	 * Constructs a <code>MCPEIdentifer</code> by parsing the specified String
	 * identifier.
	 * 
	 * @param identifier
	 *            the identifier to parse.
	 */
	public MCPEIdentifier(String identifier) {
		this(new Identifier(identifier));
	}

	/**
	 * Constructs a blank <code>MCPEIdentifier</code>.
	 */
	public MCPEIdentifier() {
		this("", -1, "", -1, -1, -1, "", "");
	}

	/**
	 * @return the server name.
	 */
	public String getServerName() {
		return this.serverName;
	}

	/**
	 * @return the server protocol.
	 */
	public int getServerProtocol() {
		return this.serverProtocol;
	}

	/**
	 * @return the version tag.
	 */
	public String getVersionTag() {
		return this.versionTag;
	}

	/**
	 * @return the online player count.
	 */
	public int getOnlinePlayerCount() {
		return this.onlinePlayerCount;
	}

	/**
	 * @return the max player count.
	 */
	public int getMaxPlayerCount() {
		return this.maxPlayerCount;
	}

	/**
	 * @return the globally unique ID.
	 */
	public long getGloballyUniqueId() {
		return this.guid;
	}

	/**
	 * @return the world name.
	 */
	public String getWorldName() {
		return this.worldName;
	}

	/**
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
	 * @return true if the version tag was set
	 */
	public boolean setVersionTag(String versionTag) {
		if (verifyVersionTag(versionTag)) {
			this.versionTag = versionTag;
			return true;
		}
		return false;
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
	 * @return true if the identifier is in legacy mode.
	 */
	public boolean isLegacyMode() {
		return this.legacy;
	}

	@Override
	public String build() {
		if (this.legacy == true) {
			return (HEADER + SEPERATOR + serverName + SEPERATOR + serverProtocol + SEPERATOR + versionTag + SEPERATOR
					+ onlinePlayerCount + SEPERATOR + maxPlayerCount);
		} else {
			return (HEADER + SEPERATOR + serverName + SEPERATOR + serverProtocol + SEPERATOR + versionTag + SEPERATOR
					+ onlinePlayerCount + SEPERATOR + maxPlayerCount + SEPERATOR + guid + SEPERATOR + worldName
					+ SEPERATOR + gamemode);
		}
	}

}
