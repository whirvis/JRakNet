/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016, 2017 Trent "MarfGamer" Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 */
package net.marfgamer.jraknet.example.chat.server.command;

import java.util.Arrays;
import java.util.HashMap;

import net.marfgamer.jraknet.RakNetLogger;
import net.marfgamer.jraknet.example.chat.server.ChatServer;

/**
 * Used by the server to register and handle commands easily.
 *
 * @author Trent "MarfGamer" Summerlin
 */
public class CommandHandler {

	private HashMap<String, Command> commands;

	public CommandHandler() {
		this.commands = new HashMap<String, Command>();
	}

	/**
	 * Registers a command.
	 * 
	 * @param command
	 *            the command to register.
	 */
	public void registerCommand(Command command) {
		if (commands.containsKey(command.getLabel())) {
			if (commands.get(command.getLabel()).isOverridable() == false) {
				throw new RuntimeException("The label \"" + command.getLabel() + "\" is not overridable!");
			}
		}
		commands.put(command.getLabel().toLowerCase(), command);
	}

	/**
	 * Registers a command.
	 * 
	 * @param commandClazz
	 *            the command class.
	 */
	public void registerCommand(Class<? extends Command> commandClazz) {
		try {
			Command command = (Command) commandClazz.newInstance();
			this.registerCommand(command);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("Command must have a nullary constructor");
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Command constructor must be accessible");
		}
	}

	/**
	 * Unregisters a command.
	 * 
	 * @param label
	 *            the label of the command to unregister.
	 */
	public void unregisterCommand(String label) {
		commands.remove(label);
	}

	/**
	 * Handles raw input as a command.
	 * 
	 * @param input
	 *            the input to process and handle.
	 */
	public void handleInput(String input) {
		// Get command
		String[] arguments = input.split(" ");
		String label = arguments[0].toLowerCase();
		arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

		// Handle command and print usage if needed
		if (commands.containsKey(label)) {
			Command command = commands.get(label);
			if (command.handleCommand(arguments) == false) {
				RakNetLogger.error(ChatServer.LOGGER_NAME, "Usage: " + command.getUsage());
			}
		} else {
			RakNetLogger.error(ChatServer.LOGGER_NAME, "Unknown command!");
		}
	}

}
