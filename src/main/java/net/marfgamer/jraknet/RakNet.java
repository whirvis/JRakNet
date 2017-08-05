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
 * Copyright (c) 2016, 2017 Trent "MarfGamer" Summerlin
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
package net.marfgamer.jraknet;

/**
 * Contains info for RakNet
 *
 * @author Trent "MarfGamer" Summerlin
 */
public class RakNet {

	// Network protocol data
	public static final int SERVER_NETWORK_PROTOCOL = 8;
	public static final int CLIENT_NETWORK_PROTOCOL = 8;

	// Maximum Transfer Unit data
	public static final int MINIMUM_TRANSFER_UNIT = 548;

	// Session limiting
	public static final int MAX_CHANNELS = 32;
	public static final byte DEFAULT_CHANNEL = 0x00;
	public static final int MAX_SPLIT_COUNT = 128;
	public static final int MAX_SPLITS_PER_QUEUE = 4;

	// Max packet count
	private static long MAX_PACKETS_PER_SECOND = 500;

	// Logging
	private static boolean USE_LOGGING = false;

	/**
	 * Returns how many packets can be received in the span of a single second (1000
	 * milliseconds) before a session is blocked.
	 * 
	 * @return how many packets can be received in the span of a single second
	 *         before a session is blocked.
	 */
	public static long getMaxPacketsPerSecond() {
		return MAX_PACKETS_PER_SECOND;
	}

	/**
	 * Sets how many packets can be received in the span of a single second (1000
	 * milliseconds) before a session is blocked.
	 * 
	 * @param maxPacketsPerSecond
	 *            how many packets can be received in the span of a single second
	 *            before a session is blocked.
	 */
	public static void setMaxPacketsPerSecond(long maxPacketsPerSecond) {
		MAX_PACKETS_PER_SECOND = maxPacketsPerSecond;
	}

	/**
	 * Removes the max packets per second limit so that no matter how many packets a
	 * session sends it will never be blocked. This is unrecommended, as it can open
	 * your server to DOS/DDOS attacks.
	 */
	public static void setMaxPacketsPerSecondUnlimited() {
		MAX_PACKETS_PER_SECOND = Long.MAX_VALUE;
	}

	/**
	 * @return whether or not if JRakNet should log to the console.
	 */
	public static boolean isLoggingEnabled() {
		return USE_LOGGING;
	}

	/**
	 * Enables JRakNet logging.
	 */
	public static void enableLogging() {
		RakNetLogger.setLevel(-1);
		USE_LOGGING = true;
	}

	/**
	 * Enables JRakNet logging with the specified logger level.
	 * 
	 * @param level
	 *            the logger level to use.
	 */
	public static void enableLogging(int level) {
		RakNetLogger.setLevel(level);
		USE_LOGGING = true;
	}

	/**
	 * Disables JRakNet logging.
	 */
	public static void disableLogging() {
		USE_LOGGING = false;
	}

	// Session timing
	public static final long SEND_INTERVAL = 50L;
	public static final long RECOVERY_SEND_INTERVAL = SEND_INTERVAL;
	public static final long PING_SEND_INTERVAL = 2500L;
	public static final long DETECTION_SEND_INTERVAL = PING_SEND_INTERVAL * 2;
	public static final long SESSION_TIMEOUT = DETECTION_SEND_INTERVAL * 5;
	public static final long MAX_PACKETS_PER_SECOND_BLOCK = (1000L * 300);

}
