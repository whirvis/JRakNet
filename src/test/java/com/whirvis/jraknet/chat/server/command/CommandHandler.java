/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
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
package com.whirvis.jraknet.chat.server.command;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.chat.server.ChatServer;

/**
 * Used by the {@link ChatServer} to register and handle commands.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public class CommandHandler {

	private static final Logger LOG = LogManager.getLogger(CommandHandler.class);

	private final ChatServer server;
	private final HashMap<String, Command> commands;

	/**
	 * Creates a command handler.
	 * 
	 * @param server
	 *            the server that the command handler belongs to.
	 * @throws NullPointerException
	 *             if the <code>server</code> is <code>null</code>.
	 */
	public CommandHandler(ChatServer server) {
		if (server == null) {
			throw new NullPointerException("Chat server cannot be null");
		}
		this.server = server;
		this.commands = new HashMap<String, Command>();
	}

	/**
	 * Registers the specified command.
	 * 
	 * @param command
	 *            the command to register.
	 * @throws NullPointerException
	 *             if the <code>command</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if another command with the same label as the specified
	 *             <code>command</code> already exists and cannot be overriden.
	 */
	public void registerCommand(Command command) throws NullPointerException, IllegalArgumentException {
		if (command == null) {
			throw new NullPointerException("Command cannot be null");
		}
		if (commands.containsKey(command.getLabel())) {
			if (commands.get(command.getLabel()).isOverridable() == false) {
				throw new IllegalArgumentException("The label \"" + command.getLabel() + "\" is not overridable!");
			}
		}
		commands.put(command.getLabel().toLowerCase(), command);
	}

	/**
	 * Registers a command by its specified class.
	 * <p>
	 * For this method to function, the specified class must have a constructor
	 * with the first and only argument being a {@link ChatServer}.
	 * 
	 * @param commandClazz
	 *            the command class.
	 * @throws NullPointerException
	 *             if the <code>commandClazz</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>commandClazz</code> does not have the needed
	 *             constructor for proper instantiation, it is inaccessable, or
	 *             if another command with the same label as the newly
	 *             instantiated command with the specified
	 *             <code>commandClazz</code> already exists and cannot be
	 *             overriden.
	 * 
	 */
	public void registerCommand(Class<? extends Command> commandClazz) throws NullPointerException, IllegalArgumentException {
		try {
			if (commandClazz == null) {
				throw new NullPointerException("Command class cannot be null");
			}
			Command command = (Command) commandClazz.getDeclaredConstructor(ChatServer.class).newInstance(server);
			this.registerCommand(command);
		} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
			throw new IllegalArgumentException("Command lacks invalid constructor for proper instantiation");
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
		if (label != null) {
			commands.remove(label);
		}
	}

	/**
	 * Handles the specified input as a command.
	 * 
	 * @param input
	 *            the input to process and handle.
	 * @throws NullPointerException
	 *             if the <code>input</code> is <code>null</code>.
	 */
	public void handleInput(String input) throws NullPointerException {
		if (input == null) {
			throw new NullPointerException("Input cannot be null");
		}

		// Convert input
		String[] arguments = input.split(" ");
		String label = arguments[0].toLowerCase();
		arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

		// Handle command and print usage if needed
		if (commands.containsKey(label)) {
			Command command = commands.get(label);
			try {
				if (command.handleCommand(arguments) == false) {
					LOG.error("Usage: " + command.getUsage());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			LOG.error("Unknown command \"" + label + "\"");
		}
	}

}
