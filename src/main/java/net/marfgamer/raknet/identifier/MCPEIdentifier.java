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
	private static final int DATA_COUNT = 5;

	/**
	 * Returns whether or not the specified version tag is valid
	 * 
	 * @param versionTag
	 *            - The version tag to validate
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

			if (valid == false)
				return false;
		}
		return true;
	}

	private String serverName;
	private int serverProtocol;
	private String versionTag;
	private int onlinePlayerCount;
	private int maxPlayerCount;
	private long timestamp; // Not really sure what this is
	private String worldName;
	private String gamemode;

	public MCPEIdentifier(String serverName, int serverProtocol, String versionTag, int onlinePlayerCount,
			int maxPlayerCount, long timestamp, String worldName, String gamemode) {
		super(null); // We don't have any data yet

		this.serverName = serverName;
		this.serverProtocol = serverProtocol;
		this.versionTag = versionTag;
		this.onlinePlayerCount = onlinePlayerCount;
		this.maxPlayerCount = maxPlayerCount;
		this.timestamp = timestamp;
		this.worldName = worldName;
		this.gamemode = gamemode;

		if (verifyVersionTag(this.versionTag) == false) {
			throw new IllegalArgumentException("Invalid version tag!");
		}

		this.updateBuild(); // Now we have data
	}

	public MCPEIdentifier(Identifier identifier) {
		super(null); // We don't have any data yet
		String[] data = identifier.build().split(SEPERATOR);
		if (data.length >= DATA_COUNT) {
			if (data[0].equals(HEADER) == false) {
				throw new IllegalArgumentException("Invalid header!");
			}

			this.serverName = data[1];
			this.serverProtocol = RakNetUtils.parseIntPassive(data[2]);
			this.versionTag = data[3];
			this.onlinePlayerCount = RakNetUtils.parseIntPassive(data[4]);
			this.maxPlayerCount = RakNetUtils.parseIntPassive(data[5]);
			this.timestamp = RakNetUtils.parseLongPassive(data[6]);
			this.worldName = data[7];
			this.gamemode = data[8];

			if (verifyVersionTag(this.versionTag) == false) {
				throw new IllegalArgumentException("Invalid version tag!");
			}
		}
	}

	public MCPEIdentifier() {
		this(null, -1, null, -1, -1, -1, null, null);
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
	 *            - The new server name
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
		this.updateBuild();
	}

	/**
	 * Sets the server protocol
	 * 
	 * @param serverProtocol
	 *            - The new server protocol
	 */
	public void setServerProtocol(int serverProtocol) {
		this.serverProtocol = serverProtocol;
		this.updateBuild();
	}

	/**
	 * Sets the version tag
	 * 
	 * @param versionTag
	 *            - The new version tag
	 * @return Whether or not the version tag was set
	 */
	public boolean setVersionTag(String versionTag) {
		if (verifyVersionTag(versionTag)) {
			this.versionTag = versionTag;
			this.updateBuild();
			return true;
		}
		return false;
	}

	/**
	 * Sets the online player count
	 * 
	 * @param onlinePlayerCount
	 *            - The new online player count
	 */
	public void setOnlinePlayerCount(int onlinePlayerCount) {
		this.onlinePlayerCount = onlinePlayerCount;
		this.updateBuild();
	}

	/**
	 * Sets the max player count
	 * 
	 * @param maxPlayerCount
	 *            - The new max player count
	 */
	public void setMaxPlayerCount(int maxPlayerCount) {
		this.maxPlayerCount = maxPlayerCount;
		this.updateBuild();
	}

	/**
	 * Sets the timestamp
	 * 
	 * @param timestamp
	 *            - The new timestamp
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Sets the world name
	 * 
	 * @param worldName
	 *            - The new world name
	 */
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	/**
	 * Sets the gamemode
	 * 
	 * @param gamemode
	 *            - The new gamemode
	 */
	public void setGamemode(String gamemode) {
		this.gamemode = gamemode;
	}

	/**
	 * Updates the identifier
	 */
	private void updateBuild() {
		this.identifier = (HEADER + SEPERATOR + serverName + SEPERATOR + serverProtocol + SEPERATOR + versionTag
				+ SEPERATOR + onlinePlayerCount + SEPERATOR + maxPlayerCount + SEPERATOR + timestamp + SEPERATOR
				+ worldName + SEPERATOR + gamemode);
	}

}
