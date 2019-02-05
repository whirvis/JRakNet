/*
 *       _   _____            _      _   _          _
 *      | | |  __ \          | |    | \ | |        | |
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.example.chat.server.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.example.chat.server.ChatServer;

/**
 * Allows the server to kick players.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class KickCommand extends Command {

	private static final Logger LOG = LogManager.getLogger(KickCommand.class);

	private final ChatServer server;

	public KickCommand(ChatServer server) {
		super("kick", "<player> [reason]", "Removes the user from the server");
		this.server = server;
	}

	@Override
	public boolean handleCommand(String[] args) {
		if (args.length >= 1) {
			String reason = (args.length >= 2 ? remainingArguments(1, args) : "Kicked from server");
			if (server.hasClient(args[0])) {
				server.kickClient(server.getClient(args[0]), reason);
				LOG.info("Kicked client \"" + args[0] + "\" with reason \"" + reason + "\"");
			} else {
				LOG.info("Client \"" + args[0] + "\" is not online!");
			}
			return true;
		}
		return false;
	}

}
