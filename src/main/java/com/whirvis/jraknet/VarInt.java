/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Trent Summerlin
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
package com.whirvis.jraknet;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utilites for reading and writing both <code>VarInt</code>s and
 * <code>VarLong</code>s.
 * 
 * @author Trent Summerlin
 * @since JRakNet v2.11.4
 */
public class VarInt {

	/**
	 * The maximum amount of bits a <code>VarInt</code> can be.
	 */
	public static final int VARINT_MAX_SIZE = 35;

	/**
	 * The maximum amount of bits a <code>VarLong</code> can be.
	 */
	public static final int VARLONG_MAX_SIZE = 70;

	/**
	 * Reads a <code>VarInt</code> from the specified {@link InputStream}.
	 * 
	 * @param in
	 *            the input stream to read from.
	 * @param max
	 *            the maximum amount of bits the <code>VarInt</code> can be.
	 * @return a <code>VarInt</code>.
	 * @throws NullPointerException
	 *             if the <code>in</code> stream is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>max</code> bits is less than or equal to
	 *             <code>0</code> or is greater than {@value #VARLONG_MAX_SIZE}.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarInt</code> exceeds the <code>max</code>
	 *             amount of bits.
	 */
	public static long read(InputStream in, int max) throws IOException {
		if (in == null) {
			throw new NullPointerException("Input stream cannot be null");
		} else if (max <= 0) {
			throw new IllegalArgumentException("Max bits must be greater than 0");
		} else if (max > VARLONG_MAX_SIZE) {
			throw new IllegalArgumentException("Max bits can be no greater than " + VARLONG_MAX_SIZE);
		}
		int result = 0;
		int shift = 0;
		int bits;
		do {
			if (shift >= max) {
				throw new IndexOutOfBoundsException("VarInt overflow");
			}
			bits = in.read();
			if (bits < 0) {
				throw new EOFException("VarInt underflow");
			}
			result |= ((bits & 0x7F) << shift);
			shift += 7;
		} while ((bits & 0x80) > 0);
		return result;
	}

	/**
	 * Writes a <code>VarInt</code> to the given {@link OutputStream}.
	 * 
	 * @param l
	 *            the value to write.
	 * @param out
	 *            the output stream to write to.
	 * @param max
	 *            the maximum amount of bits the <code>VarInt</code> can be.
	 * @throws NullPointerException
	 *             if the <code>out</code> stream is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>max</code> bits is less than or equal to
	 *             <code>0</code> or is greater than {@value #VARLONG_MAX_SIZE}.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarInt</code> exceeds the <code>max</code>
	 *             amount of bits.
	 */
	public static void write(long l, OutputStream out, int max) throws NullPointerException, IllegalArgumentException, IOException, IndexOutOfBoundsException {
		if (out == null) {
			throw new NullPointerException("Output stream cannot be null");
		} else if (max <= 0) {
			throw new IllegalArgumentException("Max bits must be greater than 0");
		} else if (max > VARLONG_MAX_SIZE) {
			throw new IllegalArgumentException("Max bits can be no greater than " + VARLONG_MAX_SIZE);
		}
		boolean more = true;
		int shift = 0;
		while (more == true) {
			if (max != VARLONG_MAX_SIZE && shift >= max) {
				throw new IndexOutOfBoundsException("VarInt overflow");
			}
			long bits = (long) (l >>> shift) & 0x7F;
			shift += 7;
			if (shift >= VARLONG_MAX_SIZE || (l >>> shift == 0)) {
				more = false;
			}
			out.write((int) (bits | (more == true ? 0x80 : 0x00)));
		}
	}

	/**
	 * Writes a <code>VarInt</code> to the given {@link OutputStream}.
	 * 
	 * @param i
	 *            the value to write.
	 * @param out
	 *            the output stream to write to.
	 * @param max
	 *            the maximum amount of bits the <code>VarInt</code> can be.
	 * @throws NullPointerException
	 *             if the <code>out</code> stream is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>max</code> bits is less than or equal to
	 *             <code>0</code> or is greater than {@value #VARINT_MAX_SIZE}.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarInt</code> exceeds the <code>max</code>
	 *             amount of bits.
	 */
	public static void write(int i, OutputStream out, int max) throws NullPointerException, IllegalArgumentException, IOException, IndexOutOfBoundsException {
		if (out == null) {
			throw new NullPointerException("Output stream cannot be null");
		} else if (max <= 0) {
			throw new IllegalArgumentException("Max bits must be greater than 0");
		} else if (max > VARINT_MAX_SIZE) {
			throw new IllegalArgumentException("Max bits can be no greater than " + VARINT_MAX_SIZE);
		}
		boolean more = true;
		int shift = 0;
		while (more == true) {
			if (max != VARINT_MAX_SIZE && shift >= max) {
				throw new IndexOutOfBoundsException("VarInt overflow");
			}
			int bits = (i >>> shift) & 0x7F;
			shift += 7;
			if (shift >= VARINT_MAX_SIZE || (i >>> shift == 0)) {
				more = false;
			}
			out.write(bits | (more == true ? 0x80 : 0x00));
		}
	}

	/**
	 * Reads a <code>VarInt</code> from the specified {@link InputStream}.
	 * 
	 * @param in
	 *            the input stream to read from.
	 * @return a <code>VarInt</code>.
	 * @throws NullPointerException
	 *             if the <code>in</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarInt</code> exceeds {@value #VARINT_MAX_SIZE}
	 *             bits.
	 */
	public static int readVarInt(InputStream in) throws NullPointerException, IOException, IndexOutOfBoundsException {
		return (int) read(in, VARINT_MAX_SIZE);
	}

	/**
	 * Writes a <code>VarInt</code> to the given {@link OutputStream}.
	 * 
	 * @param i
	 *            the value to write.
	 * @param out
	 *            the output stream to write to.
	 * @throws NullPointerException
	 *             if the <code>out</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarInt</code> exceeds {@value #VARINT_MAX_SIZE}
	 *             bits.
	 */
	public static void writeVarInt(int i, OutputStream out) throws NullPointerException, IOException, IndexOutOfBoundsException {
		write(i, out, VARINT_MAX_SIZE);
	}

	/**
	 * Reads a <code>VarLong</code> from the specified {@link InputStream}.
	 * 
	 * @param in
	 *            the input stream to read from.
	 * @return a <code>VarLong</code>.
	 * @throws NullPointerException
	 *             if the <code>in</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarLong</code> exceeds
	 *             {@value #VARLONG_MAX_SIZE} bits.
	 */
	public static long readVarLong(InputStream in) throws IOException {
		return read(in, VARLONG_MAX_SIZE);
	}

	/**
	 * Writes a <code>VarLong</code> to the given {@link OutputStream}.
	 * 
	 * @param l
	 *            the value to write.
	 * @param out
	 *            the output stream to write to.
	 * @throws NullPointerException
	 *             if the <code>out</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarLong</code> exceeds
	 *             {@value #VARLONG_MAX_SIZE} bits.
	 */
	public static void writeVarLong(long l, OutputStream out) throws NullPointerException, IOException, IndexOutOfBoundsException {
		write(l, out, VARLONG_MAX_SIZE);
	}

	/**
	 * Reads an unsigned <code>VarInt</code> from the specified
	 * {@link InputStream}.
	 * 
	 * @param in
	 *            the input stream to read from.
	 * @param max
	 *            the maximum amount of bits the <code>VarInt</code> can be.
	 * @return an unsigned <code>VarInt</code>.
	 * @throws NullPointerException
	 *             if the <code>in</code> stream is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>max</code> bits is less than or equal to
	 *             <code>0</code> or is greater than {@value #VARLONG_MAX_SIZE}.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarInt</code> exceeds the <code>max</code>
	 *             amount of bits.
	 */
	public static long readUnsigned(InputStream in, int max) throws NullPointerException, IllegalArgumentException, IOException, IndexOutOfBoundsException {
		if (in == null) {
			throw new NullPointerException("Input stream cannot be null");
		} else if (max <= 0) {
			throw new IllegalArgumentException("Max bits must be greater than 0");
		} else if (max > VARLONG_MAX_SIZE) {
			throw new IllegalArgumentException("Max bits can be no greater than " + VARLONG_MAX_SIZE);
		}
		long result = 0;
		int shift = 0;
		int bits;
		do {
			if (shift >= max) {
				throw new IndexOutOfBoundsException("VarInt overflow");
			}
			bits = in.read();
			if (bits < 0) {
				throw new EOFException("VarInt underflow");
			}
			result |= (long) (bits & 0x7F) << shift;
			shift += 7;
		} while ((bits & 0x80) > 0);
		return result;
	}

	/**
	 * Writes an unsigned <code>VarInt</code> to the given {@link OutputStream}.
	 * 
	 * @param l
	 *            the value to write.
	 * @param out
	 *            the output stream to write to.
	 * @param max
	 *            the maximum amount of bits the <code>VarInt</code> can be.
	 * @throws IllegalArgumentException
	 *             if the value is negative or the <code>max</code> bits is less
	 *             than or equal to <code>0</code> or is greater than
	 *             {@value #VARLONG_MAX_SIZE}.
	 * @throws NullPointerException
	 *             if the <code>out</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarInt</code> exceeds the <code>max</code>
	 *             amount of bits.
	 */
	public static void writeUnsigned(long l, OutputStream out, int max) throws IllegalArgumentException, NullPointerException, IOException, IndexOutOfBoundsException {
		if (l < 0) {
			throw new IllegalArgumentException("Value cannot be negative");
		} else if (out == null) {
			throw new NullPointerException("Output stream cannot be null");
		} else if (max < 0) {
			throw new IllegalArgumentException("Max bits must be greater than 0");
		} else if (max > VARLONG_MAX_SIZE) {
			throw new IllegalArgumentException("Max bits can be no greater than " + VARLONG_MAX_SIZE);
		}
		int shift = 0;
		boolean moreBits = true;
		do {
			if (shift >= max) {
				throw new IndexOutOfBoundsException("VarInt overflow");
			}
			long bits = (long) (l >>> shift) & 0x7F;
			moreBits = ((l >>> (shift + 7)) & 0x7F) != 0;
			out.write((int) (bits | (moreBits ? 0x80 : 0x00)));
			shift += 7;
		} while (moreBits);
	}

	/**
	 * Reads an unsigned <code>VarInt</code> from the specified
	 * {@link InputStream}.
	 * 
	 * @param in
	 *            the input stream to read from.
	 * @return an unsigned <code>VarInt</code>.
	 * @throws NullPointerException
	 *             if the <code>in</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if the <code>VarInt</code> exceeds {@value #VARINT_MAX_SIZE}
	 *             bits.
	 */
	public static long readUnsignedVarInt(InputStream in) throws NullPointerException, IOException, IndexOutOfBoundsException {
		return readUnsigned(in, VARINT_MAX_SIZE);
	}

	/**
	 * Writes an unsigned <code>VarInt</code> to the given {@link OutputStream}.
	 * 
	 * @param i
	 *            the value to write.
	 * @param out
	 *            the output stream to write to.
	 * @throws IllegalArgumentException
	 *             if the value is negative.
	 * @throws NullPointerException
	 *             if the <code>out</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if more than {@value #VARINT_MAX_SIZE} bits are written.
	 */
	public static void writeUnsignedVarInt(int i, OutputStream out) throws IllegalArgumentException, NullPointerException, IOException, IndexOutOfBoundsException {
		writeUnsigned(i, out, VARINT_MAX_SIZE);
	}

	/**
	 * Reads an unsigned <code>VarLong</code> from the specified
	 * {@link InputStream}.
	 * 
	 * @param in
	 *            the input stream to read from.
	 * @return an unsigned <code>VarLong</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>value</code> is negative.
	 * @throws NullPointerException
	 *             if the <code>in</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if more than {@value #VARLONG_MAX_SIZE} bits are read.
	 */
	public static long readUnsignedVarLong(InputStream in) throws NullPointerException, IOException, IndexOutOfBoundsException {
		return readUnsigned(in, VARLONG_MAX_SIZE);
	}

	/**
	 * Writes an unsigned <code>VarLong</code> to the given
	 * {@link OutputStream}.
	 * 
	 * @param l
	 *            the value to write.
	 * @param out
	 *            the output stream to write to.
	 * @throws IllegalArgumentException
	 *             if the value is negative.
	 * @throws NullPointerException
	 *             if the <code>out</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws IndexOutOfBoundsException
	 *             if more than {@value #VARLONG_MAX_SIZE} bits are written.
	 */
	public static void writeUnsignedVarLong(long l, OutputStream out) throws IllegalArgumentException, NullPointerException, IOException, IndexOutOfBoundsException {
		writeUnsigned(l, out, VARLONG_MAX_SIZE);
	}

}
