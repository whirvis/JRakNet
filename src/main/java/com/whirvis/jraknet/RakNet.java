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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dosse.upnp.UPnP;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.connection.IncompatibleProtocolVersion;
import com.whirvis.jraknet.protocol.connection.OpenConnectionRequestOne;
import com.whirvis.jraknet.protocol.connection.OpenConnectionResponseOne;
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
 * The main RakNet component class, containing protocol information and utility
 * methods.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 */
public final class RakNet {

	/**
	 * A <code>Thread</code> which runs in the background to allow for code
	 * relating to UPnP to be executed through the WaifUPnP without locking up
	 * the main thread.
	 * 
	 * @author Whirvis T. Wheatley
	 * @since JRakNet v2.11.0
	 * @see #onFinish(Runnable)
	 * @see #wasSuccessful()
	 */
	public static class UPnPResult extends Thread {

		protected boolean finished;
		protected Runnable runnable;
		protected boolean success;

		/**
		 * Sets the callback for code that will be executed when the code for
		 * the task has finished executing. This code will be run on the same
		 * thread as the original task.
		 * 
		 * @param runnable
		 *            the callback.
		 */
		public void onFinish(Runnable runnable) {
			this.runnable = runnable;
		}

		/**
		 * Retursn whether or not the UPnP task was successful.
		 * 
		 * @return
		 * @throws IllegalStateException
		 *             if the UPnP code is still being executed.
		 */
		public boolean wasSuccessful() throws IllegalStateException {
			if (finished == false) {
				throw new IllegalStateException("UPnP code is still being executed");
			}
			return this.success;
		}

	}

	/**
	 * Used by the
	 * {@link RakNet#createBootstrapAndSend(InetSocketAddress, Packet, long, int)}
	 * method to wait for a packet.
	 *
	 * @author Whirvis T. Wheatley
	 * @since JRakNet v1.0.0
	 */
	private static final class BootstrapHandler extends ChannelInboundHandlerAdapter {

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

	/**
	 * Forwards the specified UDP port via
	 * <a href="https://en.wikipedia.org/wiki/Universal_Plug_and_Play">UPnP</a>.
	 * <p>
	 * In order for this method to work,
	 * <a href="https://en.wikipedia.org/wiki/Universal_Plug_and_Play">UPnP</a>
	 * for the router must be enabled. The way to enable this varies depending
	 * on the router. There is no guarantee this method will successfully
	 * forward the specified UDP port; as it is completely dependent on the
	 * gateway (the router in this case) to do so.
	 * <p>
	 * This is not a blocking method. However, the code required to accomplish
	 * the task can up to three seconds to execute. As a result, it is
	 * encapsulated within another thread so as to prevent unnecessary blocking.
	 * If one wishes to get the result of the code, use the
	 * {@link UPnPResult#wasSuccessful() wasSuccessful()} method found inside of
	 * {@link UPnPResult}. A callback for when the task finishes can also be
	 * added using the {@link UPnPResult#onFinish(Runnable) onFinish(Runnable)}
	 * method.
	 * 
	 * @param port
	 *            the port to forward.
	 * @return the result of the execution.
	 * @throws IllegalArgumentException
	 *             if the port is not within the range of <code>0-65535</code>.
	 */
	public static synchronized UPnPResult forwardPort(int port) throws IllegalArgumentException {
		if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Port must be in between 0-65535");
		}
		UPnPResult result = new UPnPResult() {
			@Override
			public void run() {
				this.setName("jraknet-port-forwarder-" + port);
				this.success = UPnP.openPortUDP(port);
				this.finished = true;
				if (runnable != null) {
					runnable.run();
				}
			}
		};
		result.start();
		return result;
	}

	/**
	 * Closes the specified UDP port via
	 * <a href="https://en.wikipedia.org/wiki/Universal_Plug_and_Play">UPnP</a>.
	 * <p>
	 * In order for this method to work,
	 * <a href="https://en.wikipedia.org/wiki/Universal_Plug_and_Play">UPnP</a>
	 * for the router must be enabled. The way to enable this varies depending
	 * on the router. There is no guarantee this method will successfully close
	 * the specified UDP port; as it is completely dependent on the gateway (the
	 * router in this case) to do so.
	 * <p>
	 * This is not a blocking method. However the code required to accomplish
	 * the task can up to three seconds to execute. As a result, it is
	 * encapsulated within another thread so as to prevent unnecessary blocking.
	 * If one wishes to get the result of the code, use the
	 * {@link UPnPResult#wasSuccessful() wasSuccessful()} method found inside of
	 * {@link UPnPResult}. A callback for when the task finishes can also be
	 * added using the {@link UPnPResult#onFinish(Runnable) onFinish(Runnable)}
	 * method.
	 * 
	 * @param port
	 *            the port to close.
	 * @return the result of the execution.
	 * @throws IllegalArgumentException
	 *             if the port is not within the range of <code>0-65535</code>.
	 */
	public static synchronized UPnPResult closePort(int port) throws IllegalArgumentException {
		if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Port must be in between 0-65535");
		}
		UPnPResult result = new UPnPResult() {
			@Override
			public void run() {
				this.setName("jraknet-port-closer-" + port);
				this.success = UPnP.closePortUDP(port);
				this.finished = true;
				if (runnable != null) {
					runnable.run();
				}
			}
		};
		result.start();
		return result;
	}

	private static final Logger LOG = LogManager.getLogger("jraknet");
	private static final HashMap<InetAddress, Integer> MAXIMUM_TRANSFER_UNIT_SIZES = new HashMap<InetAddress, Integer>();
	private static int lowestMaximumTransferUnitSize = -1;
	private static long _maxPacketsPerSecond = 500;

	/**
	 * The amount of times the {@link #isServerOnline(InetSocketAddress)} and
	 * {@link #isServerCompatible(InetSocketAddress)} methods will attempt to
	 * ping the server before giving up.
	 */
	public static final int PING_RETRIES = 5;

	/**
	 * The timestamp the {@link #isServerOnline(InetSocketAddress)} and
	 * {@link #isServerCompatible(InetSocketAddress)} methods will use as the
	 * ping timestamp.
	 */
	public static final long PING_TIMESTAMP = System.currentTimeMillis();

	/**
	 * The ping ID that the {@link #isServerOnline(InetSocketAddress)} and
	 * {@link #isServerCompatible(InetSocketAddress)} methods will use as the
	 * ping ID.
	 */
	public static final long PING_ID = UUID.randomUUID().getLeastSignificantBits();

	/**
	 * The amount of times the {@link #getServerIdentifier(InetSocketAddress)}
	 * method will attempt to retrieve the server identifier before giving up.
	 */
	public static final int IDENTIFIER_RETRIES = 3;

	/**
	 * The current supported server network protocol.
	 */
	public static final int SERVER_NETWORK_PROTOCOL = 9;

	/**
	 * The current supported client network protocol.
	 */
	public static final int CLIENT_NETWORK_PROTOCOL = 9;

	/**
	 * The minimum maximum transfer unit size.
	 */
	public static final int MINIMUM_MTU_SIZE = 400;

	/**
	 * The amount of available channels there are to send packets on.
	 */
	public static final int MAX_CHANNELS = 32;

	/**
	 * The default channel packets are sent on.
	 */
	public static final byte DEFAULT_CHANNEL = 0;

	/**
	 * The maximum amount of chunks a single encapsulatd packet can be split
	 * into.
	 */
	public static final int MAX_SPLIT_COUNT = 128;

	/**
	 * The maximum amount of split packets can be in the split handle queue.
	 */
	public static final int MAX_SPLITS_PER_QUEUE = 4;

	/**
	 * The interval at which not acknowledged packets are automatically resent.
	 */
	public static final long RECOVERY_SEND_INTERVAL = 50L;

	/**
	 * The interval at which keep-alive pings are sent.
	 */
	public static final long PING_SEND_INTERVAL = 2500L;

	/**
	 * The interval at which detection packets are sent.
	 */
	public static final long DETECTION_SEND_INTERVAL = 5000L;

	/**
	 * The amount of time in milliseconds
	 */
	public static final long SESSION_TIMEOUT = DETECTION_SEND_INTERVAL * 5;

	/**
	 * The amount of time in milliseconds an address will be blocked if it sends
	 * too many packets in one second.
	 */
	public static final long MAX_PACKETS_PER_SECOND_BLOCK = 300000L;

	private RakNet() {
		// Static class
	}

	/**
	 * Sleeps the current thread for the specified amount of time in
	 * milliseconds.
	 * <p>
	 * If an <code>InterruptedException</code> is caught during the sleep,
	 * <code>Thread.currentThread().interrupt()</code> will automatically be
	 * called.
	 * 
	 * @param time
	 *            the amount of time the thread should sleep in milliseconds.
	 * @return <code>true</code> if the current thread was interrupted during
	 *         the sleep, <code>false</code> otherwise.
	 */
	public static boolean sleep(long time) {
		try {
			Thread.sleep(time);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return true;
		}
	}

	/**
	 * Returns how many packets can be received in the span of a single second
	 * before an address is automatically blocked.
	 * 
	 * @return how many packets can be received in the span of a single second
	 *         before an address is automatically blocked.
	 */
	public static long getMaxPacketsPerSecond() {
		return _maxPacketsPerSecond;
	}

	/**
	 * Sets how many packets can be received in the span of a single second
	 * before an address is blocked.
	 * <p>
	 * One must take caution when setting this value, as setting it to low can
	 * cause communication to become impossible.
	 * 
	 * @param maxPacketsPerSecond
	 *            how many packets can be received in the span of a single
	 *            second before a session is blocked.
	 * @throws IllegalArgumentException
	 *             if <code>maxPacketsPerSecond</code> is less than
	 *             <code>0</code>.
	 */
	public static void setMaxPacketsPerSecond(long maxPacketsPerSecond) throws IllegalArgumentException {
		if (maxPacketsPerSecond < 0) {
			throw new IllegalArgumentException("Max packets per second must be greater than 0");
		}
		_maxPacketsPerSecond = maxPacketsPerSecond;
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
			if (port >= 0x0000 && port <= 0xFFFF) {
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
	 * Parses the specified string to a <code>long</code>.
	 * 
	 * @param byteStr
	 *            the string to parse.
	 * @return the string as a <code>long</code>, <code>-1</code> if it fails to
	 *         parse.
	 */
	public static long parseLongPassive(String longStr) {
		try {
			return Long.parseLong(longStr);
		} catch (NumberFormatException e) {
			return -1L; // Failed to parse
		}
	}

	/**
	 * Parses the specified string to an <code>int</code>.
	 * 
	 * @param byteStr
	 *            the string to parse.
	 * @return the string as an <code>int</code>, <code>-1</code> if it fails to
	 *         parse.
	 */
	public static int parseIntPassive(String intStr) {
		return (int) parseLongPassive(intStr);
	}

	/**
	 * Parses the specified string to a <code>short</code>.
	 * 
	 * @param byteStr
	 *            the string to parse.
	 * @return the string as a <code>short</code>, <code>-1</code> if fails to
	 *         parse.
	 */
	public static short parseShortPassive(String shortStr) {
		return (short) parseLongPassive(shortStr);
	}

	/**
	 * Parses the specified string to a <code>byte</code>.
	 * 
	 * @param byteStr
	 *            the string to parse.
	 * @return the string as a <code>byte</code>, <code>-1</code> if it fails to
	 *         parse.
	 */
	public static byte parseBytePassive(String byteStr) {
		return (byte) parseLongPassive(byteStr);
	}

	/**
	 * Converts the specified ID to a hex string.
	 * 
	 * @param id
	 *            the ID to convert to a hex string.
	 * @return the generated hex string.
	 */
	public static String toHexStringId(int id) {
		String hexString = Integer.toHexString(id);
		if (hexString.length() % 2 != 0) {
			hexString = "0" + hexString;
		}
		return "0x" + hexString.toUpperCase();
	}

	/**
	 * Converts the ID of the specified packet to a hex string.
	 * 
	 * @param packet
	 *            the packet to get the ID from.
	 * @return the generated hex string.
	 */
	public static String toHexStringId(RakNetPacket packet) {
		return toHexStringId(packet.getId());
	}

	/**
	 * Converts the stack trace of the specified <code>Throwable</code> to a
	 * string.
	 * 
	 * @param throwable
	 *            the <code>Throwable</code> to get the stack trace from.
	 * @return the stack trace as a string.
	 * @throws NullPointerException
	 *             if the <code>throwable</code> is <code>null</code>.
	 */
	public static String getStackTrace(Throwable throwable) throws NullPointerException {
		if (throwable == null) {
			throw new NullPointerException("Throwable cannot be null");
		}
		ByteArrayOutputStream stackTraceOut = new ByteArrayOutputStream();
		PrintStream stackTracePrint = new PrintStream(stackTraceOut);
		throwable.printStackTrace(stackTracePrint);
		return new String(stackTraceOut.toByteArray());
	}

	// TODO: FIX THIS!!!
	static {
		try {
			lowestMaximumTransferUnitSize = Integer.MAX_VALUE;
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				if (networkInterface.getMTU() < lowestMaximumTransferUnitSize && networkInterface.getMTU() >= 0) {
					lowestMaximumTransferUnitSize = networkInterface.getMTU();
					System.out.println(lowestMaximumTransferUnitSize);
				}
			}
		} catch (SocketException | NullPointerException e) {
			e.printStackTrace();
			lowestMaximumTransferUnitSize = -1;
		}
	}

	/**
	 * Returns the maximum transfer unit of the network card with the specified
	 * address.
	 * 
	 * @param address
	 *            the address. A <code>null</code> value will have this function
	 *            go through each and every network interface on the machine and
	 *            return the lowest value the first time it is called.
	 * @return the maximum transfer unit of the network card with the specified
	 *         address, <code>-1</code> if it could not be determined.
	 */
	public static int getMaximumTransferUnit(InetAddress address) {
		if (address == null) {
			return lowestMaximumTransferUnitSize;
		}
		if (!MAXIMUM_TRANSFER_UNIT_SIZES.containsKey(address)) {
			try {
				MAXIMUM_TRANSFER_UNIT_SIZES.put(address, NetworkInterface.getByInetAddress(address).getMTU());
			} catch (SocketException | NullPointerException e) {
				MAXIMUM_TRANSFER_UNIT_SIZES.put(address, lowestMaximumTransferUnitSize);
			}
		}
		return MAXIMUM_TRANSFER_UNIT_SIZES.get(address).intValue();
	}

	/**
	 * Returns the maximum transfer unit of the network card with the localhost
	 * address according to {@link InetAddress#getLocalHost()}.
	 * 
	 * @return the maximum transfer unit of the network card with the specified
	 *         address, <code>-1</code> if it could not be determined.
	 */
	public static int getMaximumTransferUnit() {
		return getMaximumTransferUnit(null);
	}

	/**
	 * Sends a packet to the address.
	 * 
	 * @param address
	 *            the address to send the packet to.
	 * @param packet
	 *            the packet to send.
	 * @param timeout
	 *            how long to wait until resending the packet.
	 * @param retries
	 *            how many times the packet will be sent before giving uip.
	 * @return the packet received in response, <code>null</code> if no response
	 *         was received or the thread was interrupted.
	 * @throws NullPointerException
	 *             if the <code>address<code> or <code>packet</code> are
	 *             <code>null</code>.
	 * @throws IlelgalArgumentException
	 *             if the <code>timeout</code> or <code>retries</code> are less
	 *             than or equal to </code>0</code>.
	 */
	private static RakNetPacket createBootstrapAndSend(InetSocketAddress address, Packet packet, long timeout,
			int retries) throws NullPointerException, IllegalArgumentException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		} else if (timeout <= 0) {
			throw new IllegalArgumentException("Timeout must be greater than 0");
		} else if (retries <= 0) {
			throw new IllegalArgumentException("Retriest must be greater than 0");
		}

		// Prepare bootstrap
		RakNetPacket received = null;
		EventLoopGroup group = new NioEventLoopGroup();
		int maximumTransferUnit = getMaximumTransferUnit();
		if (maximumTransferUnit < MINIMUM_MTU_SIZE) {
			return null;
		}
		try {
			// Create bootstrap
			Bootstrap bootstrap = new Bootstrap();
			BootstrapHandler handler = new BootstrapHandler();
			bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, maximumTransferUnit)
					.option(ChannelOption.SO_SNDBUF, maximumTransferUnit).handler(handler);
			Channel channel = bootstrap.bind(0).sync().channel();

			// Wait for response
			while (retries > 0 && received == null && !Thread.currentThread().isInterrupted()) {
				long sendTime = System.currentTimeMillis();
				channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
				while (System.currentTimeMillis() - sendTime < timeout && handler.packet == null)
					; // Wait for either a timeout or a response
				received = handler.packet;
				retries--;
			}
		} catch (InterruptedException e) {
			return null;
		}
		group.shutdownGracefully();
		return received;
	}

	/**
	 * Returns whether or not the server with the specified address is online.
	 * 
	 * @param address
	 *            the address of the server.
	 * @return <code>true</code> if the server is online, <code>false</code>
	 *         otherwise.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public static boolean isServerOnline(InetSocketAddress address) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		}
		OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
		connectionRequestOne.maximumTransferUnit = MINIMUM_MTU_SIZE;
		connectionRequestOne.networkProtocol = CLIENT_NETWORK_PROTOCOL;
		connectionRequestOne.encode();
		RakNetPacket packet = createBootstrapAndSend(address, connectionRequestOne, 1000, PING_RETRIES);
		if (packet != null) {
			if (packet.getId() == RakNetPacket.ID_OPEN_CONNECTION_REPLY_1) {
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
	 *            the IP address of the server.
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
	 * @param host
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is online, <code>false</code>
	 *         otherwise.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public static boolean isServerOnline(String host, int port) throws UnknownHostException {
		return isServerOnline(InetAddress.getByName(host), port);
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
		OpenConnectionRequestOne connectionRequestOne = new OpenConnectionRequestOne();
		connectionRequestOne.maximumTransferUnit = MINIMUM_MTU_SIZE;
		connectionRequestOne.networkProtocol = CLIENT_NETWORK_PROTOCOL;
		connectionRequestOne.encode();
		RakNetPacket packet = createBootstrapAndSend(address, connectionRequestOne, 1000L, PING_RETRIES);
		if (packet != null) {
			if (packet.getId() == RakNetPacket.ID_OPEN_CONNECTION_REPLY_1) {
				OpenConnectionResponseOne connectionResponseOne = new OpenConnectionResponseOne(packet);
				connectionResponseOne.decode();
				if (connectionResponseOne.magic == true) {
					return true;
				}
			} else if (packet.getId() == RakNetPacket.ID_INCOMPATIBLE_PROTOCOL_VERSION) {
				IncompatibleProtocolVersion incompatibleProtocol = new IncompatibleProtocolVersion(packet);
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
	 * @param host
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is compatible with the current
	 *         client protocol, <code>false</code> otherwise.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public static boolean isServerCompatible(String host, int port) throws UnknownHostException {
		return isServerCompatible(InetAddress.getByName(host), port);
	}

	/**
	 * Returns the server <code>Identifier</code>.
	 * 
	 * @param address
	 *            the address of the server.
	 * @return the server <code>Identifier</code>.
	 */
	public static Identifier getServerIdentifier(InetSocketAddress address) {
		UnconnectedPing ping = new UnconnectedPing();
		ping.timestamp = System.currentTimeMillis() - PING_TIMESTAMP;
		ping.pingId = PING_ID;
		ping.encode();
		if (ping.failed()) {
			throw new RuntimeException(UnconnectedPing.class.getSimpleName() + " failed to encode");
		}
		RakNetPacket packet = createBootstrapAndSend(address, ping, 1000, IDENTIFIER_RETRIES);
		if (packet != null) {
			if (packet.getId() == RakNetPacket.ID_UNCONNECTED_PONG) {
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
	 * @param host
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return the server <code>Identifier</code>.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public static Identifier getServerIdentifier(String host, int port) throws UnknownHostException {
		return getServerIdentifier(InetAddress.getByName(host), port);
	}

}
