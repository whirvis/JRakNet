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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet.protocol;

import java.lang.reflect.Field;

/**
 * Contains all of the packet IDs for RakNet.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class MessageIdentifier {

	public final static byte[] MAGIC = new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE,
			(byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0x12,
			(byte) 0x34, (byte) 0x56, (byte) 0x78 };

	public static final short ID_CONNECTED_PING = 0x00;
	public static final short ID_UNCONNECTED_PING = 0x01;
	public static final short ID_UNCONNECTED_PING_OPEN_CONNECTIONS = 0x02;
	public static final short ID_CONNECTED_PONG = 0x03;
	public static final short ID_DETECT_LOST_CONNECTIONS = 0x04;
	public static final short ID_OPEN_CONNECTION_REQUEST_1 = 0x05;
	public static final short ID_OPEN_CONNECTION_REPLY_1 = 0x06;
	public static final short ID_OPEN_CONNECTION_REQUEST_2 = 0x07;
	public static final short ID_OPEN_CONNECTION_REPLY_2 = 0x08;
	public static final short ID_CONNECTION_REQUEST = 0x09;
	public static final short ID_REMOTE_SYSTEM_REQUIRES_PUBLIC_KEY = 0x0A;
	public static final short ID_OUR_SYSTEM_REQUIRES_SECURITY = 0x0B;
	public static final short ID_PUBLIC_KEY_MISMATCH = 0x0C;
	public static final short ID_OUT_OF_BAND_INTERNAL = 0x0D;
	public static final short ID_SND_RECEIPT_ACKED = 0x0E;
	public static final short ID_SND_RECEIPT_LOSS = 0x0F;
	public static final short ID_CONNECTION_REQUEST_ACCEPTED = 0x10;
	public static final short ID_CONNECTION_ATTEMPT_FAILED = 0x11;
	public static final short ID_ALREADY_CONNECTED = 0x12;
	public static final short ID_NEW_INCOMING_CONNECTION = 0x13;
	public static final short ID_NO_FREE_INCOMING_CONNECTIONS = 0x14;
	public static final short ID_DISCONNECTION_NOTIFICATION = 0x15;
	public static final short ID_CONNECTION_LOST = 0x16;
	public static final short ID_CONNECTION_BANNED = 0x17;
	public static final short ID_INVALID_PASSWORD = 0x18;
	public static final short ID_INCOMPATIBLE_PROTOCOL_VERSION = 0x19;
	public static final short ID_IP_RECENTLY_CONNECTED = 0x1A;
	public static final short ID_TIMESTAMP = 0x1B;
	public static final short ID_UNCONNECTED_PONG = 0x1C;
	public static final short ID_ADVERTISE_SYSTEM = 0x1D;
	public static final short ID_DOWNLOAD_PROGRESS = 0x1E;
	public static final short ID_REMOTE_DISCONNECTION_NOTIFICATION = 0x1F;
	public static final short ID_REMOTE_CONNECTION_LOST = 0x20;
	public static final short ID_REMOTE_NEW_INCOMING_CONNECTION = 0x21;
	public static final short ID_FILE_LIST_TRANSFER_HEADER = 0x22;
	public static final short ID_FILE_LIST_TRANSFER_FILE = 0x23;
	public static final short ID_FILE_LIST_REFERENCE_PUSH_ACK = 0x24;
	public static final short ID_DDT_DOWNLOAD_REQUEST = 0x25;
	public static final short ID_TRANSPORT_STRING = 0x26;
	public static final short ID_REPLICA_MANAGER_CONSTRUCTION = 0x27;
	public static final short ID_REPLICA_MANAGER_SCOPE_CHANGE = 0x28;
	public static final short ID_REPLICA_MANAGER_SERIALIZE = 0x29;
	public static final short ID_REPLICA_MANAGER_DOWNLOAD_STARTED = 0x2A;
	public static final short ID_REPLICA_MANAGER_DOWNLOAD_COMPLETE = 0x2B;
	public static final short ID_RAKVOICE_OPEN_CHANNEL_REQUEST = 0x2C;
	public static final short ID_RAKVOICE_OPEN_CHANNEL_REPLY = 0x2D;
	public static final short ID_RAKVOICE_CLOSE_CHANNEL = 0x2E;
	public static final short ID_RAKVOICE_DATA = 0x2F;
	public static final short ID_AUTOPATCHER_GET_CHANGELIST_SINCE_DATE = 0x30;
	public static final short ID_AUTOPATCHER_CREATION_LIST = 0x31;
	public static final short ID_AUTOPATCHER_DELETION_LIST = 0x32;
	public static final short ID_AUTOPATCHER_GET_PATCH = 0x33;
	public static final short ID_AUTOPATCHER_PATCH_LIST = 0x34;
	public static final short ID_AUTOPATCHER_REPOSITORY_FATAL_ERROR = 0x35;
	public static final short ID_AUTOPATCHER_CANNOT_DOWNLOAD_ORIGINAL_UNMODIFIED_FILES = 0x36;
	public static final short ID_AUTOPATCHER_FINISHED_INTERNAL = 0x37;
	public static final short ID_AUTOPATCHER_FINISHED = 0x38;
	public static final short ID_AUTOPATCHER_RESTART_APPLICATION = 0x39;
	public static final short ID_NAT_PUNCHTHROUGH_REQUEST = 0x3A;
	public static final short ID_NAT_CONNECT_AT_TIME = 0x3B;
	public static final short ID_NAT_GET_MOST_RECENT_PORT = 0x3C;
	public static final short ID_NAT_CLIENT_READY = 0x3D;
	public static final short ID_NAT_TARGET_NOT_CONNECTED = 0x3E;
	public static final short ID_NAT_TARGET_UNRESPONSIVE = 0x3F;
	public static final short ID_NAT_CONNECTION_TO_TARGET_LOST = 0x40;
	public static final short ID_NAT_ALREADY_IN_PROGRESS = 0x41;
	public static final short ID_NAT_PUNCHTHROUGH_FAILED = 0x42;
	public static final short ID_NAT_PUNCHTHROUGH_SUCCEEDED = 0x43;
	public static final short ID_READY_EVENT_SET = 0x44;
	public static final short ID_READY_EVENT_UNSET = 0x45;
	public static final short ID_READY_EVENT_ALL_SET = 0x46;
	public static final short ID_READY_EVENT_QUERY = 0x47;
	public static final short ID_LOBBY_GENERAL = 0x48;
	public static final short ID_RPC_REMOTE_ERROR = 0x49;
	public static final short ID_RPC_PLUGIN = 0x4A;
	public static final short ID_FILE_LIST_REFERENCE_PUSH = 0x4B;
	public static final short ID_READY_EVENT_FORCE_ALL_SET = 0x4C;
	public static final short ID_ROOMS_EXECUTE_FUNC = 0x4D;
	public static final short ID_ROOMS_LOGON_STATUS = 0x4E;
	public static final short ID_ROOMS_HANDLE_CHANGE = 0x4F;
	public static final short ID_LOBBY2_SEND_MESSAGE = 0x50;
	public static final short ID_LOBBY2_SERVER_ERROR = 0x51;
	public static final short ID_FCM2_NEW_HOST = 0x52;
	public static final short ID_FCM2_REQUEST_FCMGUID = 0x53;
	public static final short ID_FCM2_RESPOND_CONNECTION_COUNT = 0x54;
	public static final short ID_FCM2_INFORM_FCMGUID = 0x55;
	public static final short ID_FCM2_UPDATE_MIN_TOTAL_CONNECTION_COUNT = 0x56;
	public static final short ID_FCM2_VERIFIED_JOIN_START = 0x57;
	public static final short ID_FCM2_VERIFIED_JOIN_CAPABLE = 0x58;
	public static final short ID_FCM2_VERIFIED_JOIN_FAILED = 0x59;
	public static final short ID_FCM2_VERIFIED_JOIN_ACCEPTED = 0x5A;
	public static final short ID_FCM2_VERIFIED_JOIN_REJECTED = 0x5B;
	public static final short ID_UDP_PROXY_GENERAL = 0x5C;
	public static final short ID_SQLite3_EXEC = 0x5D;
	public static final short ID_SQLite3_UNKNOWN_DB = 0x5E;
	public static final short ID_SQLLITE_LOGGER = 0x5F;
	public static final short ID_NAT_TYPE_DETECTION_REQUEST = 0x60;
	public static final short ID_NAT_TYPE_DETECTION_RESULT = 0x61;
	public static final short ID_ROUTER_2_INTERNAL = 0x62;
	public static final short ID_ROUTER_2_FORWARDING_NO_PATH = 0x63;
	public static final short ID_ROUTER_2_FORWARDING_ESTABLISHED = 0x64;
	public static final short ID_ROUTER_2_REROUTED = 0x65;
	public static final short ID_TEAM_BALANCER_INTERNAL = 0x66;
	public static final short ID_TEAM_BALANCER_REQUESTED_TEAM_FULL = 0x67;
	public static final short ID_TEAM_BALANCER_REQUESTED_TEAM_LOCKED = 0x68;
	public static final short ID_TEAM_BALANCER_TEAM_REQUESTED_CANCELLED = 0x69;
	public static final short ID_TEAM_BALANCER_TEAM_ASSIGNED = 0x6A;
	public static final short ID_LIGHTSPEED_INTEGRATION = 0x6B;
	public static final short ID_XBOX_LOBBY = 0x6C;
	public static final short ID_TWO_WAY_AUTHENTICATION_INCOMING_CHALLENGE_SUCCESS = 0x6D;
	public static final short ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_SUCCESS = 0x6E;
	public static final short ID_TWO_WAY_AUTHENTICATION_INCOMING_CHALLENGE_FAILURE = 0x6F;
	public static final short ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_FAILURE = 0x70;
	public static final short ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_TIMEOUT = 0x71;
	public static final short ID_TWO_WAY_AUTHENTICATION_NEGOTIATION = 0x72;
	public static final short ID_CLOUD_POST_REQUEST = 0x73;
	public static final short ID_CLOUD_RELEASE_REQUEST = 0x74;
	public static final short ID_CLOUD_GET_REQUEST = 0x75;
	public static final short ID_CLOUD_GET_RESPONSE = 0x76;
	public static final short ID_CLOUD_UNSUBSCRIBE_REQUEST = 0x77;
	public static final short ID_CLOUD_SERVER_TO_SERVER_COMMAND = 0x78;
	public static final short ID_CLOUD_SUBSCRIPTION_NOTIFICATION = 0x79;
	public static final short ID_LIB_VOICE = 0x7A;
	public static final short ID_RELAY_PLUGIN = 0x7B;
	public static final short ID_NAT_REQUEST_BOUND_ADDRESSES = 0x7C;
	public static final short ID_NAT_RESPOND_BOUND_ADDRESSES = 0x7D;
	public static final short ID_FCM2_UPDATE_USER_CONTEXT = 0x7E;
	public static final short ID_RESERVED_3 = 0x7F;
	public static final short ID_RESERVED_4 = 0x80;
	public static final short ID_RESERVED_5 = 0x81;
	public static final short ID_RESERVED_6 = 0x82;
	public static final short ID_RESERVED_7 = 0x83;
	public static final short ID_RESERVED_8 = 0x84;
	public static final short ID_RESERVED_9 = 0x85;
	public static final short ID_USER_PACKET_ENUM = 0x86;

	public static final short ID_CUSTOM_0 = 0x80;
	public static final short ID_CUSTOM_1 = 0x81;
	public static final short ID_CUSTOM_2 = 0x82;
	public static final short ID_CUSTOM_3 = 0x83;
	public static final short ID_CUSTOM_4 = 0x84;
	public static final short ID_CUSTOM_5 = 0x85;
	public static final short ID_CUSTOM_6 = 0x86;
	public static final short ID_CUSTOM_7 = 0x87;
	public static final short ID_CUSTOM_8 = 0x88;
	public static final short ID_CUSTOM_9 = 0x89;
	public static final short ID_CUSTOM_A = 0x8A;
	public static final short ID_CUSTOM_B = 0x8B;
	public static final short ID_CUSTOM_C = 0x8C;
	public static final short ID_CUSTOM_D = 0x8D;
	public static final short ID_CUSTOM_E = 0x8E;
	public static final short ID_CUSTOM_F = 0x8F;

	/**
	 * Return the packet's name based on it's ID.
	 * 
	 * @param id
	 *            the ID of the packet.
	 * @return the packet's name based on it's ID.
	 */
	public static String getName(int id) {
		for (Field field : MessageIdentifier.class.getDeclaredFields()) {
			if (field.getType().equals(short.class)) {
				try {
					short packetId = field.getShort(null);
					if (packetId == id) {
						return field.getName();
					}
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * Returns the packet's ID based on it's name.
	 * 
	 * @param name
	 *            the name of the packet.
	 * @return the packet's ID based on it's name.
	 */
	public static int getId(String name) {
		try {
			return MessageIdentifier.class.getDeclaredField(name.toUpperCase()).getInt(null);
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Returns whether or not a packet with the specified ID exists.
	 * 
	 * @param id
	 *            the ID of the packet to check for.
	 * @return whether or not a packet with the specified ID exists.
	 */
	public static boolean hasPacket(int id) {
		return getName(id) != null;
	}

	/**
	 * Returns whether or not a packet with the specified name exists.
	 * 
	 * @param name
	 *            the name of the packet to check for.
	 * @return whether or not a packet with the specified name exists.
	 */
	public static boolean hasPacket(String name) {
		return getId(name) >= 0;
	}

}
