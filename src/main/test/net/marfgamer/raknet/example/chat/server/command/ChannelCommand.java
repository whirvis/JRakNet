package net.marfgamer.raknet.example.chat.server.command;

import net.marfgamer.raknet.example.chat.server.ChatServer;
import net.marfgamer.raknet.util.RakNetUtils;

public class ChannelCommand extends Command {

	private final ChatServer server;

	public ChannelCommand(ChatServer server) {
		super("channel", "channel <add:remove> <id> [name]");
		this.server = server;
	}

	@Override
	public boolean handleCommand(String[] args) {
		if (args.length >= 2) {
			int channelId = RakNetUtils.parseIntPassive(args[1]);
			if (channelId < 0) {
				return false;
			}

			if (args[0].equalsIgnoreCase("add")) {
				if (args.length >= 3) {
					String channelName = remainingArguments(2, args);
					server.addChannel(channelId, channelName);
					System.out.println("Added channel \"" + channelName + "\" with ID " + channelId);
					return true;
				}
				return false;
			} else if (args[0].equalsIgnoreCase("remove")) {
				String channelName = server.getChannel(channelId).getName();
				server.removeChannel(channelId);
				System.out.println("Removed channel \"" + channelName + "\"");
				return true;
			}
		}
		return false;
	}

}
