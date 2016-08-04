package net.marfgamer.raknet.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Attempts to retrieve the configuration for JRakNet, if this fails it will
 * hard exit using <code>System.exit(1);</code>
 * 
 * @author MarfGamer
 */
public class RakNetConfig {

	private static final String RAKNET_CONFIG_JSON = "/jraknet_config.json";
	private static final String LOGGER_LOG_LEVEL = "logger_log_level";
	private static final String SEND_TICK_MS = "send_tick_ms";

	private static JsonObject config;
	private static Level level;
	private static Logger logger;

	static {
		try {
			InputStream configStream = RakNetUtils.class.getResource(
					RAKNET_CONFIG_JSON).openStream();
			JsonObject config = new JsonParser().parse(
					new InputStreamReader(configStream)).getAsJsonObject();
			if (!config.has(SEND_TICK_MS)) {
				throw new IOException("Config is missing " + SEND_TICK_MS + "!");
			}
			if(config.has(LOGGER_LOG_LEVEL)) {
				level = Level.getLevel(config.get(LOGGER_LOG_LEVEL).getAsString());
			}
			configStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * How long until the next <code>CustomPacket</code> will be sent, if any
	 * 
	 * @return long
	 */
	public static long getSendTickMS() {
		return config.get(SEND_TICK_MS).getAsLong();
	}

	/**
	 * Returns the logger for JRakNet
	 * 
	 * @return
	 */
	public static Level getLogLevel() {
		return level;
	}

	public static Logger getLogger() {
		return logger;
	}

}
