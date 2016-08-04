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
 * Copyright (c) 2016 Whirvis T. Wheatley
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
package net.marfgamer.raknet.utils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Random;

import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.Message;
import net.marfgamer.raknet.protocol.identifier.MessageIdentifiers;
import net.marfgamer.raknet.protocol.raknet.UnconnectedConnectionRequestOne;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPing;
import net.marfgamer.raknet.protocol.raknet.UnconnectedPong;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Used to easily accomplish RakNet related tasks
 *
 * @author Whirvis T. Wheatley
 */
public abstract class RakNetUtils implements RakNet, MessageIdentifiers {

	private static long raknetId = -1;

	static {
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
	}

	/**
	 * Used to get the ID used by RakNet assigned to this system
	 * 
	 * @return long
	 */
	public static long getRakNetID() {
		if (raknetId <= -1) {
			try {
				// Does the file exist?
				boolean generateId = false;
				if (!idFile.exists()) {
					idFile.createNewFile();
					generateId = true;
				}

				// Is the JSON valid?
				try {
					new JsonParser().parse(new FileReader(idFile));
				} catch (JsonSyntaxException e) {
					generateId = true;
				}

				// Should we generate a new file?
				if (generateId) {
					FileWriter writer = new FileWriter(idFile, false);
					writer.write("{}");
					writer.flush();
					writer.close();
				}

				// Has the ID been generated?
				JsonObject idObject = new JsonParser().parse(
						new FileReader(idFile)).getAsJsonObject();
				if (!idObject.has("id")) {
					// Generate ID
					long newId = new Random().nextLong();
					idObject.addProperty("id",
							(newId > 0 ? newId : Math.abs(newId)));

					// Write data
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					FileWriter writer = new FileWriter(idFile, false);
					writer.write(gson.toJson(idObject));
					writer.flush();
					writer.close();
				}

				raknetId = idObject.get("id").getAsLong();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return raknetId;
	}

	/**
	 * Used to get the maximum transfer unit on the device based on the network
	 * interface address
	 * 
	 * @return int
	 */
	public static int getNetworkInterfaceMTU() {
		try {
			Enumeration<NetworkInterface> ni = NetworkInterface
					.getNetworkInterfaces();
			while (ni.hasMoreElements()) {
				NetworkInterface networkInterface = ni.nextElement();
				Enumeration<InetAddress> addresses = networkInterface
						.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (address.equals(InetAddress.getLocalHost())) {
						return networkInterface.getMTU();
					}
				}
			}
			return MINIMUM_TRANSFER_UNIT; // We couldn't find a match
		} catch (SocketException | UnknownHostException e) {
			return MINIMUM_TRANSFER_UNIT; // An error occurred
		}
	}

	/**
	 * Used to quickly send a packet to a sever and get it's response, do
	 * <b><i><u>NOT</u></i></b> use for long time communication!
	 * 
	 * @param address
	 *            The address of the receiver
	 * @param port
	 *            The port of the receiver
	 * @param packet
	 *            The packet to send
	 * @param timeout
	 *            The amount of time the receiver has to respond
	 * @return Packet
	 */
	public static Message createBootstrapAndSend(String address, int port,
			Message packet, long timeout) {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			// Create bootstrap, bind and send
			Bootstrap b = new Bootstrap();
			BootstrapHandler handler = new BootstrapHandler();
			b.group(group).channel(NioDatagramChannel.class)
					.option(ChannelOption.SO_BROADCAST, true)
					.option(ChannelOption.SO_RCVBUF, MINIMUM_TRANSFER_UNIT)
					.option(ChannelOption.SO_SNDBUF, MINIMUM_TRANSFER_UNIT)
					.handler(handler);
			Channel channel = b.bind(0).sync().channel();
			channel.writeAndFlush(new DatagramPacket(packet.buffer(),
					new InetSocketAddress(address, port)));

			// Wait for packet to come in, return null on timeout
			long waitTime = System.currentTimeMillis();
			synchronized (handler) {
				while (System.currentTimeMillis() - waitTime <= timeout) {
					if (handler.packet != null) {
						return handler.packet;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			group.shutdownGracefully();
		}
		return null;
	}

	/**
	 * Used to quickly send a packet to a sever and get it's response after no
	 * longer than 1000 MS, do <b><i><u>NOT</u></i></b> use for long time
	 * communication!
	 * 
	 * @param address
	 *            The address of the receiver
	 * @param port
	 *            The port of the receiver
	 * @param packet
	 *            The packet to send
	 * @return Packet
	 */
	public static Message createBootstrapAndSend(String address, int port,
			Message packet) {
		return createBootstrapAndSend(address, port, packet, 1000L);
	}

	/**
	 * Returns the specified server's identifier
	 * 
	 * @param address
	 *            The address of the server
	 * @param port
	 *            The port of the server
	 * @param timeout
	 *            The amount of time the server has to respond
	 * @return String
	 */
	public static String getServerIdentifier(String address, int port,
			long timeout) {
		UnconnectedPing ping = new UnconnectedPing();
		ping.pingId = System.currentTimeMillis();
		ping.clientId = raknetId;
		ping.encode();

		Message sprr = createBootstrapAndSend(address, port, ping, timeout);
		if (sprr != null) {
			if (sprr.getId() == ID_UNCONNECTED_PONG) {
				UnconnectedPong pong = new UnconnectedPong(sprr);
				pong.decode();
				if (pong.magic == true && pong.pingId == ping.pingId) {
					return pong.identifier;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the specified server's identifier
	 * 
	 * @param address
	 *            The address of the server
	 * @param port
	 *            The port of the server
	 * @return String
	 */
	public static String getServerIdentifier(String address, int port) {
		return getServerIdentifier(address, port, 1000L);
	}

	/**
	 * Makes sure the server with the specified address is online
	 * 
	 * @param address
	 *            The address of the server
	 * @param port
	 *            The port of the server
	 * @param timeout
	 *            The amount of time the server has to respond
	 * @return boolean
	 */
	public static boolean isServerOnline(String address, int port, long timeout) {
		UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne();
		request.mtuSize = MINIMUM_TRANSFER_UNIT;
		request.protocol = CLIENT_NETWORK_PROTOCOL;
		request.encode();

		Message response = createBootstrapAndSend(address, port, request,
				timeout);
		return (response != null);
	}

	/**
	 * Makes sure the server with the specified address is online
	 * 
	 * @param address
	 *            The address of the server
	 * @param port
	 *            The port of the server
	 * @return boolean
	 */
	public static boolean isServerOnline(String address, int port) {
		return isServerOnline(address, port, 1000L);
	}

	/**
	 * Makes sure the specified protocol is compatible with the server
	 * 
	 * @param address
	 *            The address of the server
	 * @param port
	 *            The port of the server
	 * @param protocol
	 *            The protocol of the server
	 * @param timeout
	 *            The amount of time the server has to respond
	 * @return boolean
	 */
	public static boolean isServerCompatible(String address, int port,
			int protocol, long timeout) {
		UnconnectedConnectionRequestOne request = new UnconnectedConnectionRequestOne();
		request.mtuSize = MINIMUM_TRANSFER_UNIT;
		request.protocol = (short) protocol;
		request.encode();

		Message response = createBootstrapAndSend(address, port, request,
				timeout);
		if (response != null) {
			return (response.getId() == ID_UNCONNECTED_CONNECTION_REPLY_1 && response
					.getId() == ID_UNCONNECTED_CONNECTION_REPLY_1);
		}
		return false;
	}

	/**
	 * Makes sure the specified protocol is compatible with the server
	 * 
	 * @param address
	 *            The address of the server
	 * @param port
	 *            The port of the server
	 * @param protocol
	 *            The protocol of the server
	 * @return boolean
	 */
	public static boolean isServerCompatible(String address, int port,
			int protocol) {
		return isServerCompatible(address, port, protocol, 1000L);
	}

	/**
	 * Makes sure the current protocol is compatible with the server
	 * 
	 * @param address
	 *            The address of the server
	 * @param port
	 *            The port of the server
	 * @param timeout
	 *            How long the server has to respond
	 * @return boolean
	 */
	public static boolean isServerCompatible(String address, int port,
			long timeout) {
		return isServerCompatible(address, port, CLIENT_NETWORK_PROTOCOL,
				timeout);
	}

	/**
	 * Makes sure the current protocol is compatible with the server
	 * 
	 * @param address
	 *            The address of the server
	 * @param port
	 *            The port of the server
	 * @return boolean
	 */
	public static boolean isServerCompatible(String address, int port) {
		return isServerCompatible(address, port, 1000L);
	}

	/**
	 * Converts the specified integer to a neat hexadecimal String
	 * 
	 * @param i
	 *            The integer to convert
	 * @return String
	 */
	public static String toHexString(int i) {
		String hex = new String("0x");
		String rawHex = Integer.toHexString(i).toUpperCase();
		if (rawHex.length() == 1) {
			hex += "0";
		}
		hex += rawHex;
		return hex;
	}

	private static class BootstrapHandler extends ChannelInboundHandlerAdapter {

		public volatile Message packet;

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object obj)
				throws Exception {
			if (obj instanceof DatagramPacket) {
				DatagramPacket msg = (DatagramPacket) obj;
				this.packet = new Message(msg.content().retain());
			} else {
				throw new IOException("Received " + obj.getClass().getName()
						+ " instead of " + DatagramPacket.class.getName());
			}
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

	}

}
