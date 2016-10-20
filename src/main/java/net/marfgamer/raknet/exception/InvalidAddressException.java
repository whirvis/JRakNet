package net.marfgamer.raknet.exception;

public class InvalidAddressException extends RakNetException {

	private static final long serialVersionUID = 6091226206755700292L;

	public InvalidAddressException(int protocol) {
		super("Unknown protocol IPv" + protocol);
	}

	public InvalidAddressException() {
		super("Invalid address!");
	}

}
