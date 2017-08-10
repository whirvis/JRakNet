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

/**
 * Use to signify which implementation of the RakNet protocol is being used by a
 * connection.
 * 
 * @author Whirvis "MarfGamer" Ardenaur
 */
public enum ConnectionType {

	VANILLA("Vanilla", 0x00), JRAKNET("JRakNet", 0x01), RAKLIB("RakLib", 0x02), JRAKLIB("JRakLib",
			0x03), JRAKLIB_PLUS("JRakLib+", 0x04);

	// Connection type header magic
	public static final byte[] MAGIC = new byte[] { (byte) 0x03, (byte) 0x08, (byte) 0x05, (byte) 0x0B, 0x43,
			(byte) 0x54, (byte) 0x49 };

	private final String name;
	private final short id;

	private ConnectionType(String name, int id) {
		this.name = name;
		this.id = (short) id;
		if (id < 0 || id > 255) {
			throw new IllegalArgumentException("Invalid ID, must be in between 0-255");
		}
	}

	/**
	 * @return the name of the implementation.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the ID of the implementation.
	 */
	public short getId() {
		return this.id;
	}

	/**
	 * @param id
	 *            the ID of the implementation.
	 * @return the <code>ConnectionType</code> based on it's implementation ID.
	 */
	public static ConnectionType getType(int id) {
		for (ConnectionType type : ConnectionType.values()) {
			if (type.id == id) {
				return type;
			}
		}
		return null;
	}

}