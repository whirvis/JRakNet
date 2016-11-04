package net.marfgamer.raknet.example.chat.server.command;

import net.marfgamer.raknet.example.chat.server.ChatServer;

public class StopCommand extends Command {

	private final ChatServer server;

	public StopCommand(ChatServer server) {
		super(false, "stop", "stop");
		this.server = server;
	}

	@Override
	public boolean handleCommand(String[] args) {
		server.stop();
		return true;
	}

}
