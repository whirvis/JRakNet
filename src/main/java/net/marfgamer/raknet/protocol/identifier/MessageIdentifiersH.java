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
 * Copyright (c) 2016 Trent Summerlin
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
package net.marfgamer.raknet.protocol.identifier;

/**
 * Used to discover all RakNet ID's, their ID corresponds to their index in the
 * enumerator as C++ automatically sets their value to that. This class
 * simulates that and can print out all the ID's or get an ID by it's name. <br>
 * <br>
 * Note: Do not <b>add or delete any values here</b> as it will break the system
 * entirely! Only import from the source code from GitHub
 * 
 * @author Trent Summerlin
 */
public enum MessageIdentifiersH {

	// Reserved Types - Do not change!
	ID_CONNECTED_PING, ID_UNCONNECTED_PING, ID_UNCONNECTED_PING_OPEN_CONNECTIONS, ID_CONNECTED_PONG, ID_DETECT_LOST_CONNECTIONS, ID_OPEN_CONNECTION_REQUEST_1, ID_OPEN_CONNECTION_REPLY_1, ID_OPEN_CONNECTION_REQUEST_2, ID_OPEN_CONNECTION_REPLY_2, ID_CONNECTION_REQUEST, ID_REMOTE_SYSTEM_REQUIRES_PUBLIC_KEY, ID_OUR_SYSTEM_REQUIRES_SECURITY, ID_PUBLIC_KEY_MISMATCH, ID_OUT_OF_BAND_INTERNAL, ID_SND_RECEIPT_ACKED, ID_SND_RECEIPT_LOSS,

	// User types - Do not change!
	ID_CONNECTION_REQUEST_ACCEPTED, ID_CONNECTION_ATTEMPT_FAILED, ID_ALREADY_CONNECTED, ID_NEW_INCOMING_CONNECTION, ID_NO_FREE_INCOMING_CONNECTIONS, ID_DISCONNECTION_NOTIFICATION, ID_CONNECTION_LOST, ID_CONNECTION_BANNED, ID_INVALID_PASSWORD, ID_INCOMPATIBLE_PROTOCOL_VERSION, ID_IP_RECENTLY_CONNECTED, ID_TIMESTAMP, ID_UNCONNECTED_PONG, ID_ADVERTISE_SYSTEM, ID_DOWNLOAD_PROGRESS, ID_REMOTE_DISCONNECTION_NOTIFICATION, ID_REMOTE_CONNECTION_LOST, ID_REMOTE_NEW_INCOMING_CONNECTION,

	// FileListTransfer plugin
	ID_FILE_LIST_TRANSFER_HEADER, ID_FILE_LIST_TRANSFER_FILE, ID_FILE_LIST_REFERENCE_PUSH_ACK,

	/// DirectoryDeltaTransfer plugin
	ID_DDT_DOWNLOAD_REQUEST,

	// Transport plugin
	ID_TRANSPORT_STRING,

	/// ReplicaManager plugin
	ID_REPLICA_MANAGER_CONSTRUCTION, ID_REPLICA_MANAGER_SCOPE_CHANGE, ID_REPLICA_MANAGER_SERIALIZE, ID_REPLICA_MANAGER_DOWNLOAD_STARTED, ID_REPLICA_MANAGER_DOWNLOAD_COMPLETE,

	/// RakVoice plugin
	ID_RAKVOICE_OPEN_CHANNEL_REQUEST, ID_RAKVOICE_OPEN_CHANNEL_REPLY, ID_RAKVOICE_CLOSE_CHANNEL, ID_RAKVOICE_DATA,

	/// AutoPatcher plugin
	ID_AUTOPATCHER_GET_CHANGELIST_SINCE_DATE, ID_AUTOPATCHER_CREATION_LIST, ID_AUTOPATCHER_DELETION_LIST, ID_AUTOPATCHER_GET_PATCH, ID_AUTOPATCHER_PATCH_LIST, ID_AUTOPATCHER_REPOSITORY_FATAL_ERROR, ID_AUTOPATCHER_CANNOT_DOWNLOAD_ORIGINAL_UNMODIFIED_FILES, ID_AUTOPATCHER_FINISHED_INTERNAL, ID_AUTOPATCHER_FINISHED, ID_AUTOPATCHER_RESTART_APPLICATION,

	// NAT messages
	ID_NAT_PUNCHTHROUGH_REQUEST, ID_NAT_CONNECT_AT_TIME, ID_NAT_GET_MOST_RECENT_PORT, ID_NAT_CLIENT_READY, ID_NAT_TARGET_NOT_CONNECTED, ID_NAT_TARGET_UNRESPONSIVE, ID_NAT_CONNECTION_TO_TARGET_LOST, ID_NAT_ALREADY_IN_PROGRESS, ID_NAT_PUNCHTHROUGH_FAILED, ID_NAT_PUNCHTHROUGH_SUCCEEDED,

	// Ready events
	ID_READY_EVENT_SET, ID_READY_EVENT_UNSET, ID_READY_EVENT_ALL_SET, ID_READY_EVENT_QUERY,

	/// Lobby packets. Second byte indicates type.
	ID_LOBBY_GENERAL,

	// RPC3, RPC4 error
	ID_RPC_REMOTE_ERROR,

	/// Plugin based replacement for RPC system
	ID_RPC_PLUGIN,

	// FileListTransfer for transferring large files in chunks
	ID_FILE_LIST_REFERENCE_PUSH,

	/// Force the ready event to all set
	ID_READY_EVENT_FORCE_ALL_SET,

	/// Rooms function
	ID_ROOMS_EXECUTE_FUNC, ID_ROOMS_LOGON_STATUS, ID_ROOMS_HANDLE_CHANGE,

	/// Lobby 2 messages
	ID_LOBBY2_SEND_MESSAGE, ID_LOBBY2_SERVER_ERROR,

	// FCM2 plugin
	ID_FCM2_NEW_HOST, ID_FCM2_REQUEST_FCMGUID, ID_FCM2_RESPOND_CONNECTION_COUNT, ID_FCM2_INFORM_FCMGUID, ID_FCM2_UPDATE_MIN_TOTAL_CONNECTION_COUNT, ID_FCM2_VERIFIED_JOIN_START, ID_FCM2_VERIFIED_JOIN_CAPABLE, ID_FCM2_VERIFIED_JOIN_FAILED, ID_FCM2_VERIFIED_JOIN_ACCEPTED, ID_FCM2_VERIFIED_JOIN_REJECTED,

	/// UDP proxy messages
	ID_UDP_PROXY_GENERAL,

	// MySQL SQLite
	ID_SQLite3_EXEC, ID_SQLite3_UNKNOWN_DB, ID_SQLLITE_LOGGER,

	// NAT detection
	ID_NAT_TYPE_DETECTION_REQUEST, ID_NAT_TYPE_DETECTION_RESULT,

	// Router utilities
	ID_ROUTER_2_INTERNAL, ID_ROUTER_2_FORWARDING_NO_PATH, ID_ROUTER_2_FORWARDING_ESTABLISHED, ID_ROUTER_2_REROUTED,

	/// Internal team balancer
	ID_TEAM_BALANCER_INTERNAL,

	// Team balancer
	ID_TEAM_BALANCER_REQUESTED_TEAM_FULL, ID_TEAM_BALANCER_REQUESTED_TEAM_LOCKED, ID_TEAM_BALANCER_TEAM_REQUESTED_CANCELLED, ID_TEAM_BALANCER_TEAM_ASSIGNED,

	/// Gamebryo Lightspeed integration
	ID_LIGHTSPEED_INTEGRATION,

	/// XBOX integration
	ID_XBOX_LOBBY,

	// Authentication
	ID_TWO_WAY_AUTHENTICATION_INCOMING_CHALLENGE_SUCCESS, ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_SUCCESS, ID_TWO_WAY_AUTHENTICATION_INCOMING_CHALLENGE_FAILURE, ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_FAILURE, ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_TIMEOUT,

	/// Internal
	ID_TWO_WAY_AUTHENTICATION_NEGOTIATION,

	/// Cloud plugin
	ID_CLOUD_POST_REQUEST, ID_CLOUD_RELEASE_REQUEST, ID_CLOUD_GET_REQUEST, ID_CLOUD_GET_RESPONSE, ID_CLOUD_UNSUBSCRIBE_REQUEST, ID_CLOUD_SERVER_TO_SERVER_COMMAND, ID_CLOUD_SUBSCRIPTION_NOTIFICATION,

	// LibVoice
	ID_LIB_VOICE, ID_RELAY_PLUGIN, ID_NAT_REQUEST_BOUND_ADDRESSES, ID_NAT_RESPOND_BOUND_ADDRESSES, ID_FCM2_UPDATE_USER_CONTEXT,

	// Reserved types
	ID_RESERVED_3, ID_RESERVED_4, ID_RESERVED_5, ID_RESERVED_6, ID_RESERVED_7, ID_RESERVED_8, ID_RESERVED_9,

	// For the user to use. Start your first enumeration at this value.
	ID_USER_PACKET_ENUM;

	/**
	 * Prints out all the packet names and their ID
	 * 
	 * @param args
	 *            The program arguments, which should be none you sill goose.
	 */
	public static void main(String[] args) {
		MessageIdentifiersH[] packets = MessageIdentifiersH.values();
		for (int i = 0; i < packets.length; i++) {
			System.out
					.println(packets[i].name() + " = 0x" + (i >= 16 ? "" : "0") + Integer.toHexString(i).toUpperCase());
		}
	}

	/**
	 * Returns a packet's name according to their ID
	 * 
	 * @param id
	 *            The ID of the packet
	 * @return String
	 */
	public static String getPacketName(int id) {
		MessageIdentifiersH[] packets = MessageIdentifiersH.values();
		for (int i = 0; i < packets.length; i++) {
			if (i == id) {
				return packets[i].name();
			}
		}
		return null;
	}

	/**
	 * Returns a packet's ID according to their name
	 * 
	 * @param name
	 *            The name of the packet
	 * @return int
	 */
	public static short getPacketId(String name) {
		MessageIdentifiersH[] packets = MessageIdentifiersH.values();
		for (int i = 0; i < packets.length; i++) {
			if (packets[i].name().equalsIgnoreCase(name)) {
				return (short) i;
			}
		}
		return -1;
	}

	/**
	 * Converts the enumerator to a String array, the packet ID is the index for
	 * the name
	 * 
	 * @return String
	 */
	public static String[] getPacketNames() {
		MessageIdentifiersH[] packets = MessageIdentifiersH.values();
		String[] packetNames = new String[packets.length];
		for (int i = 0; i < packets.length; i++) {
			packetNames[i] = getPacketName(i);
		}
		return packetNames;
	}

}
