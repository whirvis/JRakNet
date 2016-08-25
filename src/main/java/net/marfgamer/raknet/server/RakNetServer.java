package net.marfgamer.raknet.server;

import static net.marfgamer.raknet.protocol.MessageIdentifier.*;

import java.net.InetSocketAddress;
import java.util.HashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.unconnected.UnconnectedPing;
import net.marfgamer.raknet.protocol.unconnected.UnconnectedPong;
import net.marfgamer.raknet.server.identifier.Identifier;
import net.marfgamer.raknet.server.identifier.MCPEIdentifier;
import net.marfgamer.raknet.session.RakNetSession;

/**
 * 
 *
 * @author MarfGamer
 */
public class RakNetServer implements RakNet {

	private final long guid;

	private final int port;
	private final int maxConnections;
	private final int maxTransferUnit;
	private Identifier identifier;

	private final Bootstrap bootstrap;
	private final EventLoopGroup group;
	private final RakNetServerHandler handler;

	private Channel channel;
	private RakNetServerListener listener;

	private final HashMap<InetSocketAddress, RakNetSession> sessions;

	public RakNetServer(int port, int maxConnections, int maxTransferUnit, Identifier identifier) {
		this.guid = UNIQUE_ID_BITS.getMostSignificantBits();

		this.port = port;
		this.maxConnections = maxConnections;
		this.maxTransferUnit = maxTransferUnit;
		this.identifier = identifier;

		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup();
		this.handler = new RakNetServerHandler(this);

		this.sessions = new HashMap<InetSocketAddress, RakNetSession>();
	}

	public RakNetServer(int port, int maxConnections, int maxTransferUnit) {
		this(port, maxConnections, maxTransferUnit, null);
	}

	public RakNetServer(int port, int maxConnections) {
		this(port, maxConnections, MINIMUM_TRANSFER_UNIT);
	}

	public RakNetServer(int port, int maxConnections, Identifier identifier) {
		this(port, maxConnections);
		this.identifier = identifier;
	}

	public long getGloabilyUniqueId() {
		return this.guid;
	}

	public int getPort() {
		return this.port;
	}

	public int getMaxConnections() {
		return this.maxConnections;
	}

	public int getMaxTransferUnit() {
		return this.maxTransferUnit;
	}

	public Identifier getIdentifier() {
		return this.identifier;
	}

	public RakNetServer setIdentifier(Identifier identifier) {
		this.identifier = identifier;
		return this;
	}

	public RakNetServer setListener(RakNetServerListener listener) {
		this.listener = listener;
		return this;
	}

	public void handleMessage(Packet packet, InetSocketAddress sender) {
		short id = packet.getId();
		if (id == ID_UNCONNECTED_PING || id == ID_UNCONNECTED_PING_OPEN_CONNECTIONS) {
			UnconnectedPing ping = new UnconnectedPing(packet);
			ping.decode();

			if ((id == ID_UNCONNECTED_PING || sessions.size() < this.maxConnections) && identifier != null) {
				ServerPing pingEvent = new ServerPing(sender, identifier);
				listener.handlePing(pingEvent, this);
				if (pingEvent.getIdentifier() != null) {
					UnconnectedPong pong = new UnconnectedPong();
					pong.pingId = ping.pingId;
					pong.pongId = this.guid;
					pong.identifier = pingEvent.getIdentifier();

					pong.encode();
					this.sendRaw(pong, sender);
				}
			}
		}
	}

	private void sendRaw(Packet packet, InetSocketAddress address) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	public RakNetServer start() {
		// Make sure we have an adapter
		if (listener == null) {
			throw new RuntimeException("Handler has not been set!");
		}

		// Create bootstrap and bind the channel
		bootstrap.channel(NioDatagramChannel.class).group(group).handler(handler);
		bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, false);
		this.channel = bootstrap.bind(port).channel();
		return this;
	}

	public static void main(String[] args) {
		MCPEIdentifier identifier = new MCPEIdentifier("A JRakNet Server", 80, "0.15.0", 0, 10);
		RakNetServer s = new RakNetServer(19132, 10, identifier);
		s.setListener(new RakNetServerListener() {
			@Override
			public void handlePing(ServerPing ping, RakNetServer server) {
				ping.setIdentifier(new MCPEIdentifier("Hello!", 80, "0.15.0", 0, 10));
			}
		});
		s.start();
	}

}
