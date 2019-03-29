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
		 * Sets the callback for when the task has finished executing.
		 * <p>
		 * This callback method will be run on the same thread as the original
		 * task.
		 * 
		 * @param runnable
		 *            the callback.
		 */
		public void onFinish(Runnable runnable) {
			this.runnable = runnable;
		}

		/**
		 * Returns whether or not the UPnP task was successful.
		 * 
		 * @return <code>true</code> if the UPnP task was successful,
		 *         <code>false</code> otherwise.
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
	 * The maximum amount of chunks a single encapsulated packet can be split
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
	 * The default amount of time in milliseconds it will take for the peer to
	 * timeout.
	 * <p>
	 * This can be changed in a peer specifically via the
	 * {@link com.whirvis.jraknet.peer.RakNetPeer#setTimeout(long)
	 * RakNetPeer.setTimeout(long)} method.
	 */
	public static final long PEER_TIMEOUT = DETECTION_SEND_INTERVAL * 5;

	/**
	 * The amount of time in milliseconds an address will be blocked if it sends
	 * too many packets in one second.
	 */
	public static final long MAX_PACKETS_PER_SECOND_BLOCK = 300000L;

	private static final HashMap<InetAddress, Integer> MAXIMUM_TRANSFER_UNIT_SIZES = new HashMap<InetAddress, Integer>();
	private static int _lowestMaximumTransferUnitSize = -1;
	private static long _maxPacketsPerSecond = 500;

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
	 * Returns whether or not the specified IP address is a local address.
	 * 
	 * @param address
	 *            the IP address.
	 * @return <code>true</code> if the address is a local address,
	 *         <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public static boolean isLocalAddress(InetAddress address) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		byte[] ab = address.getAddress();
		if (address.isSiteLocalAddress()) {
			return true; // Local site address
		} else if (ab.length >= 4) {
			if (ab[0] == 127 && ab[1] == 0 && ab[2] == 0 && ab[3] >= 1 && ab[3] <= 8) {
				return true; // Address is in range of 127.0.0.1-127.0.0.8
			}
		} else if (ab.length >= 16) {
			if (ab[0] == 0 && ab[1] == 0 && ab[2] == 0 && ab[3] == 0 && ab[4] == 0 && ab[5] == 0 && ab[6] == 0
					&& ab[7] == 0 && ab[8] == 0 && ab[9] == 0 && ab[10] == 0 && ab[11] == 0 && ab[12] == 0
					&& ab[13] == 0 && ab[14] == 0 && ab[15] == 1) {
				return true; // Address is equal to 0:0:0:0:0:0:0:1
			}
		}
		return false;
	}

	/**
	 * Returns whether or not the specified IP address is a local address.
	 * 
	 * @param host
	 *            the IP address.
	 * @return <code>true</code> if the address is a local address,
	 *         <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public static boolean isLocalAddress(String host) throws NullPointerException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		return isLocalAddress(InetAddress.getByName(host));
	}

	/**
	 * Returns whether or not the address is a local address.
	 * 
	 * @param address
	 *            the address.
	 * @return <code>true</code> if the address is a local address,
	 *         <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>address</code> or the IP address of the
	 *             <code>address</code> are <code>null</code>.
	 */
	public static boolean isLocalAddress(InetSocketAddress address) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		return isLocalAddress(address.getAddress());
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
	 * {@link UPnPResult#wasSuccessful()} method found inside of
	 * {@link UPnPResult}. A callback for when the task finishes can also be set
	 * using the {@link UPnPResult#onFinish(Runnable)} method.
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
	 * {@link UPnPResult#wasSuccessful()} method found inside of
	 * {@link UPnPResult}. A callback for when the task finishes can also be set
	 * using the {@link UPnPResult#onFinish(Runnable)} method.
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

	/**
	 * Sends a packet to the specified address.
	 * 
	 * @param address
	 *            the address to send the packet to.
	 * @param packet
	 *            the packet to send.
	 * @param timeout
	 *            how long to wait until resending the packet.
	 * @param retries
	 *            how many times the packet will be sent before giving up.
	 * @return the packet received in response, <code>null</code> if no response
	 *         was received or the thread was interrupted.
	 * @throws NullPointerException
	 *             if the <code>address</code>, IP address of the
	 *             <code>address</code>, or <code>packet</code> are
	 *             <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>timeout</code> or <code>retries</code> are less
	 *             than or equal to <code>0</code>.
	 */
	private static RakNetPacket createBootstrapAndSend(InetSocketAddress address, Packet packet, long timeout,
			int retries) throws NullPointerException, IllegalArgumentException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
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
	 *             if the <code>address</code> or the IP address of the
	 *             <code>address</code> are <code>null</code>.
	 */
	public static boolean isServerOnline(InetSocketAddress address) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
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
	 * Returns whether or not the server with the specified address is online.
	 * 
	 * @param address
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is online, <code>false</code>
	 *         otherwise.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public static boolean isServerOnline(InetAddress address, int port) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		return isServerOnline(new InetSocketAddress(address, port));
	}

	/**
	 * Returns whether or not the server with the specified address is online.
	 * 
	 * @param host
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is online, <code>false</code>
	 *         otherwise.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public static boolean isServerOnline(String host, int port) throws NullPointerException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		return isServerOnline(InetAddress.getByName(host), port);
	}

	/**
	 * Returns whether or not the server with the specified address is
	 * compatible with the current client protocol.
	 * 
	 * @param address
	 *            the address of the server.
	 * @return <code>true</code> if the server is compatible with the current
	 *         client protocol, <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>address</code> or the IP address of the
	 *             <code>address</code> are <code>null</code>.
	 */
	public static boolean isServerCompatible(InetSocketAddress address) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		}
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
	 * Returns whether or not the server with the specified address is
	 * compatible with the current client protocol.
	 * 
	 * @param address
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is compatible with the current
	 *         client protocol, <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public static boolean isServerCompatible(InetAddress address, int port) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		return isServerCompatible(new InetSocketAddress(address, port));
	}

	/**
	 * Returns whether or not the server with the specified address is
	 * compatible with the current client protocol.
	 * 
	 * @param host
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return <code>true</code> if the server is compatible with the current
	 *         client protocol, <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public static boolean isServerCompatible(String host, int port) throws NullPointerException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		return isServerCompatible(InetAddress.getByName(host), port);
	}

	/**
	 * Returns the identifier of the server with the specified address.
	 * 
	 * @param address
	 *            the address of the server.
	 * @return the identifier of the server, <code>null</code> if it could not
	 *         be retrieved.
	 * @throws NullPointerException
	 *             if the <code>address</code> or the IP address of the
	 *             <code>address</code> are <code>null</code>.
	 */
	public static Identifier getServerIdentifier(InetSocketAddress address) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		}
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
	 * Returns the identifier of the server with the specified address.
	 * 
	 * @param address
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return the identifier of the server, <code>null</code> if it could not
	 *         be retrieved.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 */
	public static Identifier getServerIdentifier(InetAddress address, int port) throws NullPointerException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		return getServerIdentifier(new InetSocketAddress(address, port));
	}

	/**
	 * Returns the identifier of the server with the specified address.
	 * 
	 * @param host
	 *            the IP address of the server.
	 * @param port
	 *            the port of the server.
	 * @return the identifier of the server, <code>null</code> if it could not
	 *         be retrieved.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public static Identifier getServerIdentifier(String host, int port)
			throws NullPointerException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		return getServerIdentifier(InetAddress.getByName(host), port);
	}

	/**
	 * Returns the maximum transfer unit of the network card with the specified
	 * address.
	 * 
	 * @param address
	 *            the IP address. A <code>null</code> value will have the lowest
	 *            valid maximum transfer unit be returned instead.
	 * @return the maximum transfer unit of the network card with the specified
	 *         address, <code>-1</code> if it could not be determined.
	 * @throws RuntimeException
	 *             if an exception is caught when determining the lowest valid
	 *             maximum transfer unit size despite the safe checks put in
	 *             place.
	 */
	public static int getMaximumTransferUnit(InetAddress address) throws RuntimeException {
		// Calculate lowest valid maximum transfer unit
		if (_lowestMaximumTransferUnitSize < 0) {
			try {
				int lowestMaximumTransferUnitSize = Integer.MAX_VALUE;
				Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
				while (networkInterfaces.hasMoreElements()) {
					NetworkInterface networkInterface = networkInterfaces.nextElement();
					if (lowestMaximumTransferUnitSize > networkInterface.getMTU() && networkInterface.getMTU() >= 0) {
						lowestMaximumTransferUnitSize = networkInterface.getMTU();
					}
				}
				_lowestMaximumTransferUnitSize = lowestMaximumTransferUnitSize;
			} catch (SocketException | NullPointerException e) {
				throw new RuntimeException(e);
			}
		}

		// Get maximum transfer unit for address
		if (address == null) {
			return _lowestMaximumTransferUnitSize;
		} else if (!MAXIMUM_TRANSFER_UNIT_SIZES.containsKey(address)) {
			try {
				int maximumTransferUnit = NetworkInterface.getByInetAddress(address).getMTU();
				if (maximumTransferUnit < 0) {
					throw new SocketException("Invalid maximum transfer unit with size " + maximumTransferUnit);
				}
				MAXIMUM_TRANSFER_UNIT_SIZES.put(address, maximumTransferUnit);
			} catch (SocketException | NullPointerException e) {
				MAXIMUM_TRANSFER_UNIT_SIZES.put(address, _lowestMaximumTransferUnitSize);
			}
		}
		return MAXIMUM_TRANSFER_UNIT_SIZES.get(address).intValue();
	}

	/**
	 * Returns the maximum transfer unit of the network card with the specified
	 * address.
	 * <p>
	 * This method is simply a shorthand for
	 * {@link #getMaximumTransferUnit(InetAddress)}, with the port of the
	 * <code>address</code> not being used. Instead, the
	 * {@link InetSocketAddress#getAddress()} method is called to retrieve the
	 * original {@link InetAddress}. If the <code>address</code> is
	 * <code>null</code>, no <code>NullPointerException</code> will be thrown as
	 * the possibility of a <code>null</code> value is accounted for.
	 * 
	 * @param address
	 *            the address. A <code>null</code> value will have the lowest
	 *            valid maximum transfer unit be returned instead.
	 * @return the maximum transfer unit of the network card with the specified
	 *         address, <code>-1</code> if it could not be determined.
	 * @throws RuntimeException
	 *             if an exception is caught when determining the lowest valid
	 *             maximum transfer unit size despite the safe checks put in
	 *             place.
	 */
	public static int getMaximumTransferUnit(InetSocketAddress address) {
		return getMaximumTransferUnit(address == null ? null : address.getAddress());
	}

	/**
	 * Returns the lowest valid maximum transfer unit among all of the network
	 * cards installed on the machine.
	 * 
	 * @return the lowest valid maximum transfer unit among all of the network
	 *         cards installed on the machine.
	 */
	public static int getMaximumTransferUnit() {
		return getMaximumTransferUnit((InetAddress) /* Solves ambiguity */ null);
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
	 *            second before a peer is blocked.
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
	 * @throws NullPointerException
	 *             if the <code>packet</code> is <code>null</code>.
	 */
	public static String toHexStringId(RakNetPacket packet) throws NullPointerException {
		if (packet == null) {
			throw new NullPointerException("Packet cannot be null");
		}
		return toHexStringId(packet.getId());
	}

	/**
	 * Parses the specified string to a <code>long</code>.
	 * 
	 * @param longStr
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
	 * @param intStr
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
	 * @param shortStr
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

}
