package net.marfgamer.raknet.example.chat.server.command;

import net.marfgamer.raknet.example.chat.server.ChatServer;

public class BroadcastCommand extends Command {

	private final ChatServer server;

	public BroadcastCommand(ChatServer server) {
		super("broadcast", "broadcast <message> [id]");
		this.server = server;
	}

	@Override
	public boolean handleCommand(String[] args) {
		if (args.length >= 1) {
			String message = remainingArguments(0, args);
			server.broadcastMessage("<Server> " + message);
			return true;
		}
		return false;
	}

}
