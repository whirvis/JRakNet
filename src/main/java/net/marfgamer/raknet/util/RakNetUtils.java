package net.marfgamer.raknet.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.raknet.Packet;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.RakNetPacket;
import net.marfgamer.raknet.identifier.Identifier;
import net.marfgamer.raknet.protocol.MessageIdentifier;
import net.marfgamer.raknet.protocol.status.UnconnectedPing;
import net.marfgamer.raknet.protocol.status.UnconnectedPong;

public class RakNetUtils {

	private static final long UTILS_TIMESTAMP = System.currentTimeMillis();

	public static int getMaximumTransferUnit() {
		try {
			return NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU();
		} catch (IOException e) {
			return -1;
		}
	}

	public static RakNetPacket createBootstrapAndSend(InetSocketAddress address, Packet packet, long timeout) {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			// Create bootstrap and bind
			Bootstrap bootstrap = new Bootstrap();
			BootstrapHandler handler = new BootstrapHandler();
			bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, RakNet.MINIMUM_TRANSFER_UNIT)
					.option(ChannelOption.SO_SNDBUF, RakNet.MINIMUM_TRANSFER_UNIT).handler(handler);

			// Create channel, send packet, and close it
			Channel channel = bootstrap.bind(0).sync().channel();
			channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
			//channel.close();
			
			// Wait for packet to come in, return null on timeout
			long sendTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - sendTime < timeout) {
				if (handler.packet != null) {
					return handler.packet;
				}
			}
			group.shutdownGracefully();
		} catch (Exception e) {
			group.shutdownGracefully();
		}
		return null;
	}

	public static Identifier getIdentifier(InetAddress address, int port) {
		// Create ping packet
		UnconnectedPing ping = new UnconnectedPing();
		ping.timestamp = (System.currentTimeMillis() - UTILS_TIMESTAMP);
		ping.encode();

		// Wait for response to come in
		RakNetPacket packet = createBootstrapAndSend(new InetSocketAddress(address, port), ping, 1000);
		if (packet != null) {
			if (packet.getId() == MessageIdentifier.ID_UNCONNECTED_PONG) {
				UnconnectedPong pong = new UnconnectedPong(packet);
				pong.decode();
				if (pong.magic == true) {
					return pong.identifier;
				}
			}
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		System.out.println(getIdentifier(InetAddress.getByName("192.168.1.21"), 19132));
	}

	public static long parseLongPassive(String longStr) {
		try {
			return Long.parseLong(longStr);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static int parseIntPassive(String intStr) {
		return (int) RakNetUtils.parseLongPassive(intStr);
	}

	public static short parseShortPassive(String shortStr) {
		return (short) RakNetUtils.parseLongPassive(shortStr);
	}

	public static byte parseBytePassive(String byteStr) {
		return (byte) RakNetUtils.parseLongPassive(byteStr);
	}

	public static void passiveSleep(long time) {
		long sleepStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - sleepStart < time)
			;
	}

	private static class BootstrapHandler extends ChannelInboundHandlerAdapter {

		public volatile RakNetPacket packet;

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof DatagramPacket) {
				this.packet = new RakNetPacket(((DatagramPacket) msg).content().retain());
			}
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

	}

}
