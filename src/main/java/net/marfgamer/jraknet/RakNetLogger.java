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
package net.marfgamer.jraknet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.marfgamer.jraknet.client.RakNetClient;
import net.marfgamer.jraknet.server.RakNetServer;

/**
 * Used for logging in JRakNet.
 * 
 * @author Whirvis "MarfGamer" Ardenaur
 *
 */
public class RakNetLogger {

	private static Logger logger;

	static {
		init();
	}

	/**
	 * Initializes the logger
	 */
	public static void init() {
		logger = LogManager.getLogger("JRakNet");
	}

	/**
	 * Logs a message with the severity level set to info.
	 * 
	 * @param name
	 *            the name of the logger.
	 * @param msg
	 *            the message to log.
	 */
	public static void info(String name, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.info(name + ": " + msg);
		}
	}

	/**
	 * Logs a message with the severity level set to warn.
	 * 
	 * @param name
	 *            the name of the logger.
	 * @param msg
	 *            the message to log.
	 */
	public static void warn(String name, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.warn(name + ": " + msg);
		}
	}

	/**
	 * Logs a message with the severity level set to error.
	 * 
	 * @param name
	 *            the name of the logger.
	 * @param msg
	 *            the message to log.
	 */
	public static void error(String name, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.error(name + ": " + msg);
		}
	}

	/**
	 * Logs a message with the severity level set to info.
	 * 
	 * @param server
	 *            the server that is logging.
	 * @param msg
	 *            the message to log.
	 */
	public static void info(RakNetServer server, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.info("server #" + server.getGloballyUniqueId() + ": " + msg);
		}
	}

	/**
	 * Logs a message with the severity level set to warn.
	 * 
	 * @param server
	 *            the server that is logging.
	 * @param msg
	 *            the message to log.
	 */
	public static void warn(RakNetServer server, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.warn("server #" + server.getGloballyUniqueId() + ": " + msg);
		}
	}

	/**
	 * Logs a message with the severity level set to error.
	 * 
	 * @param server
	 *            the server that is logging.
	 * @param msg
	 *            the message to log.
	 */
	public static void error(RakNetServer server, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.error("server #" + server.getGloballyUniqueId() + ": " + msg);
		}
	}

	/**
	 * Logs a message with the severity level set to info.
	 * 
	 * @param client
	 *            the client that is logging.
	 * @param msg
	 *            the message to log.
	 */
	public static void info(RakNetClient client, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.info("client #" + client.getGloballyUniqueId() + ": " + msg);
		}
	}

	/**
	 * Logs a message with the severity level set to warn.
	 * 
	 * @param client
	 *            the client that is logging.
	 * @param msg
	 *            the message to log.
	 */
	public static void warn(RakNetClient client, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.warn("client #" + client.getGloballyUniqueId() + ": " + msg);
		}
	}

	/**
	 * Logs a message with the severity level set to error.
	 * 
	 * @param client
	 *            the client that is logging.
	 * @param msg
	 *            the message to log.
	 */
	public static void error(RakNetClient client, String msg) {
		if (RakNet.isLoggingEnabled()) {
			logger.error("client #" + client.getGloballyUniqueId() + ": " + msg);
		}
	}

}
