package net.marfgamer.raknet.example.chat.server.command;

import java.util.Arrays;
import java.util.HashMap;

public class CommandHandler {

	private HashMap<String, Command> commands;

	public CommandHandler() {
		this.commands = new HashMap<String, Command>();
	}

	public void registerCommand(Command command) {
		if (commands.containsKey(command.getLabel())) {
			if (commands.get(command.getLabel()).isOverridable() == false) {
				throw new RuntimeException("The label \"" + command.getLabel() + "\" is not overridable!");
			}
		}
		commands.put(command.getLabel().toLowerCase(), command);
	}

	public void unregisterCommand(String label) {
		commands.remove(label);
	}

	public void handleInput(String input) {
		// Get command
		String[] arguments = input.split(" ");
		String label = arguments[0].toLowerCase();
		arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

		// Handle command and print usage if needed
		if (commands.containsKey(label)) {
			Command command = commands.get(label);
			if (command.handleCommand(arguments) == false) {
				System.err.println(command.getUsage());
			}
		} else {
			System.err.println("Unknown command!");
		}
	}

}
