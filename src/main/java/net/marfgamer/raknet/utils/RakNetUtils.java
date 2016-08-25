package net.marfgamer.raknet.utils;

public class RakNetUtils {

	public static long parseLongPassive(String longStr) {
		try {
			return Long.parseLong(longStr);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static int parseIntPassive(String intStr) {
		return (int) RakNetUtils.parseLongPassive(intStr);
	}

}
