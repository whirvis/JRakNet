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
 * Copyright (c) 2016 MarfGamer
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
package net.marfgamer.raknet.example.chat.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.UtilityTest;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.protocol.LoginFailure;
import net.marfgamer.raknet.example.chat.protocol.UpdateUsername;
import net.marfgamer.raknet.example.chat.server.command.BroadcastCommand;
import net.marfgamer.raknet.example.chat.server.command.ChannelCommand;
import net.marfgamer.raknet.example.chat.server.command.CommandHandler;
import net.marfgamer.raknet.example.chat.server.command.KickCommand;
import net.marfgamer.raknet.example.chat.server.command.StopCommand;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.server.RakNetServerListener;
import net.marfgamer.raknet.session.RakNetClientSession;

public class ChatServer implements RakNetServerListener {

	private final String name;
	private final String motd;
	private final RakNetServer server;
	private final ServerChannel[] serverChannels;
	private final HashMap<InetSocketAddress, ConnectedClient> connected;

	public ChatServer(String name, String motd, int port, int maxConnections) {
		this.name = name;
		this.motd = motd;
		this.server = new RakNetServer(port, maxConnections);
		this.serverChannels = new ServerChannel[RakNet.MAX_CHANNELS];
		serverChannels[0] = new ServerChannel(0, "Main hall");
		this.connected = new HashMap<InetSocketAddress, ConnectedClient>();
	}

	public void start() {
		server.setListener(this);
		server.startThreaded();
	}

	public void stop() {
		server.stop();
	}

	private void denyLogin(RakNetClientSession session, String reason) {
		LoginFailure loginFailure = new LoginFailure();
		loginFailure.reason = reason;
		session.sendMessage(Reliability.UNRELIABLE, loginFailure);
	}

	private boolean hasUsername(String username) {
		for (ConnectedClient client : connected.values()) {
			if (client.getUsername().equalsIgnoreCase(username)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private ConnectedClient[] getClients() {
		ArrayList<ConnectedClient> clients = new ArrayList<ConnectedClient>();
		for (ConnectedClient client : connected.values()) {
			clients.add(client);
		}
		return clients.toArray(new ConnectedClient[clients.size()]);
	}

	public void addChannel(int channel, String name) {
		serverChannels[channel] = new ServerChannel(channel, name);
		for (ConnectedClient client : connected.values()) {
			client.addChannel(channel, name);
		}
	}

	public void renameChannel(int channel, String name) {
		serverChannels[channel].setName(name);
		for (ConnectedClient client : connected.values()) {
			client.renameChannel(channel, name);
		}
	}

	public void removeChannel(int channel) {
		serverChannels[channel] = null;
		for (ConnectedClient client : connected.values()) {
			client.removeChannel(channel);
		}
	}

	public boolean hasChannel(int channel) {
		return (serverChannels[channel] != null);
	}

	public String getChannelName(int channel) {
		return serverChannels[channel].getName();
	}

	private ServerChannel[] getChannels() {
		ArrayList<ServerChannel> channels = new ArrayList<ServerChannel>();
		for (ServerChannel channel : this.serverChannels) {
			if (channel != null) {
				channels.add(channel);
			}
		}
		return channels.toArray(new ServerChannel[channels.size()]);
	}

	private void broadcastMessage(String message, int channel, boolean print) {
		for (ConnectedClient client : connected.values()) {
			client.sendChatMessage(message, channel);
		}
		if (print == true) {
			System.out.println(message + " [" + serverChannels[channel].getName() + "]");
		}
	}

	public void broadcastMessage(String message, int channel) {
		this.broadcastMessage(message, channel, true);
	}

	public void broadcastMessage(String message) {
		for (ServerChannel channel : getChannels()) {
			this.broadcastMessage(message + " [Global]", channel.getChannel(), false);
		}
		System.out.println(message + " [Global]");
	}

	public ConnectedClient getClient(String username) {
		for (ConnectedClient client : connected.values()) {
			if (client.getUsername().equals(username)) {
				return client;
			}
		}
		return null;
	}

	public boolean hasClient(String username) {
		return (getClient(username) != null);
	}

	public void kickClient(String username, String reason) {
		if (hasClient(username)) {
			ConnectedClient toKick = getClient(username);
			toKick.kick(reason);
			connected.remove(toKick.getSession().getAddress());
		}
	}

	@Override
	public void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
		InetSocketAddress sender = session.getAddress();
		short packetId = packet.getId();

		if (packetId == ChatMessageIdentifier.ID_LOGIN_REQUEST) {
			if (!connected.containsKey(sender)) {
				// Should we add the user?
				String username = packet.readString();
				if (username.length() <= 0) {
					this.denyLogin(session, "Username is too short!");
					return;
				}
				if (hasUsername(username)) {
					this.denyLogin(session, "Client with that name is already on this server!");
					return;
				}
				ConnectedClient client = new ConnectedClient(session, UUID.randomUUID(), username);
				connected.put(sender, client);

				// Accept request
				client.acceptLogin(this.name, this.motd, this.getChannels());

				// Welcome our new user!
				this.broadcastMessage(username + " has connected");
			}
		} else if (packetId == ChatMessageIdentifier.ID_CHAT_MESSAGE) {
			if (connected.containsKey(sender)) {
				ConnectedClient client = connected.get(sender);
				String message = packet.readString();
				if (message.length() > 0) {
					broadcastMessage("<" + client.getUsername() + "> " + message, channel);
				}
			}
		} else if (packetId == ChatMessageIdentifier.ID_UPDATE_USERNAME_REQUEST) {
			if (connected.containsKey(sender)) {
				ConnectedClient client = connected.get(sender);
				UpdateUsername request = new UpdateUsername(packet);
				request.decode();

				if (!this.hasUsername(request.newUsername)) {
					this.broadcastMessage(client.getUsername() + " has changed their name to " + request.newUsername);
					client.acceptUsernameUpdate(request.newUsername);
				} else {
					client.denyUsernameUpdate();
				}
			}
		}
	}

	@Override
	public void onClientDisconnect(RakNetClientSession session, String reason) {
		if (connected.containsKey(session.getAddress())) {
			broadcastMessage(connected.get(session.getAddress()).getUsername() + " has disconnected");
			connected.remove(session.getAddress());
		}
	}

	public static void main(String[] args) {
		// Create and start server
		ChatServer server = new ChatServer("JRakNet Server Example", "This is a test server made for JRakNet",
				UtilityTest.MARFGAMER_DEVELOPMENT_PORT, 10);
		server.start();
		System.out.println("Started server!");

		// Register commands
		CommandHandler commandHandler = new CommandHandler();
		commandHandler.registerCommand(new StopCommand(server));
		commandHandler.registerCommand(new KickCommand(server));
		commandHandler.registerCommand(new ChannelCommand(server));
		commandHandler.registerCommand(new BroadcastCommand(server));

		// Listen for commands
		@SuppressWarnings("resource")
		Scanner commandScanner = new Scanner(System.in);
		while (true) {
			if (commandScanner.hasNextLine()) {
				String input = commandScanner.nextLine();
				commandHandler.handleInput(input);
			}
		}
	}

}