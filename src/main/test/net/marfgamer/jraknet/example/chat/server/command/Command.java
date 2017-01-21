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
 * Copyright (c) 2016, 2017 MarfGamer
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

/**
 * Used to easily implements instructions given from the console to be handled
 * by the server
 *
 * @author MarfGamer
 */
public abstract class Command {

	private final boolean overridable;
	private final String label;
	private final String usage;

	protected Command(boolean overridable, String label, String usage) {
		this.overridable = overridable;
		this.label = label;
		this.usage = usage;
	}

	public Command(String name, String usage) {
		this(true, name, usage);
	}

	public Command(String name) {
		this(name, "");
	}

	/**
	 * Returns whether or not this command be overridden by another command
	 * 
	 * @return Whether or not this command by overridden by another command
	 */
	protected boolean isOverridable() {
		return this.overridable;
	}

	/**
	 * Returns the label of the command
	 * 
	 * @return The label of the command
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * Returns the usage of the command
	 * 
	 * @return The usage of the command
	 */
	public String getUsage() {
		return (this.label + " " + this.usage);
	}

	/**
	 * Converts the remaining arguments to a single String from a single index
	 * 
	 * @param startIndex
	 *            - The index to start from
	 * @param stringArray
	 *            - The array to convert to a single String
	 * @return The converted String
	 */
	protected String remainingArguments(int startIndex, String[] stringArray) {
		StringBuilder builder = new StringBuilder();
		for (int i = startIndex; i < stringArray.length; i++) {
			builder.append(stringArray[i] + (i + 1 < stringArray.length ? " " : ""));
		}
		return builder.toString();
	}

	/**
	 * Handles the command with the specified arguments
	 * 
	 * @param args
	 *            - The command arguments
	 * @return Whether or not the command was handled successfully
	 */
	public abstract boolean handleCommand(String[] args);

}
