/*
 *       _   _____            _      _   _          _
 *      | | |  __ \          | |    | \ | |        | |
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
package com.whirvis.jraknet.stream;

import java.io.DataOutput;
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
 * Copyright (c) 2016-2018 Trent "Whirvis" Summerlin
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
import java.io.IOException;
import java.io.OutputStream;

import com.whirvis.jraknet.Packet;

/**
 * Used to write data to a <code>Packet</code> with ease, to retrieve a
 * <code>Packet</code>'s <code>DataOutput</code> simply use
 * <code>getDataOutput()</code>.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class PacketDataOutput extends OutputStream implements DataOutput {

	private final Packet packet;

	/**
	 * Constructs a <code>PacketDataOutput</code> with the specified
	 * <code>Packet</code>.
	 * 
	 * @param packet
	 *            The <code>Packet</code> to write data to.
	 */
	public PacketDataOutput(Packet packet) {
		this.packet = packet;
	}

	@Override
	public void write(int b) throws IOException {
		packet.writeByte(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		packet.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		for (int i = off; i < len; i++) {
			packet.writeByte(b[i]);
		}
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		packet.writeBoolean(v);
	}

	@Override
	public void writeByte(int v) throws IOException {
		packet.writeByte(v);
	}

	@Override
	public void writeShort(int v) throws IOException {
		packet.writeShort(v);
	}

	@Override
	public void writeChar(int v) throws IOException {
		packet.writeUnsignedShort(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		packet.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		packet.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		packet.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		packet.writeDouble(v);
	}

	@Override
	public void writeBytes(String s) throws IOException {
		packet.write(s.getBytes());
	}

	@Override
	public void writeChars(String s) throws IOException {
		for (char c : s.toCharArray()) {
			packet.writeUnsignedShort(c);
		}
	}

	@Override
	public void writeUTF(String s) throws IOException {
		packet.writeString(s);
	}

}
