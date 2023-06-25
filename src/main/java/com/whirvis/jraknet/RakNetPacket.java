/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 "Whirvis" Trent Summerlin
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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.map.ShortMap;
import com.whirvis.jraknet.protocol.ConnectionType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;

/**
 * A generic RakNet packet that has the ability to get the ID of the packet
 * along with encoding and decoding.
 *
 * @author "Whirvis" Trent Summerlin
 * @since JRakNet v1.0.0
 */
public class RakNetPacket extends Packet {

	/**
	 * The name of the <code>encode()</code> method.
	 */
	private static final String ENCODE_METHOD_NAME = "encode";

	/**
	 * The name of the <code>decode()</code> method.
	 */
	private static final String DECODE_METHOD_NAME = "decode";

	/**
	 * The cached packet names, mapped by their ID.
	 */
	private static final ShortMap<String> PACKET_NAMES = new ShortMap<>();

	/**
	 * The cached packet IDs, mapped by their name.
	 */
	private static final HashMap<String, Short> PACKET_IDS = new HashMap<>();

	/**
	 * The magic identifier.
	 */
	public final static byte[] MAGIC = new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE,
			(byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0x12,
			(byte) 0x34, (byte) 0x56, (byte) 0x78 };

	/**
	 * The ID of the {@link com.whirvis.jraknet.protocol.status.ConnectedPing
	 * CONNECTED_PING} packet.
	 */
	public static final short ID_CONNECTED_PING = 0x00;

	/**
	 * The ID of the {@link com.whirvis.jraknet.protocol.status.UnconnectedPing
	 * UNCONNECTED_PING} packet.
	 */
	public static final short ID_UNCONNECTED_PING = 0x01;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.status.UnconnectedPingOpenConnections
	 * UNCONNECTED_PING_OPEN_CONNECTIONS} packet.
	 */
	public static final short ID_UNCONNECTED_PING_OPEN_CONNECTIONS = 0x02;

	/**
	 * The ID of the {@link com.whirvis.jraknet.protocol.status.ConnectedPong
	 * CONNECTED_PONG} packet.
	 */
	public static final short ID_CONNECTED_PONG = 0x03;

	/**
	 * The ID of the <code>DETECT_LOST_CONNECTIONS</code> packet.
	 */
	public static final short ID_DETECT_LOST_CONNECTIONS = 0x04;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.connection.OpenConnectionRequestOne
	 * OPEN_CONNECTION_REQUEST_1} packet.
	 */
	public static final short ID_OPEN_CONNECTION_REQUEST_1 = 0x05;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.connection.OpenConnectionResponseOne
	 * OPEN_CONNECTION_REPLY_1} packet.
	 */
	public static final short ID_OPEN_CONNECTION_REPLY_1 = 0x06;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.connection.OpenConnectionRequestTwo
	 * OPEN_CONNECTION_REQUEST_2} packet.
	 */
	public static final short ID_OPEN_CONNECTION_REQUEST_2 = 0x07;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.connection.OpenConnectionResponseTwo
	 * OPEN_CONNECTION_REPLY_2} packet.
	 */
	public static final short ID_OPEN_CONNECTION_REPLY_2 = 0x08;

	/**
	 * The ID of the {@link com.whirvis.jraknet.protocol.login.ConnectionRequest
	 * CONNECTION_REQUEST} packet.
	 */
	public static final short ID_CONNECTION_REQUEST = 0x09;

	/**
	 * The ID of the <code>REMOVE_SYSTEM_REQUIRES_PUBLIC_KEY</code> packet.
	 */
	public static final short ID_REMOTE_SYSTEM_REQUIRES_PUBLIC_KEY = 0x0A;

	/**
	 * The ID of the <code>OUR_SYSTEM_REQUIRES_SECURITY</code> packet.
	 */
	public static final short ID_OUR_SYSTEM_REQUIRES_SECURITY = 0x0B;

	/**
	 * The ID of the <code>PUBLIC_KEY_MISMATCH</code> packet.
	 */
	public static final short ID_PUBLIC_KEY_MISMATCH = 0x0C;

	/**
	 * The ID of the <code>OUT_OF_BAND_INTERNAL</code> packet.
	 */
	public static final short ID_OUT_OF_BAND_INTERNAL = 0x0D;

	/**
	 * The ID of the <code>SND_RECEIPT_ACKED</code> packet.
	 * <p>
	 * In the original implementation of RakNet, when a packet is acknowledged
	 * by a peer this packet is sent back through loopback to the original
	 * sender of the packet with the acknoweldgeable reliability. Since this
	 * implementation has listeners will special built in acknowledgement and
	 * loss methods, this packet has no need for implementation.
	 */
	public static final short ID_SND_RECEIPT_ACKED = 0x0E;

	/**
	 * The ID of the <code>SND_RECEIPT_LOSS</code> packet.
	 * <p>
	 * In the original implementation of RakNet, when a packet is acknowledged
	 * by a peer this packet is sent back through loopback to the original
	 * sender of the packet with the acknoweldgeable reliability. Since this
	 * implementation has listeners will special built in acknowledgement and
	 * loss methods, this packet has no need for implementation.
	 */
	public static final short ID_SND_RECEIPT_LOSS = 0x0F;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.login.ConnectionRequestAccepted
	 * CONNECTION_REQUEST_ACCEPTED} packet.
	 */
	public static final short ID_CONNECTION_REQUEST_ACCEPTED = 0x10;

	/**
	 * The ID of the <code>CONNECTION_ATTEMPT_FAILED</code> packet.
	 */
	public static final short ID_CONNECTION_ATTEMPT_FAILED = 0x11;

	/**
	 * The ID of the <code>ALREADY_CONNECTED</code> packet.
	 */
	public static final short ID_ALREADY_CONNECTED = 0x12;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.login.NewIncomingConnection
	 * NEW_INCOMING_CONNECTION} packet.
	 */
	public static final short ID_NEW_INCOMING_CONNECTION = 0x13;

	/**
	 * The ID of the <code>NO_FREE_INCOMING_CONNECTIONS</code> packet.
	 */
	public static final short ID_NO_FREE_INCOMING_CONNECTIONS = 0x14;

	/**
	 * The ID of the <code>DISCONNECTION_NOTIFICATION</code> packet.
	 */
	public static final short ID_DISCONNECTION_NOTIFICATION = 0x15;

	/**
	 * The ID of the <code>CONNECTION_LOST</code> packet.
	 */
	public static final short ID_CONNECTION_LOST = 0x16;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.connection.ConnectionBanned
	 * CONNECTION_BANNED} packet.
	 */
	public static final short ID_CONNECTION_BANNED = 0x17;

	/**
	 * The ID of the <code>INVALID_PASSWORD</code> packet.
	 */
	public static final short ID_INVALID_PASSWORD = 0x18;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.connection.IncompatibleProtocolVersion
	 * INCOMPATIBLE_PROTOCOL_VERSION} packet.
	 */
	public static final short ID_INCOMPATIBLE_PROTOCOL_VERSION = 0x19;

	/**
	 * The ID of the <code>IP_RECENTLY_CONNECTED</code> packet.
	 */
	public static final short ID_IP_RECENTLY_CONNECTED = 0x1A;

	/**
	 * The ID of the <code>TIMESTAMP</code> packet.
	 */
	public static final short ID_TIMESTAMP = 0x1B;

	/**
	 * The ID of the {@link com.whirvis.jraknet.protocol.status.UnconnectedPong
	 * UNCONNECTED_PONG} packet.
	 */
	public static final short ID_UNCONNECTED_PONG = 0x1C;

	/**
	 * The ID of the <code>ADVERTISE_SYSTEM</code> packet.
	 */
	public static final short ID_ADVERTISE_SYSTEM = 0x1D;

	/**
	 * The ID of the <code>DOWNLOAD_PROGRESS</code> packet.
	 */
	public static final short ID_DOWNLOAD_PROGRESS = 0x1E;

	/**
	 * The ID of the <code>REMOTE_DISCONNECTION_NOTIFICATION</code> packet.
	 */
	public static final short ID_REMOTE_DISCONNECTION_NOTIFICATION = 0x1F;

	/**
	 * The ID of the <code>REMOTE_CONNECTION_LOST</code> packet.
	 */
	public static final short ID_REMOTE_CONNECTION_LOST = 0x20;

	/**
	 * The ID of the <code>REMOTE_NEW_INCOMING_CONNECTION</code> packet.
	 */
	public static final short ID_REMOTE_NEW_INCOMING_CONNECTION = 0x21;

	/**
	 * The ID of the <code>FILE_LIST_TRANSFER_HEADER</code> packet.
	 */
	public static final short ID_FILE_LIST_TRANSFER_HEADER = 0x22;

	/**
	 * The ID of the <code>FILE_LIST_TRANSFER_FILE</code> packet.
	 */
	public static final short ID_FILE_LIST_TRANSFER_FILE = 0x23;

	/**
	 * The ID of the <code>FILE_LIST_REFERENCE_PUSH_ACK</code> packet.
	 */
	public static final short ID_FILE_LIST_REFERENCE_PUSH_ACK = 0x24;

	/**
	 * The ID of the <code>DDT_DOWNLOAD_REQUEST</code> packet.
	 */
	public static final short ID_DDT_DOWNLOAD_REQUEST = 0x25;

	/**
	 * The ID of the <code>TRANSPORT_STRING</code> packet.
	 */
	public static final short ID_TRANSPORT_STRING = 0x26;

	/**
	 * The ID of the <code>REPLICA_MANAGER_CONSTRUCTION</code> packet.
	 */
	public static final short ID_REPLICA_MANAGER_CONSTRUCTION = 0x27;

	/**
	 * The ID of the <code>REPLICA_MANAGER_SCOPE_CHANGE</code> packet.
	 */
	public static final short ID_REPLICA_MANAGER_SCOPE_CHANGE = 0x28;

	/**
	 * The ID of the <code>REPLICA_MANAGER_SERIALIZE</code> packet.
	 */
	public static final short ID_REPLICA_MANAGER_SERIALIZE = 0x29;

	/**
	 * The ID of the <code>REPLICA_MANAGER_DOWNLOAD_STARTED</code> packet.
	 */
	public static final short ID_REPLICA_MANAGER_DOWNLOAD_STARTED = 0x2A;

	/**
	 * The ID of the <code>REPLICA_MANAGER_DOWNLOAD_COMPLETE</code> packet.
	 */
	public static final short ID_REPLICA_MANAGER_DOWNLOAD_COMPLETE = 0x2B;

	/**
	 * The ID of the <code>RAKVOICE_OPEN_CHANNEL_REQUEST</code> packet.
	 */
	public static final short ID_RAKVOICE_OPEN_CHANNEL_REQUEST = 0x2C;

	/**
	 * The ID of the <code>RAKVOICE_OPEN_CHANNEL_REPLY</code> packet.
	 */
	public static final short ID_RAKVOICE_OPEN_CHANNEL_REPLY = 0x2D;

	/**
	 * The ID of the <code>RAKVOICE_CLOSE_CHANNEL</code> packet.
	 */
	public static final short ID_RAKVOICE_CLOSE_CHANNEL = 0x2E;

	/**
	 * The ID of the <code>RAKVOICE_DATA</code> packet.
	 */
	public static final short ID_RAKVOICE_DATA = 0x2F;

	/**
	 * The ID of the <code>AUTOPATHER_GET_CHANGELIST_SINCE_DATE</code> packet.
	 */
	public static final short ID_AUTOPATCHER_GET_CHANGELIST_SINCE_DATE = 0x30;

	/**
	 * The ID of the <code>AUTOPATCHER_CREATION_LIST</code> packet.
	 */
	public static final short ID_AUTOPATCHER_CREATION_LIST = 0x31;

	/**
	 * The ID of the <code>AUTOPATCHER_DELETION_LIST</code> packet.
	 */
	public static final short ID_AUTOPATCHER_DELETION_LIST = 0x32;

	/**
	 * The ID of the <code>AUTOPATCHER_GET_PATCH</code> packet.
	 */
	public static final short ID_AUTOPATCHER_GET_PATCH = 0x33;

	/**
	 * The ID of the <code>AUTOPATCHER_PATCH_LIST</code> packet.
	 */
	public static final short ID_AUTOPATCHER_PATCH_LIST = 0x34;

	/**
	 * The ID of the <code>AUTOPATHER_REPOSITORY_FATAL_ERROR</code> packet.
	 */
	public static final short ID_AUTOPATCHER_REPOSITORY_FATAL_ERROR = 0x35;

	/**
	 * The ID of the
	 * <code>AUTOPATHER_CANNOT_DOWNLOAD_ORIGINAL_UNMODIFIED_FILES</code> packet.
	 */
	public static final short ID_AUTOPATCHER_CANNOT_DOWNLOAD_ORIGINAL_UNMODIFIED_FILES = 0x36;

	/**
	 * The ID of the <code>AUTOPATHER_FINISHED_INTERNAL</code> packet.
	 */
	public static final short ID_AUTOPATCHER_FINISHED_INTERNAL = 0x37;

	/**
	 * The ID of the <code>AUTOPATHER_FINISHED</code> packet.
	 */
	public static final short ID_AUTOPATCHER_FINISHED = 0x38;

	/**
	 * The ID of the <code>AUTOPATCHER_RESTART_APPLICATION</code> packet.
	 */
	public static final short ID_AUTOPATCHER_RESTART_APPLICATION = 0x39;

	/**
	 * The ID of the <code>NAT_PUNCHTHROUGH_REQUEST</code> packet.
	 */
	public static final short ID_NAT_PUNCHTHROUGH_REQUEST = 0x3A;

	/**
	 * The ID of the <code>NAT_CONNECT_AT_TIME</code> packet.
	 */
	public static final short ID_NAT_CONNECT_AT_TIME = 0x3B;

	/**
	 * The ID of the <code>NAT_GET_MOST_RECENT_PORT</code> packet.
	 */
	public static final short ID_NAT_GET_MOST_RECENT_PORT = 0x3C;

	/**
	 * The ID of the <code>NAT_CLIENT_READY</code> packet.
	 */
	public static final short ID_NAT_CLIENT_READY = 0x3D;

	/**
	 * The ID of the <code>NAT_TARGET_NOT_CONNECT</code> packet.
	 */
	public static final short ID_NAT_TARGET_NOT_CONNECTED = 0x3E;

	/**
	 * The ID of the <code>NAT_TARGET_UNRESPONSIVE</code> packet.
	 */
	public static final short ID_NAT_TARGET_UNRESPONSIVE = 0x3F;

	/**
	 * The ID of the <code>NAT_CONNECTION_TO_TARGET_LOST</code> packet.
	 */
	public static final short ID_NAT_CONNECTION_TO_TARGET_LOST = 0x40;

	/**
	 * The ID of the <code>NAT_ALREADY_IN_PROGRESS</code> packet.
	 */
	public static final short ID_NAT_ALREADY_IN_PROGRESS = 0x41;

	/**
	 * The ID of the <code>NAT_PUNCHTHROUGH_FAILED</code> packet.
	 */
	public static final short ID_NAT_PUNCHTHROUGH_FAILED = 0x42;

	/**
	 * The ID of the <code>NAT_PUNCHTHROUGH_SUCCEEDED</code> packet.
	 */
	public static final short ID_NAT_PUNCHTHROUGH_SUCCEEDED = 0x43;

	/**
	 * The ID of the <code>READY_EVENT_SET</code> packet.
	 */
	public static final short ID_READY_EVENT_SET = 0x44;

	/**
	 * The ID of the <code>READY_EVENT_UNSET</code> packet.
	 */
	public static final short ID_READY_EVENT_UNSET = 0x45;

	/**
	 * The ID of the <code>READY_EVENT_ALL_SET</code> packet.
	 */
	public static final short ID_READY_EVENT_ALL_SET = 0x46;

	/**
	 * The ID of the <code>READY_EVENT_QUERY</code> packet.
	 */
	public static final short ID_READY_EVENT_QUERY = 0x47;

	/**
	 * The ID of the <code>LOBBY_GENERAL</code> packet.
	 */
	public static final short ID_LOBBY_GENERAL = 0x48;

	/**
	 * The ID of the <code>RPC_REMOTE_ERROR</code> packet.
	 */
	public static final short ID_RPC_REMOTE_ERROR = 0x49;

	/**
	 * The ID of the <code>RPC_PLUGIN</code> packet.
	 */
	public static final short ID_RPC_PLUGIN = 0x4A;

	/**
	 * The ID of the <code>FILE_LIST_REFERENCE_PUSH</code> packet.
	 */
	public static final short ID_FILE_LIST_REFERENCE_PUSH = 0x4B;

	/**
	 * The ID of the <code>READY_EVENT_FORCE_ALL_SET</code> packet.
	 */
	public static final short ID_READY_EVENT_FORCE_ALL_SET = 0x4C;

	/**
	 * The ID of the <code>ROOMS_EXECUTE_FUNC</code> packet.
	 */
	public static final short ID_ROOMS_EXECUTE_FUNC = 0x4D;

	/**
	 * The ID of the <code>ROOMS_LOGON_STATUS</code> packet.
	 */
	public static final short ID_ROOMS_LOGON_STATUS = 0x4E;

	/**
	 * The ID of the <code>ROOMS_HANDLE_CHANGE</code> packet.
	 */
	public static final short ID_ROOMS_HANDLE_CHANGE = 0x4F;

	/**
	 * The ID of the <code>LOBBY2_SEND_MESSAGE</code> packet.
	 */
	public static final short ID_LOBBY2_SEND_MESSAGE = 0x50;

	/**
	 * The ID of the <code>LOBBY2_SERVER_ERROR</code> packet.
	 */
	public static final short ID_LOBBY2_SERVER_ERROR = 0x51;

	/**
	 * The ID of the <code>FMC2_NEW_HOST</code> packet.
	 */
	public static final short ID_FCM2_NEW_HOST = 0x52;

	/**
	 * The ID of the <code>FCM2_REQUEST_FCMGUID</code> packet.
	 */
	public static final short ID_FCM2_REQUEST_FCMGUID = 0x53;

	/**
	 * The ID of the <code>FCM2_RESPOND_CONNECTION_COUNT</code> packet.
	 */
	public static final short ID_FCM2_RESPOND_CONNECTION_COUNT = 0x54;

	/**
	 * The ID of the <code>FMC2_INFORM_FCMGUID</code> packet.
	 */
	public static final short ID_FCM2_INFORM_FCMGUID = 0x55;

	/**
	 * The ID of the <code>FCM2_UPDATE_MIN_TOTAL_CONNECTION_COUNT</code> packet.
	 */
	public static final short ID_FCM2_UPDATE_MIN_TOTAL_CONNECTION_COUNT = 0x56;

	/**
	 * The ID of the <code>FCM2_VERIFIED_JOIN_START</code> packet.
	 */
	public static final short ID_FCM2_VERIFIED_JOIN_START = 0x57;

	/**
	 * The ID of the <code>FCM2_VERIFIED_JOIN_CAPABLE</code> packet.
	 */
	public static final short ID_FCM2_VERIFIED_JOIN_CAPABLE = 0x58;

	/**
	 * The ID of the <code>FCM2_VERIFIED_JOIN_FAILED</code> packet.
	 */
	public static final short ID_FCM2_VERIFIED_JOIN_FAILED = 0x59;

	/**
	 * The ID of the <code>FCM2_VERIFIED_JOIN_ACCEPTED</code> packet.
	 */
	public static final short ID_FCM2_VERIFIED_JOIN_ACCEPTED = 0x5A;

	/**
	 * The ID of the <code>FCM2_VERIFIED_JOIN_REJECTED</code> packet.
	 */
	public static final short ID_FCM2_VERIFIED_JOIN_REJECTED = 0x5B;

	/**
	 * The ID of the <code>UDP_PROXY_GENERAL</code> packet.
	 */
	public static final short ID_UDP_PROXY_GENERAL = 0x5C;

	/**
	 * The ID of the <code>SQLITE3_EXEC</code> packet.
	 */
	public static final short ID_SQLITE3_EXEC = 0x5D;

	/**
	 * The ID of the <code>SQLITE3_UNKNOWN_DB</code> packet.
	 */
	public static final short ID_SQLITE3_UNKNOWN_DB = 0x5E;

	/**
	 * The ID of the <code>SQLLITE_LOGGER</code> packet.
	 */
	public static final short ID_SQLLITE_LOGGER = 0x5F;

	/**
	 * The ID of the <code>NAT_TYPE_DETECTION_REQUEST</code> packet.
	 */
	public static final short ID_NAT_TYPE_DETECTION_REQUEST = 0x60;

	/**
	 * The ID of the <code>NAT_TYPE_DETECTION_RESULT</code> packet.
	 */
	public static final short ID_NAT_TYPE_DETECTION_RESULT = 0x61;

	/**
	 * The ID of the <code>ROUTER_2_INTERNAL</code> packet.
	 */
	public static final short ID_ROUTER_2_INTERNAL = 0x62;

	/**
	 * The ID of the <code>ROUTER_2_FOWARDING_NO_PATH</code> packet.
	 */
	public static final short ID_ROUTER_2_FORWARDING_NO_PATH = 0x63;

	/**
	 * The ID of the <code>ROUTER_2_FORWARDING_ESTABLISHED</code> packet.
	 */
	public static final short ID_ROUTER_2_FORWARDING_ESTABLISHED = 0x64;

	/**
	 * The ID of the <code>ROUTER_2_REROUTED</code> packet.
	 */
	public static final short ID_ROUTER_2_REROUTED = 0x65;

	/**
	 * The ID of the <code>TEAM_BALANCER_INTERNAL</code> packet.
	 */
	public static final short ID_TEAM_BALANCER_INTERNAL = 0x66;

	/**
	 * The ID of the <code>TEAM_BALANCER_REQUESTED_TEAM_FULL</code> packet.
	 */
	public static final short ID_TEAM_BALANCER_REQUESTED_TEAM_FULL = 0x67;

	/**
	 * The ID of the <code>TEAM_BALANCER_REQUESTED_TEAM_LOCKED</code> packet.
	 */
	public static final short ID_TEAM_BALANCER_REQUESTED_TEAM_LOCKED = 0x68;

	/**
	 * The ID of the <code>TEAM_BALANCER_TEAM_REQUESTED_CANCELLED</code> packet.
	 */
	public static final short ID_TEAM_BALANCER_TEAM_REQUESTED_CANCELLED = 0x69;

	/**
	 * The ID of the <code>TEAM_BALANCER_TEAM_ASSIGNED</code> packet.
	 */
	public static final short ID_TEAM_BALANCER_TEAM_ASSIGNED = 0x6A;

	/**
	 * The ID of the <code>LIGHTSPEED_INTEGRATION</code> packet.
	 */
	public static final short ID_LIGHTSPEED_INTEGRATION = 0x6B;

	/**
	 * The ID of the <code>XBOX_LOBBY</code> packet.
	 */
	public static final short ID_XBOX_LOBBY = 0x6C;

	/**
	 * The ID of the
	 * <code>TWO_WAY_AUTHENTICATION_INCOMING_CHALLENEGE_SUCCESS</code> packet.
	 */
	public static final short ID_TWO_WAY_AUTHENTICATION_INCOMING_CHALLENGE_SUCCESS = 0x6D;

	/**
	 * The ID of the
	 * <code>TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_SUCCESS</code> packet.
	 */
	public static final short ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_SUCCESS = 0x6E;

	/**
	 * The ID of the
	 * <code>TWO_WAY_AUTHENTICATION_INCOMING_CHALLENGE_FAILURE</code> packet.
	 */
	public static final short ID_TWO_WAY_AUTHENTICATION_INCOMING_CHALLENGE_FAILURE = 0x6F;

	/**
	 * The ID of the
	 * <code>TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_FAILURE</code> packet.
	 */
	public static final short ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_FAILURE = 0x70;

	/**
	 * The ID of the
	 * <code>TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_TIMEOUT</code> packet.
	 */
	public static final short ID_TWO_WAY_AUTHENTICATION_OUTGOING_CHALLENGE_TIMEOUT = 0x71;

	/**
	 * The ID of the <code>TWO_WAY_AUTHENTICATION_NEGOTIATION</code> packet.
	 */
	public static final short ID_TWO_WAY_AUTHENTICATION_NEGOTIATION = 0x72;

	/**
	 * The ID of the <code>CLOUD_POST_REQUEST</code> packet.
	 */
	public static final short ID_CLOUD_POST_REQUEST = 0x73;

	/**
	 * The ID of the <code>CLOUD_RELEASE_REQUEST</code> packet.
	 */
	public static final short ID_CLOUD_RELEASE_REQUEST = 0x74;

	/**
	 * The ID of the <code>CLOUD_GET_REQUEST</code> packet.
	 */
	public static final short ID_CLOUD_GET_REQUEST = 0x75;

	/**
	 * The ID of the <code>CLOUD_GET_RESPONSE</code> packet.
	 */
	public static final short ID_CLOUD_GET_RESPONSE = 0x76;

	/**
	 * The ID of the <code>CLOUD_UNSUBSCRIBE_REQUEST</code> packet.
	 */
	public static final short ID_CLOUD_UNSUBSCRIBE_REQUEST = 0x77;

	/**
	 * The ID of the <code>CLOUD_SERVER_TO_SERVER_COMMAND</code> packet.
	 */
	public static final short ID_CLOUD_SERVER_TO_SERVER_COMMAND = 0x78;

	/**
	 * The ID of the <code>CLOUD_SUBSCRIPTION_NOTIFICATION</code> packet.
	 */
	public static final short ID_CLOUD_SUBSCRIPTION_NOTIFICATION = 0x79;

	/**
	 * The ID of the <code>LIB_VOICE</code> packet.
	 */
	public static final short ID_LIB_VOICE = 0x7A;

	/**
	 * The ID of the <code>RELAY_PLUGIN</code> packet.
	 */
	public static final short ID_RELAY_PLUGIN = 0x7B;

	/**
	 * The ID of the <code>NAT_REQUEST_BOUND_ADDRESSES</code> packet.
	 */
	public static final short ID_NAT_REQUEST_BOUND_ADDRESSES = 0x7C;

	/**
	 * The ID of the <code>NAT_RESPOND_BOUND_ADDRESSES</code> packet.
	 */
	public static final short ID_NAT_RESPOND_BOUND_ADDRESSES = 0x7D;

	/**
	 * The ID of the <code>FCM2_UPDATE_USER_CONTENT</code> packet.
	 */
	public static final short ID_FCM2_UPDATE_USER_CONTEXT = 0x7E;

	/**
	 * The ID of the <code>RESERVED_3</code> packet.
	 */
	public static final short ID_RESERVED_3 = 0x7F;

	/**
	 * The ID of the <code>RESERVED_4</code> packet.
	 */
	public static final short ID_RESERVED_4 = 0x80;

	/**
	 * The ID of the <code>RESERVED_5</code> packet.
	 */
	public static final short ID_RESERVED_5 = 0x81;

	/**
	 * The ID of the <code>RESERVED_6</code> packet.
	 */
	public static final short ID_RESERVED_6 = 0x82;

	/**
	 * The ID of the <code>RESERVERD_7</code> packet.
	 */
	public static final short ID_RESERVED_7 = 0x83;

	/**
	 * The ID of the <code>RESERVED_8</code> packet.
	 */
	public static final short ID_RESERVED_8 = 0x84;

	/**
	 * The ID of the <code>RESERVED_9</code> packet.
	 */
	public static final short ID_RESERVED_9 = 0x85;

	/**
	 * This is the first ID that the user can for the IDs of their packets.
	 * Since packet IDs are written using {@link #writeUnsignedByte(int)}, the
	 * highest packet ID that can be used is <code>0xFF</code>.
	 * <p>
	 * If one must have more than <code>121</code> packet IDs
	 * (<code>0xFF - ID_USER_PACKET_ENUM</code>), then one can have a singular
	 * ID that they use for all of their user packets with another field to be
	 * the packet ID.
	 */
	public static final short ID_USER_PACKET_ENUM = 0x86;

	/**
	 * The ID of the <code>CUSTOM_0</code> packet.
	 */
	public static final short ID_CUSTOM_0 = 0x80;

	/**
	 * The ID of the <code>CUSTOM_1</code> packet.
	 */
	public static final short ID_CUSTOM_1 = 0x81;

	/**
	 * The ID of the <code>CUSTOM_2</code> packet.
	 */
	public static final short ID_CUSTOM_2 = 0x82;

	/**
	 * The ID of the <code>CUSTOM_3</code> packet.
	 */
	public static final short ID_CUSTOM_3 = 0x83;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.message.CustomFourPacket CUSTOM_4}
	 * packet.
	 */
	public static final short ID_CUSTOM_4 = 0x84;

	/**
	 * The ID of the <code>CUSTOM_5</code> packet.
	 */
	public static final short ID_CUSTOM_5 = 0x85;

	/**
	 * The ID of the <code>CUSTOM_6</code> packet.
	 */
	public static final short ID_CUSTOM_6 = 0x86;

	/**
	 * The ID of the <code>CUSTOM_7</code> packet.
	 */
	public static final short ID_CUSTOM_7 = 0x87;

	/**
	 * The ID of the <code>CUSTOM_8</code> packet.
	 */
	public static final short ID_CUSTOM_8 = 0x88;

	/**
	 * The ID of the <code>CUSTOM_9</code> packet.
	 */
	public static final short ID_CUSTOM_9 = 0x89;

	/**
	 * The ID of the <code>CUSTOM_A</code> packet.
	 */
	public static final short ID_CUSTOM_A = 0x8A;

	/**
	 * The ID of the <code>CUSTOM_B</code> packet.
	 */
	public static final short ID_CUSTOM_B = 0x8B;

	/**
	 * The ID of the <code>CUSTOM_C</code> packet.
	 */
	public static final short ID_CUSTOM_C = 0x8C;

	/**
	 * The ID of the <code>CUSTOM_D</code> packet.
	 */
	public static final short ID_CUSTOM_D = 0x8D;

	/**
	 * The ID of the <code>CUSTOM_E</code> packet.
	 */
	public static final short ID_CUSTOM_E = 0x8E;

	/**
	 * The ID of the <code>CUSTOM_F</code> packet.
	 */
	public static final short ID_CUSTOM_F = 0x8F;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.message.acknowledge.AcknowledgedPacket
	 * ACK} packet.
	 */
	public static final short ID_ACK = 0xC0;

	/**
	 * The ID of the
	 * {@link com.whirvis.jraknet.protocol.message.acknowledge.NotAcknowledgedPacket
	 * NACK} packet.
	 */
	public static final short ID_NACK = 0xA0;

	private static boolean mappedNameIds;

	/**
	 * Maps all <code>public</code> packet IDs to their respective field names
	 * and vice-versa.
	 * <p>
	 * Packet IDs {@link #ID_CUSTOM_0}, {@link #ID_CUSTOM_1},
	 * {@link #ID_CUSTOM_2}, {@link #ID_CUSTOM_3}, {@link #ID_CUSTOM_4},
	 * {@link #ID_CUSTOM_5}, {@link #ID_CUSTOM_6}, {@link #ID_CUSTOM_7},
	 * {@link #ID_CUSTOM_8}, {@link #ID_CUSTOM_9}, {@link #ID_CUSTOM_A},
	 * {@link #ID_CUSTOM_B}, {@link #ID_CUSTOM_C}, {@link #ID_CUSTOM_D},
	 * {@link #ID_CUSTOM_E}, {@link #ID_CUSTOM_F}, {@link #ID_ACK}, and
	 * {@link #ID_NACK} are ignored as they are not only internal packets but
	 * they also override other packets with the same ID.
	 */
	private static void mapNameIds() {
		if (!mappedNameIds) {
			Logger log = LogManager.getLogger(RakNetPacket.class);
			for (Field field : RakNetPacket.class.getFields()) {
				if (field.getType().equals(short.class)) {
					try {
						short packetId = field.getShort(null);
						if ((packetId >= ID_CUSTOM_0 && packetId <= ID_CUSTOM_F) || packetId == ID_ACK
								|| packetId == ID_NACK) {
							continue; // Ignored as they override other packet
										// IDs
						}
						String packetName = field.getName();
						String currentName = PACKET_NAMES.put(packetId, packetName);
						PACKET_IDS.put(packetName, packetId);
						if (currentName != null) {
							if (!currentName.equals(packetName)) {
								log.warn("Found duplicate ID " + RakNet.toHexStringId(packetId) + " for \"" + packetName
										+ "\" and \"" + currentName + "\", overriding name and ID");
							}
						} else {
							log.debug("Assigned packet ID " + RakNet.toHexStringId(packetId) + " to " + packetName);
						}
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}
			}
			mappedNameIds = true;
		}
	}

	/**
	 * Returns whether or not a packet with the specified ID exists as a RakNet
	 * packet.
	 * 
	 * @param id
	 *            the ID of the packet.
	 * @return <code>true</code> if a packet with the ID exists as a RakNet
	 *         packet, <code>false</code>.
	 */
	public static boolean hasPacket(int id) {
		if (!mappedNameIds) {
			mapNameIds();
		}
		return PACKET_NAMES.containsKey((short) id);
	}

	/**
	 * Returns whether or not the specified packet exists as a RakNet packet.
	 * 
	 * @param packet
	 *            the packet.
	 * @return <code>true</code> if the specified packet exists as a RakNet
	 *         packet, <code>false</code>.
	 */
	public static boolean hasPacket(RakNetPacket packet) {
		if (packet == null) {
			return false;
		}
		return hasPacket(packet.getId());
	}

	/**
	 * Returns whether or not a packet with the specified name exists as a
	 * RakNet packet.
	 * 
	 * @param name
	 *            the name of the packet.
	 * @return <code>true</code> if a packet with the name exists as a RakNet
	 *         packet, <code>false</code>.
	 */
	public static boolean hasPacket(String name) {
		if (!mappedNameIds) {
			mapNameIds();
		}
		return PACKET_IDS.containsKey(name);
	}

	/**
	 * Returns the name of the packet with the specified ID.
	 * <p>
	 * Packet IDs {@link #ID_CUSTOM_0}, {@link #ID_CUSTOM_1},
	 * {@link #ID_CUSTOM_2}, {@link #ID_CUSTOM_3}, {@link #ID_CUSTOM_4},
	 * {@link #ID_CUSTOM_5}, {@link #ID_CUSTOM_6}, {@link #ID_CUSTOM_7},
	 * {@link #ID_CUSTOM_8}, {@link #ID_CUSTOM_9}, {@link #ID_CUSTOM_A},
	 * {@link #ID_CUSTOM_B}, {@link #ID_CUSTOM_C}, {@link #ID_CUSTOM_D},
	 * {@link #ID_CUSTOM_E}, {@link #ID_CUSTOM_F}, {@link #ID_ACK}, and
	 * {@link #ID_NACK} will never be returned as they are not only internal
	 * packets but they also override other packets with the same ID.
	 * 
	 * @param id
	 *            the ID of the packet.
	 * @return the name of the packet with the specified ID, its hexadecimal ID
	 *         according to {@link RakNet#toHexStringId(int)} if it does not
	 *         exist.
	 */
	public static String getName(int id) {
		if (mappedNameIds == false) {
			mapNameIds();
		}
		if (!PACKET_NAMES.containsKey((short) id)) {
			return RakNet.toHexStringId(id & 0xFF);
		}
		return PACKET_NAMES.get((short) id);
	}

	/**
	 * Returns the name of the specified packet.
	 * <p>
	 * Packet IDs {@link #ID_CUSTOM_0}, {@link #ID_CUSTOM_1},
	 * {@link #ID_CUSTOM_2}, {@link #ID_CUSTOM_3}, {@link #ID_CUSTOM_4},
	 * {@link #ID_CUSTOM_5}, {@link #ID_CUSTOM_6}, {@link #ID_CUSTOM_7},
	 * {@link #ID_CUSTOM_8}, {@link #ID_CUSTOM_9}, {@link #ID_CUSTOM_A},
	 * {@link #ID_CUSTOM_B}, {@link #ID_CUSTOM_C}, {@link #ID_CUSTOM_D},
	 * {@link #ID_CUSTOM_E}, {@link #ID_CUSTOM_F}, {@link #ID_ACK}, and
	 * {@link #ID_NACK} will never be returned as they are not only internal
	 * packets but they also override other packets with the same ID.
	 * 
	 * @param packet
	 *            the packet.
	 * @return the name of the packet, its hexadecimal ID according to
	 *         {@link RakNet#toHexStringId(int)} if it does not exist.
	 */
	public static String getName(RakNetPacket packet) {
		if (packet == null) {
			return null;
		}
		return getName(packet.getId());
	}

	/**
	 * Returns the ID of the packet with the specified name.
	 * 
	 * @param name
	 *            the name of the packet.
	 * @return the ID of the packet with the specified name, <code>-1</code> if
	 *         it does not exist.
	 */
	public static short getId(String name) {
		if (!mappedNameIds) {
			mapNameIds();
		}
		return PACKET_IDS.containsKey(name) ? PACKET_IDS.get(name) : -1;
	}

	/**
	 * Returns whether or not a method with the specified name has been
	 * overridden the method in the original specified class by the specified
	 * class instance.
	 * 
	 * @param instance
	 *            the class instance.
	 * @param clazz
	 *            the original class.
	 * @param methodName
	 *            the name of the method.
	 * @return <code>true</code> if the method has been overridden,
	 *         <code>false</code> otherwise.
	 */
	private static boolean isMethodOverriden(Class<?> instance, Class<?> clazz, String methodName) {
		try {
			if (instance == null || clazz == null || methodName == null) {
				return false; // Not enough information to compare
			}
			return !clazz.getMethod(methodName).getDeclaringClass().equals(clazz);
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
	}

	private short id;
	private final boolean supportsEncoding;
	private final boolean supportsDecoding;

	/**
	 * Creates a RakNet packet.
	 * 
	 * @param id
	 *            the ID of the packet.
	 * @throws IllegalArgumentException
	 *             if the <code>id</code> is not in between <code>0-255</code>.
	 */
	public RakNetPacket(int id) throws IllegalArgumentException {
		super();
		if (id < 0x00 || id > 0xFF) {
			throw new IllegalArgumentException("ID must be in between 0-255");
		}
		this.writeUnsignedByte(this.id = (short) id);
		this.supportsEncoding = isMethodOverriden(this.getClass(), RakNetPacket.class, ENCODE_METHOD_NAME);
		this.supportsDecoding = isMethodOverriden(this.getClass(), RakNetPacket.class, DECODE_METHOD_NAME);
	}

	/**
	 * Creates a RakNet packet.
	 * 
	 * @param buffer
	 *            the buffer to read from and write to. The buffer must have at
	 *            least one byte to be read from for the ID.
	 * @throws IllegalArgumentException
	 *             if the <code>buffer</code> has less than <code>1</code>
	 *             readable <code>byte</code>.
	 */
	public RakNetPacket(ByteBuf buffer) throws IllegalArgumentException {
		super(buffer);
		if (this.remaining() < 1) {
			throw new IllegalArgumentException("Buffer must have at least one readable byte for the ID");
		}
		this.id = this.readUnsignedByte();
		this.supportsEncoding = isMethodOverriden(this.getClass(), RakNetPacket.class, ENCODE_METHOD_NAME);
		this.supportsDecoding = isMethodOverriden(this.getClass(), RakNetPacket.class, DECODE_METHOD_NAME);
	}

	/**
	 * Creates a RakNet packet.
	 * 
	 * @param datagram
	 *            the datagram packet to read from. The datagram must have at
	 *            least one byte to be read from for the ID.
	 * @throws IllegalArgumentException
	 *             if the buffer contained within the datagram has less than
	 *             <code>1</code> readable <code>byte</code>.
	 * @see #RakNetPacket(ByteBuf)
	 */
	public RakNetPacket(DatagramPacket datagram) throws IllegalArgumentException {
		this(datagram.content());
	}

	/**
	 * Creates a RakNet packet.
	 * 
	 * @param data
	 *            the byte array to read to read from. The byte array must have
	 *            at least one byte to be read from for the ID.
	 * @throws IllegalArgumentException
	 *             if the length of the <code>data</code> is less than
	 *             <code>1</code>.
	 * @see #RakNetPacket(ByteBuf)
	 */
	public RakNetPacket(byte[] data) throws IllegalArgumentException {
		this(Unpooled.copiedBuffer(data));
	}

	/**
	 * Creates a RakNet packet.
	 * 
	 * @param packet
	 *            the packet to read from and write to. The packet must have at
	 *            least one byte to be read from for the ID. If the packet is an
	 *            instance of {@link RakNetPacket}, it will be casted and have
	 *            its ID retrieved via {@link #getId()}.
	 * @throws IllegalArgumentException
	 *             if the packet size has less than <code>1</code> readable
	 *             <code>byte</code> and is not an instance of
	 *             {@link RakNetPacket}.
	 */
	public RakNetPacket(Packet packet) throws IllegalArgumentException {
		super(packet);
		if (packet instanceof RakNetPacket) {
			this.id = ((RakNetPacket) packet).id;
		} else {
			if (this.remaining() < 1) {
				throw new IllegalArgumentException("The packet must have at least one byte to read the ID");
			}
			this.id = this.readUnsignedByte();
		}
		this.supportsEncoding = isMethodOverriden(this.getClass(), RakNetPacket.class, ENCODE_METHOD_NAME);
		this.supportsDecoding = isMethodOverriden(this.getClass(), RakNetPacket.class, DECODE_METHOD_NAME);
	}

	/**
	 * Returns the ID of the packet.
	 * 
	 * @return the ID of the packet.
	 */
	public final short getId() {
		return this.id;
	}

	/**
	 * Reads a magic array and returns whether or not it is valid.
	 * 
	 * @return <code>true</code> if the magic array was valid,
	 *         <code>false</code> otherwise.
	 */
	public final boolean readMagic() {
		byte[] magicCheck = this.read(MAGIC.length);
		return Arrays.equals(MAGIC, magicCheck);
	}

	/**
	 * Reads a {@link ConnectionType}.
	 * <p>
	 * This method will check to make sure if there is at least enough data to
	 * read the the connection type magic before reading the data. This is due
	 * to the fact that this is meant to be used strictly at the end of packets
	 * that can be used to signify the protocol implementation of the sender.
	 * 
	 * @return a {@link ConnectionType}, {@link ConnectionType#VANILLA} if not
	 *         enough data to read one is present.
	 * @throws RakNetException
	 *             if not enough data is present in the packet after the
	 *             connection type magic or there are duplicate keys in the
	 *             metadata.
	 */
	public final ConnectionType readConnectionType() throws RakNetException {
		if (this.remaining() >= ConnectionType.MAGIC.length) {
			byte[] connectionMagicCheck = this.read(ConnectionType.MAGIC.length);
			if (Arrays.equals(ConnectionType.MAGIC, connectionMagicCheck)) {
				UUID uuid = this.readUUID();
				String name = this.readString();
				String language = this.readString();
				String version = this.readString();
				HashMap<String, String> metadata = new HashMap<>();
				int metadataLength = this.readUnsignedByte();
				for (int i = 0; i < metadataLength; i++) {
					String key = this.readString();
					String value = this.readString();
					if (metadata.containsKey(key)) {
						throw new RakNetException("Duplicate metadata key \"" + key + "\"");
					}
					metadata.put(key, value);
				}
				return new ConnectionType(uuid, name, language, version, metadata);
			}
		}
		return ConnectionType.VANILLA;
	}

	/**
	 * Writes the magic sequence to the packet.
	 * 
	 * @return the packet.
	 */
	public final RakNetPacket writeMagic() {
		this.write(MAGIC);
		return this;
	}

	/**
	 * Writes a {@link ConnectionType} to the packet.
	 * 
	 * @param connectionType
	 *            the connection type, a <code>null</code> value will have
	 *            {@link ConnectionType#JRAKNET} connection type be used
	 *            instead.
	 * @return the packet.
	 * @throws RakNetException
	 *             if there are too many values in the metadata.
	 */
	public final Packet writeConnectionType(ConnectionType connectionType) throws RakNetException {
		connectionType = (connectionType != null ? connectionType : ConnectionType.JRAKNET);
		this.write(ConnectionType.MAGIC);
		this.writeUUID(connectionType.getUUID());
		this.writeString(connectionType.getName());
		this.writeString(connectionType.getLanguage());
		this.writeString(connectionType.getVersion());
		if (connectionType.getMetaData().size() > ConnectionType.MAX_METADATA_VALUES) {
			throw new RakNetException("Too many metadata values");
		}
		this.writeUnsignedByte(connectionType.getMetaData().size());
		for (Entry<String, String> metadataEntry : connectionType.getMetaData().entrySet()) {
			this.writeString(metadataEntry.getKey());
			this.writeString(metadataEntry.getValue());
		}
		return this;
	}

	/**
	 * Writes the {@link ConnectionType#JRAKNET} connection type to the packet.
	 * 
	 * @return the packet.
	 * @throws RuntimeException
	 *             if a <code>RakNetException</code> is caught despite the fact
	 *             that this method should never throw an error in the first
	 *             place.
	 */
	public final Packet writeConnectionType() throws RuntimeException {
		try {
			return this.writeConnectionType(null);
		} catch (RakNetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns whether or not encoding is supported. If encoding is not
	 * supported, calling {@link #encode()} will yield an
	 * <code>UnsupportedOperationException</code>.
	 * 
	 * @return <code>true</code> if encoding is supported, <code>false</code>
	 *         otherwise.
	 */
	public final boolean supportsEncoding() {
		return this.supportsEncoding;
	}

	/**
	 * Encodes the packet.
	 * 
	 * @throws UnsupportedOperationException
	 *             if encoding the packet is not supported.
	 */
	public void encode() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Encoding not supported");
	}

	/**
	 * Returns whether or not decoding is supported. If decoding is not
	 * supported, calling {@link #decode()} will yield an
	 * <code>UnsupportedOperationException</code>.
	 * 
	 * @return <code>true</code> if decoding is supported, <code>false</code>
	 *         otherwise.
	 */
	public final boolean supportsDecoding() {
		return this.supportsDecoding;
	}

	/**
	 * Decodes the packet.
	 * 
	 * @throws UnsupportedOperationException
	 *             if decoding the packet is not supported.
	 */
	public void decode() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Decoding not supported");
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param buffer
	 *            the buffer to read from and write to, a <code>null</code>
	 *            value will have a new buffer be used instead.
	 * @param updateId
	 *            <code>true</code> if the ID should be updated,
	 *            <code>false</code> otherwise.
	 * @return the packet.
	 * @throws IndexOutOfBoundsException
	 *             if <code>updateId</code> is <code>true</code> and the new
	 *             buffer has less than <code>1</code> readable
	 *             <code>byte</code>.
	 * @see #setBuffer(ByteBuf)
	 */
	public final RakNetPacket setBuffer(ByteBuf buffer, boolean updateId) throws IndexOutOfBoundsException {
		super.setBuffer(buffer);
		if (updateId) {
			this.id = this.readUnsignedByte();
		}
		return this;
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param datagram
	 *            the {@link DatagramPacket} whose buffer to read from and write
	 *            to.
	 * @param updateId
	 *            <code>true</code> if the ID should be updated,
	 *            <code>false</code> otherwise.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>datagram</code> packet is <code>null</code>.
	 * @throws IndexOutOfBoundsException
	 *             if <code>updateId</code> is <code>true</code> and the new
	 *             buffer has less than <code>1</code> readable
	 *             <code>byte</code>.
	 * @see #setBuffer(DatagramPacket)
	 */
	public final RakNetPacket setBuffer(DatagramPacket datagram, boolean updateId)
			throws NullPointerException, IndexOutOfBoundsException {
		if (datagram == null) {
			throw new NullPointerException("Datagram packet cannot be null");
		}
		return this.setBuffer(datagram.content(), updateId);
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param data
	 *            the <code>byte[]</code> to create the new buffer from.
	 * @param updateId
	 *            <code>true</code> if the ID should be updated,
	 *            <code>false</code> otherwise.
	 * @return the packet.
	 * @throws NullPointerException
	 *             if the <code>data</code> is <code>null</code>.
	 * @throws IndexOutOfBoundsException
	 *             if <code>updateId</code> is <code>true</code> and the new
	 *             buffer has less than <code>1</code> readable
	 *             <code>byte</code>.
	 * @see #setBuffer(byte[])
	 */
	public final RakNetPacket setBuffer(byte[] data, boolean updateId)
			throws NullPointerException, IndexOutOfBoundsException {
		return this.setBuffer(Unpooled.copiedBuffer(data), updateId);
	}

	/**
	 * Updates the buffer.
	 * 
	 * @param packet
	 *            the packet whose buffer to copy to read from and write to.
	 * @param updateId
	 *            <code>true</code> if the ID should be updated,
	 *            <code>false</code> otherwise.
	 * @return the packet.
	 * @throws IndexOutOfBoundsException
	 *             if <code>updateId</code> is <code>true</code> and the new
	 *             buffer has less than <code>1</code> readable
	 *             <code>byte</code>.
	 * @see #setBuffer(Packet)
	 */
	public final RakNetPacket setBuffer(Packet packet, boolean updateId)
			throws NullPointerException, IndexOutOfBoundsException {
		return this.setBuffer(packet.copy(), updateId);
	}

	/**
	 * Flips the packet.
	 * 
	 * @param updateId
	 *            <code>true</code> if ID should be updated, <code>false</code>
	 *            otherwise.
	 * @return the packet.
	 * @throws IndexOutOfBoundsException
	 *             if <code>updateId</code> is <code>true</code> and the buffer
	 *             has less than <code>1</code> readable <code>byte</code>.
	 * @see #flip()
	 */
	public final RakNetPacket flip(boolean updateId) throws IndexOutOfBoundsException {
		super.flip();
		if (updateId) {
			this.id = this.readUnsignedByte();
		}
		return this;
	}

	/**
	 * {@inheritDoc} After the packet has been flipped, an unsigined
	 * <code>byte</code> will be read to get the ID.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the buffer has less than <code>1</code> readable
	 *             <code>byte</code>.
	 */
	@Override
	public Packet flip() throws IndexOutOfBoundsException {
		return this.flip(true);
	}

	@Override
	public String toString() {
		return "RakNetPacket [id=" + id + ", size()=" + size() + ", remaining()=" + remaining() + "]";
	}

}
