package net.marfgamer.raknet.session;

public enum RakNetState {

	DISCONNECTED(0), HANDSHAKING(1), CONNECTED(2);

	private final int order;

	private RakNetState(int order) {
		this.order = order;
	}

	/**
	 * Get's the order the state is in as a integer value
	 * 
	 * @return int
	 */
	public int getOrder() {
		return this.order;
	}

	/**
	 * Gets the state by it's numerical order
	 * 
	 * @param order
	 *            The order of the state
	 * @return SessionState
	 */
	public static RakNetState getState(int order) {
		for (RakNetState state : RakNetState.values()) {
			if (state.getOrder() == order) {
				return state;
			}
		}
		return null;
	}

}
