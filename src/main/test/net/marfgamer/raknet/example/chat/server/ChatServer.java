package net.marfgamer.raknet.example.chat.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.UtilityTest;
import net.marfgamer.raknet.example.chat.ChatMessageIdentifier;
import net.marfgamer.raknet.example.chat.ServerChannel;
import net.marfgamer.raknet.example.chat.protocol.AddUserPacket;
import net.marfgamer.raknet.example.chat.protocol.LoginAccepted;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.server.RakNetServerListener;
import net.marfgamer.raknet.session.RakNetClientSession;

public class ChatServer implements RakNetServerListener {

	private final RakNetServer server;
	private final ServerChannel[] serverChannels;
	private final HashMap<InetSocketAddress, ConnectedClient> connected;

	public ChatServer(int port, int maxConnections) {
		this.server = new RakNetServer(port, maxConnections);
		this.serverChannels = new ServerChannel[RakNet.MAX_CHANNELS];
		serverChannels[0] = new ServerChannel(0, "Main hall");
		this.connected = new HashMap<InetSocketAddress, ConnectedClient>();
	}

	public void start() {
		server.setListener(this);
		server.start();
	}

	private void failLogin(RakNetClientSession session, String reason) {
		RakNetPacket loginFailPacket = new RakNetPacket(ChatMessageIdentifier.ID_KICK);
		loginFailPacket.writeString(reason);
		session.sendMessage(Reliability.UNRELIABLE, loginFailPacket);
		System.out.println("Denied login from address " + session.getAddress() + " (" + reason + ")");
	}

	private boolean hasUsername(String username) {
		for (ConnectedClient client : connected.values()) {
			if (client.getUsername().equalsIgnoreCase(username)) {
				return true;
			}
		}
		return false;
	}

	private ConnectedClient[] getClients() {
		ArrayList<ConnectedClient> clients = new ArrayList<ConnectedClient>();
		for (ConnectedClient client : connected.values()) {
			clients.add(client);
		}
		return clients.toArray(new ConnectedClient[clients.size()]);
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

	private void broadcastMessage(String message, int channel) {
		for (ConnectedClient client : connected.values()) {
			client.sendChatMessage(message, channel);
		}
		System.out.println(message + " [" + channel + "]");
	}

	@Override
	public void handlePacket(RakNetClientSession session, RakNetPacket packet, int channel) {
		InetSocketAddress sender = session.getAddress();
		short packetId = packet.getId();

		if (packetId == ChatMessageIdentifier.ID_LOGIN_REQUEST) {
			if (!connected.containsKey(sender)) {
				// Should we add the user?
				String username = packet.readString();
				if (hasUsername(username)) {
					failLogin(session, "Client with that name is already on this server!");
					return;
				}
				ConnectedClient client = new ConnectedClient(session, UUID.randomUUID(), username);
				connected.put(sender, client);

				// Accept request
				LoginAccepted accepted = new LoginAccepted();
				accepted.channels = getChannels();
				accepted.onlineUsers = getClients();
				accepted.userId = client.getUUID();

				// Tells the clients of our new friend!
				for (ConnectedClient connectedClient : connected.values()) {
					AddUserPacket addUser = new AddUserPacket();
					addUser.userId = client.getUUID();
					addUser.username = client.getUsername();
					addUser.encode();
					// TODO
				}

				// Welcome our new user!
				broadcastMessage(username + " has connected", 0);
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

		}
	}

	@Override
	public void onClientDisconnect(RakNetClientSession session, String reason) {
		if (connected.containsKey(session)) {
			broadcastMessage(connected.get(session).getUsername() + " has disconnected", 0);
			connected.remove(session.getAddress());
		}
	}

	public static void main(String[] args) {
		ChatServer server = new ChatServer(UtilityTest.MARFGAMER_DEVELOPMENT_PORT, 10);
		server.start();
	}

}
