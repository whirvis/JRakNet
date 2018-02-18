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
package com.whirvis.jraknet.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Used for easily manipulation of arrays.
 *
 * @author Trent "Whirvis" Summerlin
 */
public abstract class ArrayUtils {

	/**
	 * Splits an array into more chunks with the specified maximum size for each
	 * array chunk.
	 * 
	 * @param src
	 *            the original array.
	 * @param size
	 *            the max size for each array that has been split.
	 * @return the split byte array's no bigger than the maximum size.
	 */
	public static final byte[][] splitArray(byte[] src, int size) {
		int index = 0;
		ArrayList<byte[]> split = new ArrayList<byte[]>();
		while (index < src.length) {
			if (index + size <= src.length) {
				split.add(Arrays.copyOfRange(src, index, index + size));
				index += size;
			} else {
				split.add(Arrays.copyOfRange(src, index, src.length));
				index = src.length;
			}
		}
		return split.toArray(new byte[split.size()][size]);
	}

	/**
	 * Returns all the integers in between each other as a normal subtraction.
	 * 
	 * @param low
	 *            the starting point.
	 * @param high
	 *            the ending point.
	 * @return the numbers in between high and low.
	 */
	public static final int[] subtractionArray(int low, int high) {
		if (low > high) {
			return new int[0];
		}

		int[] arr = new int[high - low - 1];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = i + low + 1;
		}
		return arr;
	}

	/**
	 * Convert the specified list of objects to a String. Used primarily by
	 * JRakNet objects to easily convert their data to a readable String.
	 * 
	 * @param obj
	 *            the objects to convert.
	 * @return a converted string.
	 */
	public static final String toJRakNetString(Object... obj) {
		StringBuilder str = new StringBuilder();
		str.append("[");
		for (int i = 0; i < obj.length; i++) {
			str.append((obj[i] instanceof Number ? ((Number) obj[i]).longValue() : obj[i].toString())
					+ (i + 1 < obj.length ? ", " : "]"));
		}
		return str.toString();
	}

}
