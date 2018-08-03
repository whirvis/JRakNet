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
 * Copyright (c) 2016-2018 Trent Summerlin
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
package com.whirvis.jraknet.windows;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Used to execute commands with administrative privileges on Windows machines.
 * This is only ever needed if a command that requires administrative privileges
 * needs to be executed. Here, we must take special care not to use anything
 * that is not in the default Java library, as the way this class is called
 * prevents it from having access to the others libraries normally included with
 * JRakNet.
 * 
 * @author Trent Summerlin
 */
public class PowerShellAdministrativeClient {

	// PowerShell administrative server data
	private static final char END_OF_TEXT = (char) 0x03;
	private static final int POWERSHELL_ADMINISTRATIVE_TIMEOUT = 10000;

	/**
	 * Converts the specified <code>InputStream</code> to a <code>String</code>.
	 * This will result in the closing of the stream, as all available data will
	 * be read from it during conversion.
	 * 
	 * @param in
	 *            the stream to convert.
	 * @return the converted stream.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	private static String ioStr(InputStream in) throws IOException {
		// Read input
		String str = new String();
		String next = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		while ((next = reader.readLine()) != null) {
			str += next + "\n";
		}
		in.close();

		// Convert result accordingly
		if (str.length() > 1) {
			return str.substring(0, str.length() - 1);
		}
		return str;
	}

	/**
	 * This is the main method for the administrative PowerShell process. This
	 * must act as a main method since it is called through the JVM as a normal
	 * process.
	 * 
	 * @param args
	 *            the arguments. The first should be the port the PowerShell
	 *            server is listening on, the second is the password, and the
	 *            final argument is the command itself terminated by a ETX
	 *            (<code>0x03</code>) character.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static void main(String[] args) throws IOException {
		Socket administrativePowerShellClientSocket = null;
		try {
			// Get arguments
			System.out.println("Parsing arguments for administrative PowerShell communication");
			int port = Integer.parseInt(args[0]);
			long password = Long.parseLong(args[1]);
			String command = new String();
			for (int i = 2; i < args.length; i++) {
				for (char c : args[i].toCharArray()) {
					if (c == END_OF_TEXT) {
						break;
					}
					command += c;
				}
				command += (i + 1 < args.length ? " " : "");
			}

			// Connect to server
			System.out.println("Connecting to server on port " + port);
			administrativePowerShellClientSocket = new Socket("127.0.0.1", port);
			administrativePowerShellClientSocket.setSoTimeout(POWERSHELL_ADMINISTRATIVE_TIMEOUT);
			DataOutputStream administrativePowerShellDataOut = new DataOutputStream(
					administrativePowerShellClientSocket.getOutputStream());

			// Authorize connection
			System.out.println("Authenticating with password " + password);
			administrativePowerShellDataOut.writeLong(password);
			administrativePowerShellDataOut.flush();

			// Execute command
			System.out.println("Executing administrative PowerShell command");
			Process administrativePowerShell = Runtime.getRuntime().exec(command);
			administrativePowerShell.getOutputStream().close();
			administrativePowerShell.waitFor();

			// Send command output
			System.out.println("Sending PowerShell command output");
			administrativePowerShellDataOut.writeUTF(ioStr(administrativePowerShell.getErrorStream()));
			administrativePowerShellDataOut.flush();
			administrativePowerShellDataOut.writeUTF(ioStr(administrativePowerShell.getInputStream()));
			administrativePowerShellDataOut.flush();

			// Shutdown client
			System.out.println("Shutting down client...");
			administrativePowerShellDataOut.close();
			administrativePowerShellClientSocket.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			if (administrativePowerShellClientSocket != null) {
				administrativePowerShellClientSocket.close();
			}
			System.exit(1);
		}
	}

}
