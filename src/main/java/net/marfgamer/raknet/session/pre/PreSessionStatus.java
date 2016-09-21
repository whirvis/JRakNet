package net.marfgamer.raknet.session.pre;

public enum PreSessionStatus {

	CONNECTING_1(0), CONNECTING_2(1), INCOMPATIBLE_PROTOCOL(3, "Incompatible protocol!"), SERVER_FULL(4,
			"Server full"), DISCONNECTED(5, "Disconnected from server");

	private final int state;
	private final String disconnectMessage;

	private PreSessionStatus(int state, String disconnectMessage) {
		this.state = state;
		this.disconnectMessage = disconnectMessage;
	}

	private PreSessionStatus(int state) {
		this(state, null);
	}

	public int getState() {
		return this.state;
	}

	public String getDisconnectMessage() {
		return this.disconnectMessage;
	}

	public static PreSessionStatus getStatus(int state) {
		PreSessionStatus[] status = PreSessionStatus.values();
		for (int i = 0; i < status.length; i++) {
			if (status[i].getState() == state) {
				return status[i];
			}
		}
		return null;
	}

}
