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

import java.util.HashMap;

import com.whirvis.jraknet.chat.server.ChatServer;

/**
 * Used to easily implements instructions given from the console to be handled
 * by the {@link ChatServer}.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public abstract class Command {

	private static final HashMap<String, Command> COMMANDS = new HashMap<String, Command>();

	/**
	 * Returns the currently registered commands.
	 * 
	 * @return the currently registered commands.
	 */
	public static final Command[] getRegisteredCommands() {
		return COMMANDS.values().toArray(new Command[COMMANDS.size()]);
	}

	private final ChatServer server;
	private final boolean overridable;
	private final String label;
	private final String usage;
	private final String description;

	/**
	 * Creates a command.
	 * 
	 * @param server      the server that the command belongs to.
	 * @param overridable <code>true</code> if the command can be overriden,
	 *                    <code>false</code> otherwise.
	 * @param label       the command label.
	 * @param usage       the command usage, a <code>null</code> value will have the
	 *                    <code>label</code> be used as the usage as well.
	 * @param description the command description.
	 * @throws NullPointerException     if the <code>server</code>,
	 *                                  <code>label</code>, <code>description</code>
	 *                                  are <code>null</code>.
	 * @throws IllegalArgumentException if an unoverridable command with the same
	 *                                  <code>label</code> already exists.
	 */
	protected Command(ChatServer server, boolean overridable, String label, String usage, String description)
			throws NullPointerException, IllegalArgumentException {
		if (server == null) {
			throw new NullPointerException("Chat server cannot be null");
		} else if (label == null) {
			throw new NullPointerException("Label cannot be null");
		} else if (description == null) {
			throw new NullPointerException("Description cannot be null");
		}
		this.server = server;
		this.overridable = overridable;
		this.label = label;
		this.usage = usage == null ? label : usage;
		this.description = description;
		if (COMMANDS.containsKey(label)) {
			if (!COMMANDS.get(label).isOverridable()) {
				throw new IllegalArgumentException("Command with label \"" + label + "\" cannot be overriden");
			}
		}
		COMMANDS.put(label, this);
	}

	/**
	 * Creates a command.
	 * 
	 * @param server      the server that the command belongs to.
	 * @param label       the command label.
	 * @param usage       the command usage, a <code>null</code> value will have the
	 *                    <code>label</code> be used as the usage as well.
	 * @param description the command description.
	 * @throws NullPointerException     if the <code>server</code>,
	 *                                  <code>label</code>, <code>description</code>
	 *                                  are <code>null</code>.
	 * @throws IllegalArgumentException if an unoverridable command with the same
	 *                                  <code>label</code> already exists.
	 */
	protected Command(ChatServer server, String label, String usage, String description)
			throws NullPointerException, IllegalArgumentException {
		this(server, true, label, usage, description);
	}

	/**
	 * Creates a command.
	 * 
	 * @param server      the server that the command belongs to.
	 * @param label       the command label.
	 * @param description the command description.
	 * @throws NullPointerException     if the <code>server</code>,
	 *                                  <code>label</code>, <code>description</code>
	 *                                  are <code>null</code>.
	 * @throws IllegalArgumentException if an unoverridable command with the same
	 *                                  <code>label</code> already exists.
	 */
	protected Command(ChatServer server, String label, String description)
			throws NullPointerException, IllegalArgumentException {
		this(server, label, label, description);
	}

	/**
	 * Returns the server that the command belongs to.
	 * 
	 * @return the server that the command belongs to.
	 */
	protected final ChatServer getServer() {
		return this.server;
	}

	/**
	 * Returns whether or not this command can be overriden by another command.
	 * 
	 * @return <code>true</code> if this command can be overridden by another
	 *         command, <code>false</code> otherwise.
	 */
	protected final boolean isOverridable() {
		return this.overridable;
	}

	/**
	 * Returns the label of the command.
	 * 
	 * @return the label of the command.
	 */
	public final String getLabel() {
		return this.label;
	}

	/**
	 * Returns the usage of the command.
	 * 
	 * @return the usage of the command.
	 */
	public final String getUsage() {
		return (this.label + " " + this.usage);
	}

	/**
	 * Returns the description of the command.
	 * 
	 * @return the description of the command.
	 */
	public final String getDescription() {
		return this.description;
	}

	/**
	 * Converts the remaining arguments to a single string from a single index.
	 * 
	 * @param startIndex  the index to start from.
	 * @param stringArray the array to convert to a single string.
	 * @return the converted string.
	 */
	protected final String remainingArguments(int startIndex, String[] stringArray) {
		StringBuilder builder = new StringBuilder();
		for (int i = startIndex; i < stringArray.length; i++) {
			builder.append(stringArray[i] + (i + 1 < stringArray.length ? " " : ""));
		}
		return builder.toString();
	}

	/**
	 * Handles the command with the specified arguments.
	 * 
	 * @param args the arguments.
	 * @return <code>true</code> if the command was handled successfully,
	 *         <code>false</code> otherwise.
	 */
	public abstract boolean handleCommand(String[] args);

}
