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
 * Copyright (c) 2016-2018 Whirvis "MarfGamer" Ardenaur
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
 * Used to signify which implementation of the RakNet protocol is being used by
 * a connection. If you would like your implementation to be listed here, create
 * an issue on the JRakNet repository with the tag "Connection type support".
 * Keep in mind that this functionality has <i>no</i> guarantees to function
 * completely, as it is completely dependent on the implementation to implement
 * this feature.
 * 
 * @author Whirvis "MarfGamer" Ardenaur
 * @see <a href=
 *      "https://github.com/JRakNet/JRakNet/issues/new">https://github.com/JRakNet/JRakNet/issues/new</a>
 */
public enum ConnectionType {

	/**
	 * A connection from a vanilla client or an unknown implementation.
	 */
	VANILLA("Vanilla", null, 0x00),

	/**
	 * A JRakNet connection.
	 */
	JRAKNET("JRakNet", "Java", 0x01),

	/**
	 * A RakLib connection.
	 */
	RAKLIB("RakLib", "PHP", 0x02),

	/**
	 * A JRakLib+ connection.
	 */
	JRAKLIB_PLUS("JRakLib+", "Java", 0x03),

	/**
	 * A CRakNet++ connection.
	 */
	CRAKNET_PLUS_PLUS("CRakNet++", "C++", 0x04),

	/**
	 * A PyRaknet connection.
	 */
	PYRAKNET("PyRakNet", "Python", 0x05),

	/**
	 * A GoRakNet connection.
	 */
	GORAKNET("GoRakNet", "Go", 0x06),

	/**
	 * A LuaRakNet connection.
	 */
	LUARAKNET("LuaRakNet", "Lua", 0x07),

	/**
	 * A CRakNet# connection.
	 */
	CRAKNET_SHARP("CRakNet#", "C#", 0x08),

	/**
	 * A PHPRakNet connection.
	 */
	PHPRAKNET("PHPRakNet", "PHP", 0x09),

	/**
	 * A CRakNet connection.
	 */
	CRAKNET("CRakNet", "C", 0x0A),

	/**
	 * A DRakNet connection.
	 */
	DRAKNET("DRakNet", "D", 0x0B),

	/**
	 * A NodeRakNet connection.
	 */
	NODERAKNET("NodeRakNet", "Node.js", 0x0C),

	/**
	 * An AsmRaknet connection.
	 */
	ASMRAKNET("AsmRakNet", "Assembly", 0x0D),

	/**
	 * A JungleTree connection.
	 */
	JUNGLETREE("JungleTree", "Java", 0x0E);

	// Connection type header magic
	public static final byte[] MAGIC = new byte[] { (byte) 0x03, (byte) 0x08, (byte) 0x05, (byte) 0x0B, 0x43,
			(byte) 0x54, (byte) 0x49 };

	private final String name;
	private final String language;
	private final short id;

	private ConnectionType(String name, String language, int id) {
		this.name = name;
		this.language = language;
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
	 * @return the programming language of the implementation.
	 */
	public String getLanguage() {
		return this.language;
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
		return VANILLA;
	}

}