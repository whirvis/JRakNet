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
package com.whirvis.jraknet.windows;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Used to execute commands with administrative privileges on Windows machines.
 * <p>
 * This is only ever needed if a command that requires administrative privileges
 * needs to be executed. Here, we must take special care not to use anything
 * that is not in the default Java library, as the way this class is called
 * prevents it from having access to the others libraries normally included with
 * JRakNet.
 * 
 * @author Trent Summerlin
 * @since JRakNet v2.10.0
 */
public final class PowerShellAdministrativeClient {

	private static final char END_OF_TEXT = (char) 0x03;
	private static final int POWERSHELL_ADMINISTRATIVE_TIMEOUT = 10000;
	private static final int AUTHENTICATION_SUCCESS = 0x01;

	private PowerShellAdministrativeClient() {
		// Static class
	}

	/**
	 * Converts the <code>InputStream</code> to a string. This will result in
	 * the closing of the stream, as all available data will be read from it
	 * during conversion.
	 * 
	 * @param in
	 *            the stream to convert.
	 * @return the converted stream.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	private static String ioStr(InputStream in) throws IOException {
		String str = new String();
		String next = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		while ((next = reader.readLine()) != null) {
			str += next + "\n";
		}
		in.close();
		if (str.length() > 1) {
			return str.substring(0, str.length() - 1);
		}
		return str;
	}

	/**
	 * The main method for the administrative PowerShell process. This must act
	 * as a main method since it is called through the JVM as a normal process.
	 * 
	 * @param args
	 *            the arguments. The first should be the port the PowerShell
	 *            server is listening on, with the second being the password,
	 *            and the final being the command itself terminated by an
	 *            <code>ETX 0x03</code> character.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static void main(String[] args) throws IOException {
		Socket client = null;
		try {
			// Parse arguments
			System.out.println("Parsing arguments...");
			int i = 0;
			int port = Integer.parseInt(args[i++]);
			long password = Long.parseLong(args[i++]);
			StringBuilder commandBuilder = new StringBuilder();
			commandLoop: for (/* Already declared */; i < args.length; i++) {
				for (int j = 0; j < args[i].length(); j++) {
					if (args[i].charAt(j) == END_OF_TEXT) {
						break commandLoop;
					}
					commandBuilder.append(args[i].charAt(j));
				}
				commandBuilder.append(i + 1 < args.length ? " " : "");
			}
			System.out.println("Parsed arguments!");

			// Connect to server
			System.out.println("Connecting to server on port " + port + "...");
			client = new Socket("127.0.0.1", port);
			client.setSoTimeout(POWERSHELL_ADMINISTRATIVE_TIMEOUT);
			DataInputStream clientIn = new DataInputStream(client.getInputStream());
			DataOutputStream clientOut = new DataOutputStream(client.getOutputStream());
			System.out.println("Connected to server");

			// Authorize connection
			System.out.println("Authenticating with password " + password + "...");
			clientOut.writeLong(password);
			clientOut.flush();
			int authenticationResult = clientIn.readInt();
			if (authenticationResult == AUTHENTICATION_SUCCESS) {
				System.out.println("Authenticated with server");
			} else {
				System.err.println("Failed to authenticate with server");
				clientIn.close();
				clientOut.close();
				client.close();
				return;
			}

			// Execute command
			System.out.println("Executing administrative PowerShell command");
			Process powerShell = Runtime.getRuntime().exec(commandBuilder.toString());
			powerShell.getOutputStream().close();
			powerShell.waitFor();
			powerShell.destroyForcibly();
			System.out.println("Executed administrative PowerShell command");

			// Send command output
			System.out.println("Sending PowerShell command output...");
			clientOut.writeUTF(ioStr(powerShell.getErrorStream()).trim());
			clientOut.flush();
			clientOut.writeUTF(ioStr(powerShell.getInputStream()).trim());
			clientOut.flush();
			System.out.println("Sent PowerShell command output");

			// Shutdown client
			System.out.println("Shutting down client...");
			clientOut.close();
			client.close();
			System.out.println("Shutdown client");
		} catch (Exception e) {
			e.printStackTrace();
			if (client != null) {
				client.close();
			}
			System.exit(1);
		}
	}

}
