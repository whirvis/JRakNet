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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.marfgamer.jraknet.Packet;
import net.marfgamer.jraknet.RakNet;
import net.marfgamer.jraknet.RakNetPacket;
import net.marfgamer.jraknet.identifier.Identifier;
import net.marfgamer.jraknet.protocol.MessageIdentifier;
import net.marfgamer.jraknet.protocol.login.IncompatibleProtocol;
import net.marfgamer.jraknet.protocol.login.OpenConnectionRequestOne;
import net.marfgamer.jraknet.protocol.login.OpenConnectionResponseOne;
import net.marfgamer.jraknet.protocol.status.UnconnectedPing;
import net.marfgamer.jraknet.protocol.status.UnconnectedPong;

/**
 * This class is used to accomplish tasks related to the RakNet protocol without
 * needing a dedicated client or server.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class RakNetUtils {

	// Utility data
	private static final long UTILS_TIMESTAMP = System.currentTimeMillis();
	private static final long UTILS_PING_ID = new Random().nextLong();
	private static final int SERVER_PING_RETRIES = 5;
	private static final int IDENTIFIER_RETRIES = 3;

	/**
	 * Sends a raw message to the specified address for the specified amount of
	 * times in the specified interval until the packet is received or there is
	 * a timeout.
	 * 
	 * @param address
	 *            the address to send the packet to.
	 * @param packet
	 *            the packet to send.
	 * @param timeout
	 *            the interval of which the packet is sent.
	 * @param retries
	 *            how many times the packet will be sent.
	 * @return the received packet if it was received.
	 */
	private static RakNetPacket createBootstrapAndSend(InetSocketAddress address, Packet packet, long timeout,
			int retries) {
		RakNetPacket packetReceived = null;

		// Create bootstrap and bind
		EventLoopGroup group = new NioEventLoopGroup();

		try {
			Bootstrap bootstrap = new Bootstrap();
			BootstrapHandler handler = new BootstrapHandler();
			bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, RakNet.MINIMUM_TRANSFER_UNIT)
					.option(ChannelOption.SO_SNDBUF, RakNet.MINIMUM_TRANSFER_UNIT).handler(handler);

			// Create channel, send packet, and close it
			Channel channel = bootstrap.bind(0).sync().channel();
			channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));

			// Wait for packet to come in, return null on timeout
			while (retries > 0) {
				long sendTime = System.currentTimeMillis();
				while (System.currentTimeMillis() - sendTime < timeout) {
					if (handler.packet != null) {
						packetReceived = handler.packet;
						break; // We found the packet
					}
				}
				if (packetReceived != null) {
					break; // the master loop is no longer needed
				}
				retries--;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Shutdown bootstrap
		group.shutdownGracefully();
		return packetReceived;
	}

	/**
	 * @param address
	 *            the address of the server.
	 * @return <code>true</code> if the server is online.
	 */
	public static boolean isServerOnline(InetSocketAddress address) {
		// Create connection packet
		OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
		connectionRequestOne.maximumTransferUnit = RakNet.MINIMUM_TRANSFER_UNIT;
		connectionRequestOne.protocolVersion = RakNet.CLIENT_NETWORK_PROTOCOL;
		connectionRequestOne.encode();

		// Wait for response to come in
		RakNetPacket packet = createBootstrapAndSend(address, connectionRequestOne, 1000, SERVER_PING_RETRIES);
		if (packet != null) {
			if (packet.getId() == MessageIdentifier.ID_OPEN_CONNECTION_REPLY_1) {
				OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne(packet);
				connectionResponseOne.decode();
				if (connectionResponseOne.magic == true) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is online.
	 */
	public static boolean isServerOnline(InetAddress address, int port) {
		return isServerOnline(new InetSocketAddress(address, port));
	}

	/**
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is online.
	 * @throws UnknownHostException
	 *             if the specified address is an unknown host.
	 */
	public static boolean isServerOnline(String address, int port) throws UnknownHostException {
		return isServerOnline(InetAddress.getByName(address), port);
	}

	/**
	 * @param address
	 *            the address of the server.
	 * @return <code>true</code> if the server is compatible to the current
	 *         client protocol.
	 */
	public static boolean isServerCompatible(InetSocketAddress address) {
		// Create connection packet
		OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
		connectionRequestOne.maximumTransferUnit = RakNet.MINIMUM_TRANSFER_UNIT;
		connectionRequestOne.protocolVersion = RakNet.CLIENT_NETWORK_PROTOCOL;
		connectionRequestOne.encode();

		// Wait for response to come in
		RakNetPacket packet = createBootstrapAndSend(address, connectionRequestOne, 1000, SERVER_PING_RETRIES);
		if (packet != null) {
			if (packet.getId() == MessageIdentifier.ID_OPEN_CONNECTION_REPLY_1) {
				OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne(packet);
				connectionResponseOne.decode();
				if (connectionResponseOne.magic == true) {
					return true;
				}
			} else if (packet.getId() == MessageIdentifier.ID_INCOMPATIBLE_PROTOCOL_VERSION) {
				IncompatibleProtocol incompatibleProtocol = new IncompatibleProtocol(packet);
				incompatibleProtocol.decode();

				return false;
			}
		}
		return false;
	}

	/**
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is compatible to the current
	 *         client protocol.
	 */
	public static boolean isServerCompatible(InetAddress address, int port) {
		return isServerCompatible(new InetSocketAddress(address, port));
	}

	/**
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is compatible to the current
	 *         client protocol.
	 * @throws UnknownHostException
	 *             if the specified address is an unknown host.
	 */
	public static boolean isServerCompatible(String address, int port) throws UnknownHostException {
		return isServerCompatible(InetAddress.getByName(address), port);
	}

	/**
	 * @param address
	 *            the address of the server.
	 * @return the specified server's <code>Identifier</code>.
	 */
	public static Identifier getServerIdentifier(InetSocketAddress address) {
		// Create ping packet
		UnconnectedPing ping = new UnconnectedPing();
		ping.timestamp = (System.currentTimeMillis() - UTILS_TIMESTAMP);
		ping.pingId = UTILS_PING_ID;
		ping.encode();

		// Wait for response to come in
		RakNetPacket packet = createBootstrapAndSend(address, ping, 1000, IDENTIFIER_RETRIES);
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

	/**
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return the specified server's <code>Identifier</code>.
	 */
	public static Identifier getServerIdentifier(InetAddress address, int port) {
		return getServerIdentifier(new InetSocketAddress(address, port));
	}

	/**
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return the specified server's <code>Identifier</code>.
	 * @throws UnknownHostException
	 *             if the specified address is an unknown host.
	 */
	public static Identifier getServerIdentifier(String address, int port) throws UnknownHostException {
		return getServerIdentifier(InetAddress.getByName(address), port);
	}

	/**
	 * @return the maximum transfer unit of the network interface binded to the
	 *         localhost.
	 */
	public static int getMaximumTransferUnit() {
		try {
			return NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU();
		} catch (Throwable throwable) {
			try {
				/*
				 * We failed to get the NetworkInterface, we're gonna have to
				 * cycle through them manually and choose the lowest one to make
				 * sure we never exceed any hardware limitations
				 */
				boolean foundDevice = false;
				int lowestMaximumTransferUnit = Integer.MAX_VALUE;
				for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
						.getNetworkInterfaces(); networkInterfaces.hasMoreElements();) {
					NetworkInterface networkInterface = networkInterfaces.nextElement();
					int maximumTransferUnit = networkInterface.getMTU();
					if (maximumTransferUnit < lowestMaximumTransferUnit
							&& maximumTransferUnit >= RakNet.MINIMUM_TRANSFER_UNIT) {
						lowestMaximumTransferUnit = maximumTransferUnit;
						foundDevice = true;
					}
				}

				// This is a serious error and will cause startup to fail
				if (foundDevice == false) {
					throw new IOException("Failed to locate a network interface with an MTU higher than the minimum ("
							+ RakNet.MINIMUM_TRANSFER_UNIT + ")");
				}
				return lowestMaximumTransferUnit;
			} catch (Throwable throwable2) {
				throwable2.printStackTrace();
				return -1;
			}
		}
	}

	/**
	 * Parses a single String as an address and port and converts it to an
	 * <code>InetSocketAddress</code>.
	 * 
	 * @param address
	 *            the address to convert.
	 * @param defaultPort
	 *            the default port to use if one is not specified.
	 * @return the parsed <code>InetSocketAddress</code>.
	 * @throws UnknownHostException
	 *             if the address is in an invalid format or if the host cannot
	 *             be found.
	 */
	public static InetSocketAddress parseAddress(String address, int defaultPort) throws UnknownHostException {
		String[] addressSplit = address.split(":");
		if (addressSplit.length == 1 || addressSplit.length == 2) {
			InetAddress inetAddress = InetAddress.getByName(addressSplit[0]);
			int port = (addressSplit.length == 2 ? parseIntPassive(addressSplit[1]) : defaultPort);
			if (port >= 0 && port <= 65535) {
				return new InetSocketAddress(inetAddress, port);
			} else {
				throw new UnknownHostException("Port number must be between 0-65535");
			}
		} else {
			throw new UnknownHostException("Format must follow address:port");
		}
	}

	/**
	 * Parses a single String as an address and port and converts it to an
	 * <code>InetSocketAddress</code>.
	 * 
	 * @param address
	 *            the address to convert.
	 * @return the parsed <code>InetSocketAddress</code>.
	 * @throws UnknownHostException
	 *             if the address is in an invalid format or if the host cannot
	 *             be found.
	 */
	public static InetSocketAddress parseAddress(String address) throws UnknownHostException {
		return parseAddress(address, -1);
	}

	/**
	 * Parses a single String as an address and port and converts it to an
	 * <code>InetSocketAddress</code>.
	 * 
	 * @param address
	 *            the address to convert.
	 * @param defaultPort
	 *            the default port to use if one is not specified.
	 * @return the parsed <code>InetSocketAddress</code>.
	 */
	public static InetSocketAddress parseAddressPassive(String address, int defaultPort) {
		try {
			return parseAddress(address, defaultPort);
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * Parses a single String as an address and port and converts it to an
	 * <code>InetSocketAddress</code>.
	 * 
	 * @param address
	 *            the address to convert.
	 * @return the parsed <code>InetSocketAddress</code>.
	 */
	public static InetSocketAddress parseAddressPassive(String address) {
		return parseAddressPassive(address, -1);
	}

	/**
	 * Parses a String as a long and returns -1 in the case of a
	 * <code>NumberFormatException</code>.
	 * 
	 * @param longStr
	 *            the String to parse.
	 * @return the String as a long.
	 */
	public static long parseLongPassive(String longStr) {
		try {
			return Long.parseLong(longStr);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Parses a String as an int and returns -1 in the case of a
	 * <code>NumberFormatException</code>.
	 * 
	 * @param intStr
	 *            the String to parse.
	 * @return the String as an int.
	 */
	public static int parseIntPassive(String intStr) {
		return (int) RakNetUtils.parseLongPassive(intStr);
	}

	/**
	 * Parses a String as a short and returns -1 in the case of a
	 * <code>NumberFormatException</code>.
	 * 
	 * @param shortStr
	 *            the String to parse.
	 * @return the String as a short.
	 */
	public static short parseShortPassive(String shortStr) {
		return (short) RakNetUtils.parseLongPassive(shortStr);
	}

	/**
	 * Parses a String as a byte and returns -1 in the case of a
	 * <code>NumberFormatException</code>.
	 * 
	 * @param byteStr
	 *            the String to parse.
	 * @return the String as a byte.
	 */
	public static byte parseBytePassive(String byteStr) {
		return (byte) RakNetUtils.parseLongPassive(byteStr);
	}

	/**
	 * Converts the given ID to a hex string.
	 * 
	 * @param id
	 *            the ID to convert to a hex string.
	 * @return the generated hex string.
	 */
	public static String toHexStringId(int id) {
		return ("0x" + Integer.toHexString(id).toUpperCase());
	}

	/**
	 * Converts the ID of the given <code>RakNetPacket</code> to a hex string.
	 * 
	 * @param packet
	 *            the packet to get the ID from.
	 * @return the generated hex string.
	 */
	public static String toHexStringId(RakNetPacket packet) {
		return toHexStringId(packet.getId());
	}

	/**
	 * Causes a sleep on the main thread using a simple while loop.
	 * 
	 * @param time
	 *            How long the thread will sleep in milliseconds.
	 */
	public static void threadLock(long time) {
		long sleepStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - sleepStart < time)
			;
	}

	/**
	 * Used by <code>createBootstrapAndSend()</code> to wait for the packet and
	 * return it.
	 *
	 * @author Whirvis "MarfGamer" Ardenaur
	 */
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
