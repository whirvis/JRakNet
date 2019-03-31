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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.chat.TextChannel;
import com.whirvis.jraknet.chat.server.ChatServer;

/**
 * The "channel" command.
 * <p>
 * This command allows the {@link ChatServer} to add, remove, and rename
 * channels.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class ChannelCommand extends Command {

	private static final Logger LOG = LogManager.getLogger(ChannelCommand.class);

	/**
	 * Constructs a <code>ChannelCommand</code>.
	 * 
	 * @param server
	 *            the server that the command belongs to.
	 */
	public ChannelCommand(ChatServer server) {
		super(server, "channel", "<add:remove:list> [id] <name>", "Used to modify channel data");
	}

	@Override
	public boolean handleCommand(String[] args) {
		if (args.length >= 2) {
			int channelId = RakNet.parseIntPassive(args[1]);
			TextChannel channel = null;
			if (channelId >= 0) {
				channel = this.getServer().getChannel(channelId);
			}
			if (args[0].equalsIgnoreCase("add")) {
				// Automatically determine channel ID if necessary
				boolean specifiedChannel = channelId >= 0;
				if (specifiedChannel == false) {
					channelId = 0;
					while (this.getServer().hasChannel(channelId)) {
						if (channelId++ >= RakNet.MAX_CHANNELS) {
							LOG.error("No more available channels, either remove some or manually assign an ID");
							return true;
						}
					}
				} else if (args.length < 3) {
					LOG.error("No name provided for channel with ID " + channelId);
					return true;
				}

				// Add channel
				String channelName = this.remainingArguments(specifiedChannel ? 2 : 1, args);
				this.getServer().addChannel(channelId, channelName);
				LOG.info("Added channel \"" + channelName + "\" with ID " + channelId);
				return true;
			} else if (args[0].equalsIgnoreCase("rename") && args.length >= 3) {
				if (channel == null) {
					LOG.error("Channel with ID " + channelId + " has not yet been created");
					return true;
				}
				String oldName = channel.getName();
				this.getServer().renameChannel(channelId, this.remainingArguments(2, args));
				LOG.info("Renamed channel with ID " + channelId + " from \"" + oldName + "\" to \"" + channel.getName()
						+ "\"");
				return true;
			} else if (args[0].equalsIgnoreCase("remove")) {
				if (channel == null) {
					LOG.error("Channel was ID " + channelId + " does not exist");
					return true;
				}
				this.getServer().removeChannel(channelId);
				LOG.info("Removed channel \"" + channel.getName() + "\"");
				return true;
			}
		} else if (args.length >= 1) {
			if (args[0].equalsIgnoreCase("list")) {
				// Get the last used channel for formatting
				int lastChannel = RakNet.MAX_CHANNELS - 1;
				while (!this.getServer().hasChannel(lastChannel)) {
					lastChannel--;
				}

				// Print registered channels
				StringBuilder channelListBuilder = new StringBuilder("Registered channels:\n");
				for (int i = 0; i < RakNet.MAX_CHANNELS; i++) {
					if (this.getServer().hasChannel(i)) {
						channelListBuilder.append("\tChannel " + i + ": " + (lastChannel > 10 && i < 10 ? " " : "")
								+ this.getServer().getChannel(i).getName() + (i != lastChannel ? "\n" : ""));
					}
				}
				LOG.info(channelListBuilder);
				return true;
			}
		}
		return false;
	}

}
