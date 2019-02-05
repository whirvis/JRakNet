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
package com.whirvis.jraknet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.MessageIdentifier;
import com.whirvis.jraknet.protocol.login.IncompatibleProtocol;
import com.whirvis.jraknet.protocol.login.OpenConnectionRequestOne;
import com.whirvis.jraknet.protocol.login.OpenConnectionResponseOne;
import com.whirvis.jraknet.protocol.status.UnconnectedPing;
import com.whirvis.jraknet.protocol.status.UnconnectedPong;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Contains info for RakNet
 *
 * @author Whirvis T. Wheatley
 */
public final class RakNet {

	/**
	 * Used by <code>createBootstrapAndSend()</code> to wait for the packet and
	 * return it.
	 *
	 * @author Whirvis T. Wheatley
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

	private RakNet() {
		// Static class
	}

	/**
	 * Quick note about RakNet protocol versions: If the RakNet protocol version
	 * for Minecraft is ever bumped, please let me know immediately. I would add
	 * a function to change the network protocol values, however I would rather
	 * have myself change it after being notified by someone through an issue on
	 * GitHub so I know the protocol is possibly out of date and that I need to
	 * bump the versions.
	 */

	private static final Logger LOG = LogManager.getLogger(RakNet.class);

	// Network protocol data
	public static final int SERVER_NETWORK_PROTOCOL = 9;
	public static final int CLIENT_NETWORK_PROTOCOL = 9;
	public static final int MINIMUM_MTU_SIZE = 400;
	private static final int PING_RETRIES = 5;
	private static final long PING_TIMESTAMP = System.currentTimeMillis();
	private static final long PING_ID = UUID.randomUUID().getLeastSignificantBits();
	private static final int IDENTIFIER_RETRIES = 3;
	private static int DEVICE_MTU_SIZE = -1;

	// Session data
	public static final int MAX_CHANNELS = 32;
	public static final byte DEFAULT_CHANNEL = 0;
	public static final int MAX_SPLIT_COUNT = 128;
	public static final int MAX_SPLITS_PER_QUEUE = 4;
	public static final long SEND_INTERVAL = 50L;
	public static final long RECOVERY_SEND_INTERVAL = SEND_INTERVAL;
	public static final long PING_SEND_INTERVAL = 2500L;
	public static final long DETECTION_SEND_INTERVAL = PING_SEND_INTERVAL * 2;
	public static final long SESSION_TIMEOUT = DETECTION_SEND_INTERVAL * 5;
	public static final long MAX_PACKETS_PER_SECOND_BLOCK = (1000L * 300);

	// Configurable options
	private static long MAX_PACKETS_PER_SECOND = 500;

	/**
	 * Returns how many packets can be received in the span of a single second
	 * (1000 milliseconds) before a session is blocked.
	 * 
	 * @return how many packets can be received in the span of a single second
	 *         before a session is blocked.
	 */
	public static long getMaxPacketsPerSecond() {
		return MAX_PACKETS_PER_SECOND;
	}

	/**
	 * Sets how many packets can be received in the span of a single second
	 * (1000 milliseconds) before a session is blocked.
	 * 
	 * @param maxPacketsPerSecond
	 *            how many packets can be received in the span of a single
	 *            second before a session is blocked.
	 */
	public static void setMaxPacketsPerSecond(long maxPacketsPerSecond) {
		MAX_PACKETS_PER_SECOND = maxPacketsPerSecond;
	}

	/**
	 * Removes the max packets per second limit so that no matter how many
	 * packets a session sends it will never be blocked. This is unrecommended,
	 * as it can open your server to DOS/DDOS attacks.
	 */
	public static void setMaxPacketsPerSecondUnlimited() {
		MAX_PACKETS_PER_SECOND = Long.MAX_VALUE;
	}

	/**
	 * Used to determine the maximum transfer unit of the device. Normally, the
	 * maximum transfer unit that will be used is the one of the network
	 * interface that is bound to localhost. However, if it is not possible to
	 * fetch the network interface with the localhost address, the lowest valid
	 * maximum transfer unit of the device will be used instead.
	 * 
	 * @return the maximum transfer unit of the device.
	 */
	public static int getMaximumTransferUnit() {
		if (DEVICE_MTU_SIZE < 0) {
			try {
				int maximumTransferUnit = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU();
				if (maximumTransferUnit < 0) {
					throw new RuntimeException("Invalid maximum transfer unit for localhost address");
				}
				DEVICE_MTU_SIZE = maximumTransferUnit;
			} catch (Throwable throwable) {
				try {
					/*
					 * We failed to get the NetworkInterface, we're going to
					 * have to cycle through them manually and choose the lowest
					 * one to make sure we never exceed any hardware limitations
					 */
					boolean foundDevice = false;
					int lowestMaximumTransferUnit = Integer.MAX_VALUE;
					for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
							.getNetworkInterfaces(); networkInterfaces.hasMoreElements();) {
						NetworkInterface networkInterface = networkInterfaces.nextElement();
						int maximumTransferUnit = networkInterface.getMTU();
						if (maximumTransferUnit < lowestMaximumTransferUnit
								&& maximumTransferUnit >= MINIMUM_MTU_SIZE) {
							lowestMaximumTransferUnit = maximumTransferUnit;
							foundDevice = true;
						}
					}

					// This is a serious error and will cause startup to fail
					if (foundDevice == false) {
						throw new IOException(
								"Failed to locate a network interface with an MTU higher than the minimum ("
										+ MINIMUM_MTU_SIZE + ")");
					}
					DEVICE_MTU_SIZE = lowestMaximumTransferUnit;
				} catch (Throwable throwable2) {
					throwable2.printStackTrace();
					return -1;
				}
			}
			LOG.debug("Device maximum transfer unit determiened to be " + DEVICE_MTU_SIZE);
		}
		return DEVICE_MTU_SIZE;
	}

	/**
	 * Sends a raw message to the address for the amount of
	 * times in the interval until the packet is received or there is
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
					.option(ChannelOption.SO_RCVBUF, MINIMUM_MTU_SIZE).option(ChannelOption.SO_SNDBUF, MINIMUM_MTU_SIZE)
					.handler(handler);

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
	 * Returns whether or not the server is online.
	 * 
	 * @param address
	 *            the address of the server.
	 * @return <code>true</code> if the server is online, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isServerOnline(InetSocketAddress address) {
		// Create connection packet
		OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
		connectionRequestOne.maximumTransferUnit = MINIMUM_MTU_SIZE;
		connectionRequestOne.protocolVersion = CLIENT_NETWORK_PROTOCOL;
		connectionRequestOne.encode();

		// Wait for response to come in
		RakNetPacket packet = createBootstrapAndSend(address, connectionRequestOne, 1000, PING_RETRIES);
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
	 * Return whether or not the server is online.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is online, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isServerOnline(InetAddress address, int port) {
		return isServerOnline(new InetSocketAddress(address, port));
	}

	/**
	 * Returns whether or not the server is online.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is online, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public static boolean isServerOnline(String address, int port) throws UnknownHostException {
		return isServerOnline(InetAddress.getByName(address), port);
	}

	/**
	 * Returns whether or not the server is compatible with the current client
	 * protocol.
	 * 
	 * @param address
	 *            the address of the server.
	 * @return <code>true</code> if the server is compatible with the current
	 *         client protocol, <code>false</code> otherwise.
	 */
	public static boolean isServerCompatible(InetSocketAddress address) {
		// Create connection packet
		OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
		connectionRequestOne.maximumTransferUnit = MINIMUM_MTU_SIZE;
		connectionRequestOne.protocolVersion = CLIENT_NETWORK_PROTOCOL;
		connectionRequestOne.encode();

		// Wait for response to come in
		RakNetPacket packet = createBootstrapAndSend(address, connectionRequestOne, 1000, PING_RETRIES);
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
	 * Returns whether or not the server is compatible with the current client
	 * protocol.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is compatible with the current
	 *         client protocol, <code>false</code> otherwise.
	 */
	public static boolean isServerCompatible(InetAddress address, int port) {
		return isServerCompatible(new InetSocketAddress(address, port));
	}

	/**
	 * Returns whether or not the server is comptaible with the current client
	 * protocol.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is compatible with the current
	 *         client protocol, <code>false</code> otherwise.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public static boolean isServerCompatible(String address, int port) throws UnknownHostException {
		return isServerCompatible(InetAddress.getByName(address), port);
	}

	/**
	 * Returns the server <code>Identifier</code>.
	 * 
	 * @param address
	 *            the address of the server.
	 * @return the server <code>Identifier</code>.
	 */
	public static Identifier getServerIdentifier(InetSocketAddress address) {
		// Create ping packet
		UnconnectedPing ping = new UnconnectedPing();
		ping.timestamp = (System.currentTimeMillis() - PING_TIMESTAMP);
		ping.pingId = PING_ID;
		ping.encode();
		if (ping.failed()) {
			LOG.error(UnconnectedPing.class.getSimpleName() + " failed to encode");
			return null;
		}

		// Wait for response to come in
		RakNetPacket packet = createBootstrapAndSend(address, ping, 1000, IDENTIFIER_RETRIES);
		if (packet != null) {
			if (packet.getId() == MessageIdentifier.ID_UNCONNECTED_PONG) {
				UnconnectedPong pong = new UnconnectedPong(packet);
				pong.decode();
				if (!pong.failed() && pong.magic == true) {
					return pong.identifier;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the server <code>Identifier</code>.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return the server <code>Identifier</code>.
	 */
	public static Identifier getServerIdentifier(InetAddress address, int port) {
		return getServerIdentifier(new InetSocketAddress(address, port));
	}

	/**
	 * Returns the server <code>Identifier</code>.
	 * 
	 * @param address
	 *            the address of the server.
	 * @param port
	 *            the port of the server.
	 * @return the server <code>Identifier</code>.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public static Identifier getServerIdentifier(String address, int port) throws UnknownHostException {
		return getServerIdentifier(InetAddress.getByName(address), port);
	}

	/**
	 * Returns whether or not the address is a local address.
	 * 
	 * @param address
	 *            the address to check.
	 * @return <code>true</code> if the address is a local address,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isLocalAddress(InetAddress address) {
		try {
			return address.isSiteLocalAddress() || address.equals(InetAddress.getByName("127.0.0.1"));
		} catch (UnknownHostException e) {
			return false; // Unknown host
		}
	}

	/**
	 * Returns whether or not the address is a local address.
	 * 
	 * @param address
	 *            the address to check.
	 * @return <code>true</code> if the address is a local address,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isLocalAddress(InetSocketAddress address) {
		return isLocalAddress(address.getAddress());
	}

	/**
	 * Parses a single String as an address and port and converts it to an
	 * <code>InetSocketAddress</code>.
	 * 
	 * @param address
	 *            the address to convert.
	 * @param defaultPort
	 *            the default port to use if one is not.
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
	 *            the default port to use if one is not.
	 * @return the parsed <code>InetSocketAddress</code>.
	 */
	public static InetSocketAddress parseAddressPassive(String address, int defaultPort) {
		try {
			return parseAddress(address, defaultPort);
		} catch (UnknownHostException e) {
			return null; // Unknown host
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
	 * Parses a String as a long and returns <code>-1</code> in the case of a
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
			return -1; // Failed to parse
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
		return (int) parseLongPassive(intStr);
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
		return (short) parseLongPassive(shortStr);
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
		return (byte) parseLongPassive(byteStr);
	}

	/**
	 * Converts the ID to a hex string.
	 * 
	 * @param id
	 *            the ID to convert to a hex string.
	 * @return the generated hex string.
	 */
	public static String toHexStringId(int id) {
		return ("0x" + Integer.toHexString(id).toUpperCase());
	}

	/**
	 * Converts the ID of the <code>RakNetPacket</code> to a hex string.
	 * 
	 * @param packet
	 *            the packet to get the ID from.
	 * @return the generated hex string.
	 */
	public static String toHexStringId(RakNetPacket packet) {
		return toHexStringId(packet.getId());
	}

	/**
	 * Splits an array into more chunks with the maximum size for each
	 * array chunk.
	 * 
	 * @param src
	 *            the original array.
	 * @param size
	 *            the max size for each array that has been split.
	 * @return the split byte array's no bigger than the maximum size.
	 */
	public static final byte[][] splitArray(byte[] src, int size) {
		int index = 0;
		ArrayList<byte[]> split = new ArrayList<byte[]>();
		while (index < src.length) {
			if (index + size <= src.length) {
				split.add(Arrays.copyOfRange(src, index, index + size));
				index += size;
			} else {
				split.add(Arrays.copyOfRange(src, index, src.length));
				index = src.length;
			}
		}
		return split.toArray(new byte[split.size()][size]);
	}

	/**
	 * Returns all the integers in between each other as a normal subtraction.
	 * 
	 * @param low
	 *            the starting point.
	 * @param high
	 *            the ending point.
	 * @return the numbers in between high and low.
	 */
	public static final int[] subtractionArray(int low, int high) {
		if (low > high) {
			return new int[0];
		}

		int[] arr = new int[high - low - 1];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = i + low + 1;
		}
		return arr;
	}

}
