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
 * Copyright (c) 2016 Whirvis Ardenaur
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
package net.marfgamer.raknet;

import java.io.File;

import net.marfgamer.raknet.branding.MarfUtils;

/**
 * Contains info for RakNet
 *
 * @author Whirvis Ardenaur
 */
public interface RakNet {

	// Library data
	public static final File dataFolder = new File(MarfUtils.getDataFolder(), "JRakNet");
	public static final File idFile = new File(dataFolder, "unique_id.json");

	// Protocol version
	public static final int SERVER_NETWORK_PROTOCOL = 8;
	public static final int CLIENT_NETWORK_PROTOCOL = 8;

	// Transfer size
	public static final int MINIMUM_TRANSFER_UNIT = 530;

	// Split packet size
	public static final int MAX_SPLITS_PER_QUEUE = 4;
	public static final int MAX_SPLIT_COUNT = 128;

	// Time conversion
	public static final long SERVER_TIMEOUT = 10 * 1000L;
	public static final long CLIENT_TIMEOUT = 10 * 1000L;
	public static final long ONE_MINUTES_MILLIS = 60 * 1000L;
	public static final long FIVE_MINUTES_MILLIS = 300 * 1000L;

}
