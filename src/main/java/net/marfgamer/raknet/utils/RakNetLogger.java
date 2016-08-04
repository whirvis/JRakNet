package net.marfgamer.raknet.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class RakNetLogger {
	
	private final Logger logger;
	private final Level level;
	
	public RakNetLogger(Logger logger) {
		this.logger = logger;
		this.level = RakNetConfig.getLogLevel();
	}
	
	public void log(String msg) {
		logger.info( msg);
	}
	
}
