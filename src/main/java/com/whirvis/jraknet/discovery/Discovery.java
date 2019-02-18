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
 * Copyright (c) 2016-2019 Trent Summerlin
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
package com.whirvis.jraknet.discovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.status.UnconnectedPong;
import com.whirvis.jraknet.scheduler.Scheduler;

/**
 * This thread is used by a {@link com.whirvis.jraknet.client.RakNetClient
 * RakNetClient} to discover servers without needing to be explicitly started.
 * <p>
 * Only one instance of this class can exist at a time. When a client is
 * created, it will automatically check if a discovery system is already
 * running. If none exists, one will be created and started automatically. Once
 * the last client using the discovery system has shutdown, it will shut down
 * the discovery system and nullify the reference. If another client is created
 * after this, the process will repeat.
 *
 * @author Trent Summerlin
 * @since JRakNet v2.0
 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
 * @see com.whirvis.jraknet.client.RakNetClientListener RakNetClientListener
 * @see com.whirvis.jraknet.discovery.DiscoveredServer DiscoveredServer
 */
public class Discovery extends Thread {

	private static final String BROADCAST_ADDRESS = "255.255.255.255";
	private static final boolean LOCAL_SERVER = false;
	private static final boolean EXTERNAL_SERVER = true;

	private static final IntFunction<DiscoveredServer[]> DISCOVERED_SERVER_FUNCTION = new IntFunction<DiscoveredServer[]>() {

		@Override
		public DiscoveredServer[] apply(int value) {
			return new DiscoveredServer[value];
		}

	};

	private static final Logger LOG = LogManager.getLogger("jraknet-discovery");
	private static final long TIMESTAMP = System.currentTimeMillis();
	protected static final long PING_ID = UUID.randomUUID().getLeastSignificantBits();
	protected static final ArrayList<DiscoveryListener> LISTENERS = new ArrayList<DiscoveryListener>();
	protected static final ConcurrentHashMap<InetSocketAddress, Boolean> DISCOVERY_ADDRESSES = new ConcurrentHashMap<InetSocketAddress, Boolean>();
	protected static final ConcurrentHashMap<InetSocketAddress, DiscoveredServer> DISCOVERED = new ConcurrentHashMap<InetSocketAddress, DiscoveredServer>();

	private static DiscoveryMode discoveryMode = DiscoveryMode.ALL_CONNECTIONS;

	protected static DiscoveryThread thread;

	public static void main(String[] args) throws Exception {
		Discovery.addDiscoveryPort(19132);
		Discovery.addExternalServer("sg.lbsg.net", 19132);
		Discovery.addExternalServer("192.168.1.6", 19132);
		Discovery.addListener(new DiscoveryListener() {

			@Override
			public void onServerDiscovered(InetSocketAddress address, boolean external, Identifier identifier) {
				System.out.println((external ? "EXTERNAL " : "LOCAL ") + address + " DISCOVERED -> " + identifier);
			}

		});

		thread = new DiscoveryThread();
		thread.start();
	}

	/**
	 * Returns the timestamp of the discovery system. The timestamp of the
	 * discovery system is how long in milliseconds has passed since it has been
	 * created.
	 * 
	 * @return the timestamp of the discovery system.
	 */
	protected static long getTimestamp() {
		return System.currentTimeMillis() - TIMESTAMP;
	}

	/**
	 * Calls an event.
	 * 
	 * @param event
	 *            the event to call.
	 * @see com.whirvis.jraknet.discovery.DiscoveryListener DiscoveryListener
	 */
	protected static void callEvent(Consumer<? super DiscoveryListener> event) {
		LISTENERS.forEach(listener -> Scheduler.scheduleSync(listener, event));
	}

	/**
	 * Adds a listener to the discovery system. Listeners are used to listen for
	 * events that occur relating to the client such as discovering servers,
	 * forgetting servers, etc.
	 * 
	 * @param listener
	 *            the listener to add.
	 * @throws NullPointerException
	 *             if the listener is <code>null</code>.
	 * @see com.whirvis.jraknet.discovery.DiscoveryListener DiscoveryListener
	 */
	public static void addListener(DiscoveryListener listener) {
		if (listener == null) {
			throw new NullPointerException("Listener cannot be null");
		}
		LISTENERS.add(listener);
		if (thread == null) {
			thread = new DiscoveryThread();
			thread.start();
		}
	}

	/**
	 * Removes a listener from the discovery system.
	 * 
	 * @param listener
	 *            the listener to remove.
	 * @see com.whirvis.jraknet.discovery.DiscoveryListener DiscoveryListener
	 */
	public static void removeListener(DiscoveryListener listener) {
		LISTENERS.remove(listener);
	}

	/**
	 * Returns the addresses to broadcast pings to for server discovery.
	 * 
	 * @return the addresses to broadcast pings to for server discovery.
	 */
	public static InetSocketAddress[] getDiscoveryAddresses() {
		return DISCOVERY_ADDRESSES.keySet().toArray(new InetSocketAddress[DISCOVERY_ADDRESSES.size()]);
	}

	/**
	 * Adds discovery ports to start broadcasting to on the local network.
	 * 
	 * @param discoveryPorts
	 *            the ports to start broadcasting to.
	 * @throws IllegalArgumentException
	 *             if one of the ports is not within the range of
	 *             <code>0-65535</code>.
	 */
	public static void addDiscoveryPorts(int... discoveryPorts) {
		for (int discoveryPort : discoveryPorts) {
			if (discoveryPort < 0x0000 || discoveryPort > 0xFFFF) {
				throw new IllegalArgumentException("Invalid port range");
			}
			InetSocketAddress discoveryAddress = new InetSocketAddress(BROADCAST_ADDRESS, discoveryPort);
			DISCOVERY_ADDRESSES.put(discoveryAddress, LOCAL_SERVER);
		}
	}

	/**
	 * Adds a discovery port to start broadcasting to on the local network.
	 * 
	 * @param discoveryPort
	 *            the port to start broadcasting to.
	 * @throws IllegalArgumentException
	 *             if the port is not within the range of <code>0-65535</code>.
	 */
	public static void addDiscoveryPort(int discoveryPort) {
		addDiscoveryPorts(discoveryPort);
	}

	/**
	 * Removes discovery ports to stop broadcasting to on the local network.
	 * 
	 * @param discoveryPorts
	 *            the port to stop broadcasting to.
	 */
	public static void removeDiscoveryPorts(int... discoveryPorts) {
		for (int discoveryPort : discoveryPorts) {
			/*
			 * It would makes sense to check if the address is a local address
			 * and not an external address. However, the broadcast address
			 * 255.255.255.255 could never be used for external servers, so it
			 * is not checked for here. As an extra safeguard, the
			 * addExternalServer() method will not allow for an IP address that
			 * is equivalent to that of the broadcast address 255.255.255.255.
			 */
			InetSocketAddress discoveryAddress = new InetSocketAddress(BROADCAST_ADDRESS, discoveryPort);
			DISCOVERY_ADDRESSES.remove(discoveryAddress);
		}
	}

	/**
	 * Removes a discovery port to stop broadcasting to on the local network.
	 * 
	 * @param discoveryPorts
	 *            the port to stop broadcasting to.
	 */
	public static void removeDiscoveryPort(int discoveryPort) {
		removeDiscoveryPorts(discoveryPort);
	}

	/**
	 * Returns the locally discovered servers.
	 * 
	 * @return the locally discovered servers.
	 * @see com.whirvis.jraknet.discovery.DiscoveryMode DiscoveryMode
	 */
	public static DiscoveredServer[] getLocalServers() {
		return DISCOVERED.values().stream().filter(server -> !server.isExternal()).toArray(DISCOVERED_SERVER_FUNCTION);
	}

	/**
	 * Returns the externally discovered servers.
	 * 
	 * @return the externally discovered servers.
	 * @see com.whirvis.jraknet.discovery.DiscoveryMode DiscoveryMode
	 */
	public static DiscoveredServer[] getExternalServers() {
		return DISCOVERED.values().stream().filter(server -> server.isExternal()).toArray(DISCOVERED_SERVER_FUNCTION);
	}

	/**
	 * Adds a server to the client's external server discovery list. This allows
	 * for the discovery of servers on external networks.
	 * 
	 * @param address
	 *            the server address.
	 */
	public static void addExternalServer(InetSocketAddress address) {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannnot be null");
		} else if (BROADCAST_ADDRESS.equals(address.getAddress().getHostAddress())) {
			throw new IllegalArgumentException("IP address cannot be broadcast address " + BROADCAST_ADDRESS);
		}
		DISCOVERY_ADDRESSES.put(address, EXTERNAL_SERVER);
		if (thread == null && LISTENERS.size() > 0) {
			thread = new DiscoveryThread();
			thread.start();
		}
	}

	/**
	 * Adds a server to the client's external server discovery list. This allows
	 * for the discovery of servers on external networks.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 */
	public static void addExternalServer(InetAddress address, int port) {
		addExternalServer(new InetSocketAddress(address, port));
	}

	/**
	 * Adds a server to the client's external server discovery list. This allows
	 * for the discovery of servers on external networks.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public static void addExternalServer(String address, int port) throws UnknownHostException {
		addExternalServer(InetAddress.getByName(address), port);
	}

	/**
	 * Removes an external server from the client's external server discovery
	 * list.
	 * 
	 * @param address
	 *            the server address.
	 * @throws IllegalArgumentException
	 *             if the address is the address of a local server rather than
	 *             an external server.
	 */
	public static void removeExternalServer(InetSocketAddress address) {
		if (address == null) {
			return; // No address to remove
		} else if (!DISCOVERY_ADDRESSES.containsKey(address)) {
			return; // No address to remove
		} else if (DISCOVERY_ADDRESSES.get(address).booleanValue() != EXTERNAL_SERVER) {
			throw new IllegalArgumentException("Address must be that of an external server");
		}
		// TODO: Forget server if it exists!
		DISCOVERY_ADDRESSES.remove(address);
	}

	/**
	 * Removes an external server from the client's external server discovery
	 * list.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 */
	public static void removeExternalServer(InetAddress address, int port) {
		removeExternalServer(new InetSocketAddress(address, port));
	}

	/**
	 * Removes an external server from the client's external server discovery
	 * list.
	 * 
	 * @param address
	 *            the server address.
	 * @param port
	 *            the server port.
	 * @throws UnknownHostException
	 *             if the address is an unknown host.
	 */
	public static void removeExternalServer(String address, int port) throws UnknownHostException {
		removeExternalServer(InetAddress.getByName(address), port);
	}

	/**
	 * Removes an external server from the client's external server discovery
	 * list.
	 * 
	 * @param server
	 *            the discovered server.
	 * @see com.whirvis.jraknet.discovery.DiscoveryMode DiscoveryMode
	 */
	public static void removeExternalServer(DiscoveredServer server) throws IllegalArgumentException {
		removeExternalServer(server.getAddress());
	}

	/**
	 * Removes all external servers from the external server discovery list.
	 * 
	 * @see com.whirvis.jraknet.discovery.DiscoveredServer DiscoveredServer
	 */
	public static void clearExternalServers() {
		if (!DISCOVERY_ADDRESSES.containsValue(EXTERNAL_SERVER)) {
			return; // No external servers to clear
		}
		Iterator<InetSocketAddress> addresses = DISCOVERY_ADDRESSES.keySet().iterator();
		while (addresses.hasNext()) {
			InetSocketAddress address = addresses.next();
			boolean external = DISCOVERY_ADDRESSES.get(address).booleanValue();
			if (external == true) {
				// TODO: Forget server!
				addresses.remove();
			}
		}
		LOG.debug("Cleared external servers");
	}

	/**
	 * Returns the discovered servers, both local and external.
	 * 
	 * @return the discovered servers, both local and external.
	 * @see com.whirvis.jraknet.discovery.DiscoveredServer DiscoveredServer
	 */
	public static DiscoveredServer[] getServers() {
		return DISCOVERED.values().toArray(new DiscoveredServer[DISCOVERED.size()]);
	}

	/**
	 * Returns the discovery mode.
	 * 
	 * @return the discovery mode.
	 * @see com.whirvis.jraknet.discovery.DiscoveryMode DiscoveryMode
	 */
	public static DiscoveryMode getDiscoveryMode() {
		if (discoveryMode == null) {
			discoveryMode = DiscoveryMode.DISABLED;
		}
		return discoveryMode;
	}

	/**
	 * Sets the client's discovery mode. If disabling the discovery system, all
	 * of the currently discovered servers will be treated as if they had been
	 * forgotten.
	 * 
	 * @param mode
	 *            the new discovery mode. A <code>null</code> value will have
	 *            the discovery mode be set to
	 *            {@link com.whirvis.jraknet.discovery.DiscoveryMode#DISABLED
	 *            DISABLED}.
	 * @see com.whirvis.jraknet.discovery.DiscoveryMode DiscoveryMode
	 */
	public static void setDiscoveryMode(DiscoveryMode mode) {
		discoveryMode = (mode == null ? DiscoveryMode.DISABLED : mode);
		if (discoveryMode == DiscoveryMode.DISABLED) {
			DISCOVERY_ADDRESSES.keySet().stream().filter(address -> DISCOVERED.containsKey(address))
					.forEach(address -> callEvent(listener -> listener.onServerForgotten(address,
							DISCOVERY_ADDRESSES.get(listener).booleanValue())));
			DISCOVERED.clear(); // Forget all servers
		} else if (thread == null && LISTENERS.size() > 0) {
			thread = new DiscoveryThread();
			thread.start();
		}
		LOG.debug("Set discovery mode to " + mode + (mode == DiscoveryMode.DISABLED ? ", forgot all servers" : ""));
	}

	protected static void updateDiscoveryData(InetSocketAddress sender, UnconnectedPong pong) {
		if (!RakNet.isLocalAddress(sender) && !DISCOVERY_ADDRESSES.containsKey(sender)) {
			return; // Not a local server or a registered external server
		}
		boolean external = !RakNet.isLocalAddress(sender);
		if (DISCOVERY_ADDRESSES.containsKey(sender)) {
			external = DISCOVERY_ADDRESSES.get(sender).booleanValue();
		}

		// Update server information
		if (!DISCOVERED.containsKey(sender)) {
			DiscoveredServer discovered = new DiscoveredServer(sender, external, pong.identifier);
			DISCOVERED.put(sender, discovered);
			LOG.info("Discovered local server with address " + sender);
			callEvent(listener -> listener.onServerDiscovered(sender, discovered.isExternal(), pong.identifier));
		} else {
			DiscoveredServer discovered = DISCOVERED.get(sender);
			discovered.setTimestamp(System.currentTimeMillis());
			if (!pong.identifier.equals(discovered.getIdentifier())) {
				discovered.setIdentifier(pong.identifier);
				LOG.debug("Updated local server with address " + sender + " identifier to \"" + pong.identifier + "\"");
				callEvent(listener -> listener.onServerIdentifierUpdate(sender, discovered.isExternal(),
						pong.identifier));
			}
		}
	}

	private Discovery() {
		// Static class
	}

}
