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
package com.whirvis.jraknet.discovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.ThreadedListener;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.status.UnconnectedPong;

/**
 * Used to discover servers on the local network and on external networks.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.11.0
 * @see DiscoveryMode
 * @see DiscoveredServer
 * @see DiscoveryListener
 * @see #setDiscoveryMode(DiscoveryMode)
 * @see #addListener(DiscoveryListener)
 */
public final class Discovery {

	/**
	 * The address to broadcast to in order to discover servers on the local
	 * network.
	 */
	private static final String BROADCAST_ADDRESS = "255.255.255.255";

	/**
	 * The server is a server discovered on the local network.
	 */
	private static final boolean LOCAL_SERVER = false;

	/**
	 * The server is a server discovered on an external network.
	 */
	private static final boolean EXTERNAL_SERVER = true;

	/**
	 * The logger used by the discovery system.
	 */
	private static final Logger LOG = LogManager.getLogger(Discovery.class);

	/**
	 * The timestamp used in pings sent by the discovery system.
	 */
	private static final long TIMESTAMP = System.currentTimeMillis();

	/**
	 * The ping ID used in pings sent by the discovery system.
	 */
	private static final long PING_ID = UUID.randomUUID().getLeastSignificantBits();

	protected static DiscoveryMode discoveryMode = DiscoveryMode.ALL_CONNECTIONS;
	protected static int eventThreadCount;
	protected static final ConcurrentLinkedQueue<DiscoveryListener> LISTENERS = new ConcurrentLinkedQueue<DiscoveryListener>();
	protected static final ConcurrentHashMap<InetSocketAddress, Boolean> DISCOVERY_ADDRESSES = new ConcurrentHashMap<InetSocketAddress, Boolean>();
	protected static final ConcurrentHashMap<InetSocketAddress, DiscoveredServer> DISCOVERED = new ConcurrentHashMap<InetSocketAddress, DiscoveredServer>();
	protected static DiscoveryThread thread = null;

	private Discovery() {
		// Static class
	}

	/**
	 * Returns the timestamp of the discovery system.
	 * <p>
	 * The timestamp of the discovery system is how long in milliseconds has
	 * passed since it has been created.
	 * 
	 * @return the timestamp of the discovery system.
	 */
	protected static long getTimestamp() {
		return System.currentTimeMillis() - TIMESTAMP;
	}

	/**
	 * Returns the ping ID of the discovery system.
	 * 
	 * @return the ping ID of the discovery system.
	 */
	protected static long getPingId() {
		return PING_ID;
	}

	/**
	 * Returns the discovery mode.
	 * <p>
	 * The discovery mode determines how server server discovery is handled.
	 * 
	 * @return the discovery mode.
	 */
	public static DiscoveryMode getDiscoveryMode() {
		if (discoveryMode == null) {
			discoveryMode = DiscoveryMode.DISABLED;
		}
		return discoveryMode;
	}

	/**
	 * Sets the discovery mode.
	 * <p>
	 * If disabling the discovery system, all of the currently discovered
	 * servers will be treated as if they had been forgotten. If discovery is
	 * enabled once again later on, all servers listed for discovery via
	 * {@link #addPort(int)} and {@link #addServer(InetSocketAddress)} will be
	 * rediscovered. To stop this from occurring, they can be removed via
	 * {@link #removePort(int)} and {@link #removeServer(DiscoveredServer)}
	 * 
	 * @param mode
	 *            the new discovery mode. A <code>null</code> value will have
	 *            the discovery mode be set to {@link DiscoveryMode#DISABLED}.
	 */
	public static synchronized void setDiscoveryMode(DiscoveryMode mode) {
		discoveryMode = (mode == null ? DiscoveryMode.DISABLED : mode);
		if (discoveryMode == DiscoveryMode.DISABLED) {
			for (InetSocketAddress address : DISCOVERED.keySet()) {
				if (DISCOVERED.containsKey(address)) {
					callEvent(listener -> listener.onServerForgotten(DISCOVERED.get(address)));
				}
			}
			DISCOVERED.clear(); // Forget all servers
		} else if (thread == null && !LISTENERS.isEmpty()) {
			thread = new DiscoveryThread();
			thread.start();
		}
		LOG.debug("Set discovery mode to " + mode + (mode == DiscoveryMode.DISABLED ? ", forgot all servers" : ""));
	}

	/**
	 * Adds a {@link DiscoveryListener} to the discovery system.
	 * <p>
	 * Listeners are used to listen for events that occur relating to the
	 * discovery system such as discovering servers, forgetting servers, etc.
	 * 
	 * @param listener
	 *            the listener to add.
	 * @throws NullPointerException
	 *             if the <code>listener</code> is <code>null</code>.
	 */
	public static synchronized void addListener(DiscoveryListener listener) throws NullPointerException {
		if (listener == null) {
			throw new NullPointerException("Listener cannot be null");
		} else if (!LISTENERS.contains(listener)) {
			LISTENERS.add(listener);
			LOG.debug("Added listener of class " + listener.getClass().getName());
			if (thread == null) {
				thread = new DiscoveryThread();
				thread.start();
			}
		}
	}

	/**
	 * Removes a {@link DiscoveryListener} from the discovery system.
	 * 
	 * @param listener
	 *            the listener to remove.
	 */
	public static void removeListener(DiscoveryListener listener) {
		if (LISTENERS.remove(listener)) {
			LOG.debug("Removed listener of class " + listener.getClass().getName());
		}
	}

	/**
	 * Calls an event.
	 * 
	 * @param event
	 *            the event to call.
	 * @throws NullPointerException
	 *             if the <code>event</code> is <code>null</code>.
	 * @see DiscoveryListener
	 */
	protected static void callEvent(Consumer<? super DiscoveryListener> event) throws NullPointerException {
		if (event == null) {
			throw new NullPointerException("Event cannot be null");
		}
		for (DiscoveryListener listener : LISTENERS) {
			if (listener.getClass().isAnnotationPresent(ThreadedListener.class)) {
				ThreadedListener threadedListener = listener.getClass().getAnnotation(ThreadedListener.class);
				new Thread(Discovery.class.getSimpleName() + (threadedListener.name().length() > 0 ? "-" : "") + threadedListener.name() + "-Thread-" + ++eventThreadCount) {

					@Override
					public void run() {
						event.accept(listener);
					}

				}.start();
			} else {
				event.accept(listener);
			}
		}
	}

	/**
	 * Returns whether or not the specified ports are being broadcasted to.
	 * 
	 * @param ports
	 *            the ports.
	 * @return <code>true</code> if all of the <code>ports</code> are being
	 *         broadcasted to, <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>ports</code> are <code>null</code>.
	 */
	public static boolean hasPorts(int... ports) throws NullPointerException {
		if (ports == null) {
			throw new NullPointerException("Ports cannot be null");
		} else if (ports.length <= 0) {
			return false; // It is impossible to broadcast to zero ports
		}
		for (int i = 0; i < ports.length; i++) {
			for (InetSocketAddress discoveryAddress : DISCOVERY_ADDRESSES.keySet()) {
				if (discoveryAddress.getAddress().getHostAddress().equals(BROADCAST_ADDRESS)) {
					if (discoveryAddress.getPort() == ports[i]) {
						continue;
					}
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns whether or not the specified port is being broadcasted to.
	 * 
	 * @param port
	 *            the port.
	 * @return <code>true</code> if the <code>port</code> is being broadcasted
	 *         to, <code>false</code> otherwise.
	 */
	public static boolean hasPort(int port) {
		return hasPorts(port);
	}

	/**
	 * Returns the ports that are being broadcasted to on the local network.
	 * 
	 * @return the ports that are being broadcasted to on the local network.
	 */
	public static int[] getPorts() {
		ArrayList<Integer> portsBoxed = new ArrayList<Integer>();
		for (InetSocketAddress address : DISCOVERY_ADDRESSES.keySet()) {
			if (DISCOVERY_ADDRESSES.get(address).booleanValue() == LOCAL_SERVER) {
				portsBoxed.add(address.getPort());
			}
		}
		int[] ports = new int[portsBoxed.size()];
		for (int i = 0; i < ports.length; i++) {
			ports[i] = portsBoxed.get(i);
		}
		return ports;
	}

	/**
	 * Starts broadcasting to the specified ports on the local network for
	 * server discovery.
	 * <p>
	 * It is also possible to discovery only certain servers on the local
	 * network via {@link #addServer(InetSocketAddress)} if desired.
	 * 
	 * @param ports
	 *            the ports to start broadcasting to.
	 * @throws IllegalArgumentException
	 *             if one of the ports is not in between <code>0-65535</code>.
	 */
	public static synchronized void addPorts(int... ports) throws IllegalArgumentException {
		for (int port : ports) {
			if (port < 0x0000 || port > 0xFFFF) {
				throw new IllegalArgumentException("Port must be in between 0-65535");
			}
			InetSocketAddress discoveryAddress = new InetSocketAddress(BROADCAST_ADDRESS, port);
			if (DISCOVERY_ADDRESSES.put(discoveryAddress, LOCAL_SERVER) == null) {
				LOG.debug("Added discovery port " + port);
			}
		}
		if (thread == null && !DISCOVERY_ADDRESSES.isEmpty()) {
			thread = new DiscoveryThread();
			thread.start();
		}
	}

	/**
	 * Starts broadcasting to the specified port on the local network for server
	 * discovery.
	 * <p>
	 * It is also possible to discovery only certain servers on the local
	 * network via {@link #addServer(InetSocketAddress)} if desired.
	 * 
	 * @param port
	 *            the port to start broadcasting to.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not within the range of
	 *             <code>0-65535</code>.
	 */
	public static void addPort(int port) throws IllegalArgumentException {
		addPorts(port);
	}

	/**
	 * Stops broadcasting to the specified ports on the local network.
	 * 
	 * @param ports
	 *            the ports to stop broadcasting to.
	 */
	public static void removePorts(int... ports) {
		for (int port : ports) {
			if (port < 0x0000 || port > 0xFFFF) {
				continue; // Invalid port range
			}

			/*
			 * It would makes sense to check if the address is a local address
			 * and not an external address. However, the broadcast address
			 * 255.255.255.255 could never be used for external servers, so it
			 * is not checked for here. As an extra safeguard, the
			 * addExternalServer() method will not allow for an IP address that
			 * is equivalent to that of the broadcast address 255.255.255.255.
			 */
			InetSocketAddress discoveryAddress = new InetSocketAddress(BROADCAST_ADDRESS, port);
			if (DISCOVERY_ADDRESSES.remove(discoveryAddress) != null) {
				LOG.debug("Removed discovery port " + port);
			}
		}
	}

	/**
	 * Stops broadcasting to the specified port on the local network.
	 * 
	 * @param port
	 *            the port to stop broadcasting to.
	 */
	public static void removePort(int port) {
		removePorts(port);
	}

	/**
	 * Stops broadcasting to all ports.
	 * <p>
	 * To stop broadcasting to a specific port, use the {@link #removePort(int)}
	 * method.
	 */
	public static void clearPorts() {
		if (DISCOVERY_ADDRESSES.containsValue(LOCAL_SERVER)) {
			Iterator<InetSocketAddress> addresses = DISCOVERY_ADDRESSES.keySet().iterator();
			while (addresses.hasNext()) {
				InetSocketAddress address = addresses.next();
				boolean type = DISCOVERY_ADDRESSES.get(address).booleanValue();
				if (type == LOCAL_SERVER) {
					addresses.remove();
					DiscoveredServer forgotten = DISCOVERED.remove(address);
					if (forgotten != null) {
						callEvent(listener -> listener.onServerForgotten(forgotten));
					}
				}
			}
			LOG.debug("Cleared discovery ports");
		}
	}

	/**
	 * Sets the ports that will be broadcasted to on the local network.
	 * <p>
	 * This method is simply a shorthand for {@link #addPort(int)} and
	 * {@link #removePort(int)}. The way this method works is by adding all
	 * ports in the <code>ports</code> parameter that are not already up for
	 * discovery, and removing all ports that are up for discovery but not found
	 * in the <code>ports</code> parameter. If the <code>ports</code> parameter
	 * is empty, the {@link #clearPorts()} method will be called instead.
	 * 
	 * @param ports
	 *            the ports to broadcast to.
	 * @throws NullPointerException
	 *             if <code>ports</code> are <code>null</code>.
	 */
	public static void setPorts(int... ports) throws NullPointerException {
		if (ports == null) {
			throw new NullPointerException("Ports cannot be null");
		} else if (ports.length > 0) {
			// Convert to list for use of contains() method
			ArrayList<Integer> portsList = new ArrayList<Integer>();
			for (int port : ports) {
				if (!portsList.contains(port)) {
					portsList.add(port);
				}
			}

			// Add ports in the list that are not up for discovery
			for (Integer port : portsList) {
				if (!hasPort(port.intValue())) {
					addPort(port.intValue());
				}
			}

			// Remove ports that are up for discovery but not in the list
			for (int port : getPorts()) {
				if (!portsList.contains(port)) {
					removePort(port);
				}
			}
		} else {
			clearPorts();
		}
	}

	/**
	 * Sets the port that will be broadcasted to on the local network.
	 * <p>
	 * This method is simply a shorthand for {@link #clearPorts()} and
	 * {@link #addPort(int)}. The way this method works is simply by clearing
	 * all the current ports and then adding the specified port.
	 * 
	 * @param port
	 *            the port to broadcast to.
	 * @throws IllegalArgumentException
	 *             if the port is not within the range of <code>0-65535</code>.
	 */
	public static void setPort(int port) throws IllegalArgumentException {
		clearPorts();
		addPort(port);
	}

	/**
	 * Returns whether or not the specified servers are being broadcasted to.
	 * 
	 * @param servers
	 *            the servers
	 * @return <code>true</code> if all of the <code>servers</code> are being
	 *         broadcasted to, <code>false</code> otherwise.
	 * @throws NullPointerException
	 *             if the <code>servers</code> are <code>null</code>.
	 */
	public static boolean hasServers(InetSocketAddress... servers) throws NullPointerException {
		if (servers == null) {
			throw new NullPointerException("Servers cannot be null");
		} else if (servers.length <= 0) {
			return false; // It is impossible to broadcast to zero servers
		}
		for (int i = 0; i < servers.length; i++) {
			for (InetSocketAddress discoveryAddress : DISCOVERY_ADDRESSES.keySet()) {
				if (discoveryAddress.getAddress().equals(servers[i])) {
					continue;
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns whether or not the specified server is being broadcasted to.
	 * 
	 * @param server
	 *            the server.
	 * @return <code>true</code> if the <code>server</code> is being broadcasted
	 *         to, <code>false</code> otherwise.
	 */
	public static boolean hasServer(InetSocketAddress server) {
		return hasServers(server);
	}

	/**
	 * Returns the external servers that are being broadcasted to.
	 * 
	 * @return the external servers that are being broadcasted to.
	 */
	public static InetSocketAddress[] getServers() {
		ArrayList<InetSocketAddress> external = new ArrayList<InetSocketAddress>();
		for (InetSocketAddress address : DISCOVERY_ADDRESSES.keySet()) {
			if (DISCOVERY_ADDRESSES.get(address).booleanValue() == EXTERNAL_SERVER) {
				external.add(address);
			}
		}
		return external.toArray(new InetSocketAddress[external.size()]);
	}

	/**
	 * Starts broadcasting to the specified server address for server discovery.
	 * <p>
	 * This allows for the discovery of servers on external networks. If
	 * discovering servers on the local network, it is possible to discover all
	 * servers running on a specified port via the {@link #addPort(int)} method.
	 * 
	 * @param address
	 *            the server address.
	 * @throws NullPointerException
	 *             if the <code>address</code> or the IP address of the
	 *             <code>address</code> are <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the IP address of <code>address</code> is the broadcast
	 *             address of {@value #BROADCAST_ADDRESS}.
	 */
	public static synchronized void addServer(InetSocketAddress address) throws NullPointerException, IllegalArgumentException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannnot be null");
		} else if (BROADCAST_ADDRESS.equals(address.getAddress().getHostAddress())) {
			throw new IllegalArgumentException("IP address cannot be broadcast address " + BROADCAST_ADDRESS);
		}
		if (DISCOVERY_ADDRESSES.put(address, EXTERNAL_SERVER) == null) {
			LOG.debug("Added external server with address " + address + " for discovery");
		}
		if (thread == null) {
			thread = new DiscoveryThread();
			thread.start();
		}
	}

	/**
	 * Starts broadcasting to the specified server address for server discovery.
	 * <p>
	 * This allows for the discovery of servers on external networks. If
	 * discovering on the local network, it is possible to discover all servers
	 * running on a specified port via the {@link #addPort(int)} method.
	 * 
	 * @param address
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not in between
	 *             <code>0-65535</code> or the <code>address</code> is the
	 *             broadcast address of {@value #BROADCAST_ADDRESS}.
	 */
	public static void addServer(InetAddress address, int port) throws NullPointerException, IllegalArgumentException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		} else if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Port must be in between 0-65535");
		}
		addServer(new InetSocketAddress(address, port));
	}

	/**
	 * Starts broadcasting to the specified server address for server discovery.
	 * <p>
	 * This allows for the discovery of servers on external networks. If
	 * discovering on the local network, it is possible to discover all servers
	 * running on a specified port via the {@link #addPort(int)} method.
	 * 
	 * @param host
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not in between
	 *             <code>0-65535</code> or the <code>host</code> is the
	 *             broadcast address of {@value #BROADCAST_ADDRESS}.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public static void addServer(String host, int port) throws NullPointerException, IllegalArgumentException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		addServer(InetAddress.getByName(host), port);
	}

	/**
	 * Stops broadcasting to the specified server address.
	 * 
	 * @param address
	 *            the server address.
	 * @throws IllegalArgumentException
	 *             if the address is not that of an external server.
	 */
	public static void removeServer(InetSocketAddress address) throws IllegalArgumentException {
		if (address == null) {
			return; // No address
		} else if (address.getAddress() == null) {
			return; // No IP address
		} else if (!DISCOVERY_ADDRESSES.containsKey(address)) {
			return; // No address to remove
		} else if (DISCOVERY_ADDRESSES.get(address).booleanValue() != EXTERNAL_SERVER) {
			throw new IllegalArgumentException("Address must be that of an external server");
		} else if (DISCOVERY_ADDRESSES.remove(address) != null) {
			DiscoveredServer forgotten = DISCOVERED.remove(address);
			if (forgotten != null) {
				callEvent(listener -> listener.onServerForgotten(forgotten));
			}
			LOG.debug("Removed external server with address " + address + " from discovery");
		}
	}

	/**
	 * Stops broadcasting to the specified server address.
	 * 
	 * @param address
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 */
	public static void removeServer(InetAddress address, int port) {
		if (address != null && port >= 0x0000 && port <= 0xFFFF) {
			removeServer(new InetSocketAddress(address, port));
		}
	}

	/**
	 * Stops broadcasting to the specified server address.
	 * 
	 * @param host
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public static void removeServer(String host, int port) throws UnknownHostException {
		if (host != null) {
			removeServer(InetAddress.getByName(host), port);
		}
	}

	/**
	 * Stops broadcasting to the specified discovered server.
	 * 
	 * @param server
	 *            the discovered server.
	 * @throws IllegalArgumentException
	 *             if the <code>server</code> is not an external server.
	 */
	public static void removeServer(DiscoveredServer server) throws IllegalArgumentException {
		if (!server.isExternal()) {
			throw new IllegalArgumentException("Discovered server must be an external server");
		}
		removeServer(server.getAddress());
	}

	/**
	 * Sets the servers that will be broadcasted to.
	 * <p>
	 * This method is simply a shorthand for
	 * {@link #addServer(InetSocketAddress)} and
	 * {@link #removeServer(InetSocketAddress)}. The way this method works is by
	 * adding all servers in the <code>servers</code> parameter that are not
	 * already up for discovery, and removing all servers that are up for
	 * discovery but not found in the <code>servers</code> parameter. If the
	 * <code>ports</code> parameter is empty, the {@link #clearServers()} method
	 * will be called instead.
	 * 
	 * @param servers
	 *            the servers to broadcast to.
	 * @throws NullPointerException
	 *             if <code>servers</code> are <code>null</code>.
	 */
	public static void setServers(InetSocketAddress... servers) throws NullPointerException {
		if (servers == null) {
			throw new NullPointerException("Servers cannot be null");
		} else if (servers.length > 0) {
			// Convert to list for use of contains() method
			ArrayList<InetSocketAddress> serversList = new ArrayList<InetSocketAddress>();
			for (InetSocketAddress server : servers) {
				if (!serversList.contains(server)) {
					serversList.add(server);
				}
			}

			// Add servers in the list that are not up for discovery
			for (InetSocketAddress server : serversList) {
				if (!hasServer(server)) {
					addServer(server);
				}
			}

			// Remove servers that are up for discovery but not in the list
			for (InetSocketAddress server : getServers()) {
				if (!serversList.contains(server)) {
					removeServer(server);
				}
			}
		} else {
			clearServers();
		}
	}

	/**
	 * Sets the server that will be broadcasted to.
	 * <p>
	 * This method is simply a shorthand for {@link #clearServers()} and
	 * {@link #addServer(InetSocketAddress)}. The way this method works is
	 * simply by clearing all the current ports and then adding the specified.
	 * 
	 * @param server
	 *            the server to broadcast to.
	 * @throws NullPointerException
	 *             if the <code>server</code> or the <code>server</code> IP
	 *             address are <code>null</code>.
	 */
	public static void setServer(InetSocketAddress server) throws NullPointerException {
		if (server == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (server.getAddress() == null) {
			throw new NullPointerException("IP address cannot be null");
		}
		clearServers();
		addServer(server);
	}

	/**
	 * Sets the server that will be broadcasted to.
	 * <p>
	 * This method is simply a shorthand for {@link #clearServers()} and
	 * {@link #addServer(InetSocketAddress)}. The way this method works is
	 * simply by clearing all the current ports and then adding the specified.
	 * 
	 * @param address
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not in between
	 *             <code>0-65535</code>.
	 */
	public static void setServer(InetAddress address, int port) throws NullPointerException, IllegalArgumentException {
		clearServers();
		addServer(address, port);
	}

	/**
	 * Sets the server that will be broadcasted to.
	 * <p>
	 * This method is simply a shorthand for {@link #clearServers()} and
	 * {@link #addServer(InetSocketAddress)}. The way this method works is
	 * simply by clearing all the current ports and then adding the specified.
	 * 
	 * @param host
	 *            the server address.
	 * @param port
	 *            the server port.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not in between
	 *             <code>0-65535</code>.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public static void setServer(String host, int port) throws NullPointerException, IllegalArgumentException, UnknownHostException {
		clearServers();
		addServer(host, port);
	}

	/**
	 * Stops broadcasting to all server addresses. To stop broadcasting to a
	 * specific server address, use the {@link #removeServer(InetSocketAddress)}
	 * method.
	 */
	public static void clearServers() {
		if (DISCOVERY_ADDRESSES.containsValue(EXTERNAL_SERVER)) {
			Iterator<InetSocketAddress> addresses = DISCOVERY_ADDRESSES.keySet().iterator();
			while (addresses.hasNext()) {
				InetSocketAddress address = addresses.next();
				boolean type = DISCOVERY_ADDRESSES.get(address).booleanValue();
				if (type == EXTERNAL_SERVER) {
					addresses.remove();
					DiscoveredServer forgotten = DISCOVERED.remove(address);
					if (forgotten != null) {
						callEvent(listener -> listener.onServerForgotten(forgotten));
					}
				}
			}
			LOG.debug("Cleared external servers from discovery");
		}
	}

	/**
	 * Returns the discovered servers, both local and external.
	 * 
	 * @return the discovered servers, both local and external.
	 * @see #getLocal()
	 * @see #getExternal()
	 */
	public static DiscoveredServer[] getDiscovered() {
		return DISCOVERED.values().toArray(new DiscoveredServer[DISCOVERED.size()]);
	}

	/**
	 * Returns the locally discovered servers.
	 * 
	 * @return the locally discovered servers.
	 */
	public static DiscoveredServer[] getLocal() {
		ArrayList<DiscoveredServer> local = new ArrayList<DiscoveredServer>();
		for (DiscoveredServer server : DISCOVERED.values()) {
			if (!server.isExternal()) {
				local.add(server);
			}
		}
		return local.toArray(new DiscoveredServer[local.size()]);
	}

	/**
	 * Returns the externally discovered servers.
	 * 
	 * @return the externally discovered servers.
	 */
	public static DiscoveredServer[] getExternal() {
		ArrayList<DiscoveredServer> external = new ArrayList<DiscoveredServer>();
		for (DiscoveredServer server : DISCOVERED.values()) {
			if (server.isExternal()) {
				external.add(server);
			}
		}
		return external.toArray(new DiscoveredServer[external.size()]);
	}

	/**
	 * Updates discovery information for the server with the specified address.
	 * 
	 * @param sender
	 *            the server address.
	 * @param pong
	 *            the decoded unconnected pong packet.
	 * @throws NullPointerException
	 *             if the <code>sender</code> is <code>null</code> or the IP
	 *             address of the <code>sender</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>pong</code> packet failed to decode.
	 */
	protected static synchronized void updateDiscoveryData(InetSocketAddress sender, UnconnectedPong pong) throws NullPointerException, IllegalArgumentException {
		if (sender == null) {
			throw new NullPointerException("Sender cannot be null");
		} else if (sender.getAddress() == null) {
			throw new NullPointerException("Sender IP address cannot be null");
		} else if (pong.failed()) {
			throw new IllegalArgumentException("Unconnected pong failed to decode");
		} else if (RakNet.isLocalAddress(sender) || DISCOVERY_ADDRESSES.containsKey(sender)) {
			boolean external = !RakNet.isLocalAddress(sender);
			if (DISCOVERY_ADDRESSES.containsKey(sender)) {
				external = DISCOVERY_ADDRESSES.get(sender).booleanValue();
			}

			// Update server information
			if (!DISCOVERED.containsKey(sender)) {
				DiscoveredServer discovered = new DiscoveredServer(sender, external, pong.identifier);
				DISCOVERED.put(sender, discovered);
				LOG.info("Discovered " + (external ? "external" : "local") + " with address " + sender);
				callEvent(listener -> listener.onServerDiscovered(discovered));
			} else {
				DiscoveredServer discovered = DISCOVERED.get(sender);
				discovered.setTimestamp(System.currentTimeMillis());
				if (!pong.identifier.equals(discovered.getIdentifier())) {
					Identifier oldIdentifier = discovered.getIdentifier();
					discovered.setIdentifier(pong.identifier);
					LOG.debug("Updated local server with address " + sender + " identifier to \"" + pong.identifier + "\"");
					callEvent(listener -> listener.onServerIdentifierUpdate(discovered, oldIdentifier));
				}
			}
		}
	}

}
