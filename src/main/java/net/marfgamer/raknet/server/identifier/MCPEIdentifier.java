package net.marfgamer.raknet.server.identifier;

import net.marfgamer.raknet.utils.RakNetUtils;

public class MCPEIdentifier extends Identifier {

	private static final String HEADER = "MCPE";
	private static final String SEPERATOR = ";";
	private static final int DATA_COUNT = 5;

	private String serverName;
	private int serverProtocol;
	private String versionTag;
	private int onlinePlayerCount;
	private int maxPlayerCount;

	public MCPEIdentifier(String serverName, int serverProtocol, String versionTag, int onlinePlayerCount,
			int maxPlayerCount) {
		super(null); // We don't have any data yet
		this.serverName = serverName;
		this.serverProtocol = serverProtocol;
		this.versionTag = versionTag;
		this.onlinePlayerCount = onlinePlayerCount;
		this.maxPlayerCount = maxPlayerCount;
		this.updateBuild(); // Now we have data
	}

	public MCPEIdentifier(Identifier identifier) {
		super(null); // We don't have any data yet
		String[] data = identifier.build().split(SEPERATOR);
		if (data.length >= DATA_COUNT) {
			this.serverName = data[0];
			this.serverProtocol = RakNetUtils.parseIntPassive(data[1]);
			this.versionTag = data[2];
			this.onlinePlayerCount = RakNetUtils.parseIntPassive(data[3]);
			this.maxPlayerCount = RakNetUtils.parseIntPassive(data[4]);
		}
	}

	public MCPEIdentifier() {
		this(null, -1, null, -1, -1);
	}

	public String getServerName() {
		return this.serverName;
	}

	public int getServerProtocol() {
		return this.serverProtocol;
	}

	public String getVersionTag() {
		return this.versionTag;
	}

	public int getOnlinePlayerCount() {
		return this.onlinePlayerCount;
	}

	public int getMaxPlayerCount() {
		return this.maxPlayerCount;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
		this.updateBuild();
	}

	public void setServerProtocol(int serverProtocol) {
		this.serverProtocol = serverProtocol;
		this.updateBuild();
	}

	public void setVersionTag(String versionTag) {
		this.versionTag = versionTag;
		this.updateBuild();
	}

	public void setOnlinePlayerCount(int onlinePlayerCount) {
		this.onlinePlayerCount = onlinePlayerCount;
		this.updateBuild();
	}

	public void setMaxPlayerCount(int maxPlayerCount) {
		this.maxPlayerCount = maxPlayerCount;
		this.updateBuild();
	}

	private void updateBuild() {
		this.identifier = (HEADER + SEPERATOR + serverName + SEPERATOR + serverProtocol + SEPERATOR + versionTag
				+ SEPERATOR + onlinePlayerCount + SEPERATOR + maxPlayerCount);
	}

}
