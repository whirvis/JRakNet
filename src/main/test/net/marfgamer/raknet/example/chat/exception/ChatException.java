package net.marfgamer.raknet.example.chat.exception;

import net.marfgamer.raknet.exception.RakNetException;

public class ChatException extends Exception {

	private static final long serialVersionUID = 4497482433048975592L;

	private final String exception;

	public ChatException(String exception) {
		super(exception);
		this.exception = exception;
	}

	public ChatException(RakNetException exception) {
		this(exception.getMessage());
	}

	@Override
	public String getMessage() {
		return this.exception;
	}

	@Override
	public String getLocalizedMessage() {
		return this.exception;
	}

}
