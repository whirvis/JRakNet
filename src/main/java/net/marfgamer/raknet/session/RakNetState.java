package net.marfgamer.raknet.session;

/**
 * Represents the current status of a connection in a RakNetSession
 *
 * @author MarfGamer
 */
public enum RakNetState {

	DISCONNECTED(0), HANDSHAKING(1), CONNECTED(2);

	private final int order;

	private RakNetState(int order) {
		this.order = order;
	}

	/**
	 * Returns the order the state is in as an int value
	 * 
	 * @return The order the state is in as an int value
	 */
	public int getOrder() {
		return this.order;
	}

	/**
	 * Returns the state by it's numerical order
	 * 
	 * @param order
	 *            The order of the state
	 * @return the state by it's numerical order
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
