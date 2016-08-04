package net.marfgamer.raknet.branding;

import java.io.File;

public class MarfUtils {

	private static final File appData = new File(System.getenv("APPDATA"));
	private static final File marfData = new File(appData, "MarfData");

	public static File getDataFolder() {
		if (!marfData.exists()) {
			marfData.mkdirs();
		}
		return MarfUtils.marfData;
	}

}
