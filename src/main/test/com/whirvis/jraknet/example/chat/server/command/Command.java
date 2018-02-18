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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.example.chat.server.command;

import java.util.HashMap;

/**
 * Used to easily implements instructions given from the console to be handled
 * by the server.
 *
 * @author Trent "Whirvis" Summerlin
 */
public abstract class Command {

	// Pre-registered commands
	private static final HashMap<String, Command> commands = new HashMap<String, Command>();

	/**
	 * @return the pre-registered commands.
	 */
	public static final Command[] getRegisteredCommands() {
		return commands.values().toArray(new Command[commands.size()]);
	}

	// Command data
	private final boolean overridable;
	private final String label;
	private final String usage;
	private final String description;

	/**
	 * Constructs a <code>Command</code> with whether or not it is overridable along
	 * the specified label and usage.
	 * 
	 * @param overridable
	 *            whether or not the command is overridable.
	 * @param label
	 *            the command label.
	 * @param usage
	 *            the command usage.
	 * @param description
	 *            the command description.
	 */
	protected Command(boolean overridable, String label, String usage, String description) {
		// Set command data
		this.overridable = overridable;
		this.label = label;
		this.usage = usage;
		this.description = description;

		// Add ourself to the command list
		if (commands.containsKey(label)) {
			if (!commands.get(label).isOverridable()) {
				throw new IllegalArgumentException("Command with label \"" + label + "\" cannot be overriden");
			}
		}
		commands.put(label, this);
	}

	/**
	 * Constructs a <code>Command</code> with the specified label and usage.
	 * 
	 * @param label
	 *            the command label.
	 * @param usage
	 *            the command usage.
	 * @param description
	 *            the command description.
	 */
	public Command(String label, String usage, String description) {
		this(true, label, usage, description);
	}

	/**
	 * Constructs a <code>Command</code> with the specified label.
	 * 
	 * @param label
	 *            the command label.
	 * @param description
	 *            the command description.
	 */
	public Command(String label, String description) {
		this(label, label, description);
	}

	/**
	 * @return <code>true</code> if this command by overridden by another command.
	 */
	protected final boolean isOverridable() {
		return this.overridable;
	}

	/**
	 * @return the label of the command.
	 */
	public final String getLabel() {
		return this.label;
	}

	/**
	 * @return the usage of the command.
	 */
	public final String getUsage() {
		return (this.label + " " + this.usage);
	}

	/**
	 * @return the description of the command.
	 */
	public final String getDescription() {
		return this.description;
	}

	/**
	 * Converts the remaining arguments to a single String from a single index.
	 * 
	 * @param startIndex
	 *            the index to start from.
	 * @param stringArray
	 *            the array to convert to a single String.
	 * @return the converted String
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
	 * @param args
	 *            the command arguments.
	 * @return <code>true</code> if the command was handled successfully.
	 */
	public abstract boolean handleCommand(String[] args);

}
