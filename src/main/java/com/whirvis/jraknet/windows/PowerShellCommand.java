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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a command that can be executed in the Windows PowerShell
 * environment under the Windows 10 operating system. Commands can be created
 * and executed on devices that do not have Windows PowerShell, however they
 * will fail silently.
 * 
 * @author Trent Summerlin
 */
public class PowerShellCommand {

	private static final Logger log = LoggerFactory.getLogger(PowerShellCommand.class);

	// PowerShell data
	public static final String POWERSHELL_EXECUTABLE = "powershell.exe";
	public static final Charset POWERSHELL_BASE64_CHARSET = Charset.forName("UTF-16LE");
	public static final char ARGUMENT_PREFIX = '$';

	// PowerShell result messages
	public static final String RESULT_OK = "OK.";
	public static final String RESULT_NO_POWERSHELL_INSTALLED = "No PowerShell installed.";
	public static final String RESULT_COMMAND_EXECUTION_FAILED = "Command execution failed";
	public static final String RESULT_COMMAND_SUCCEEDED_FAILED_TO_GET_RESULT = "Command succeeded, but failed to get result.";
	public static final String RESULT_ADMINISTRATIVE_EXECUTION_FAILED = "Failed to execute with administrative privileges";

	// PowerShell administrative client data
	private static final char END_OF_TEXT = (char) 0x03;
	private static final int POWERSHELL_ADMINISTRATIVE_TIMEOUT = 10000;

	/**
	 * @return the location the program is being run at.
	 */
	private static File getRunningLocation() {
		try {
			return new File(PowerShellCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			log.error("Failed to determine running location");
			return null;
		}
	}

	/**
	 * @return the current running JAR file, or <code>null</code> if the
	 *         application is not being run from a JAR.
	 */
	private static File getRunningJarFile() {
		File runningJar = getRunningLocation();
		if (runningJar.isDirectory() || !runningJar.getName().endsWith(".jar")) {
			return null; // Not a JAR file
		}
		return runningJar;
	}

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

	// PowerShell command data
	private final String command;
	private final HashMap<String, String> arguments;

	public PowerShellCommand(String command) {
		this.command = command;
		this.arguments = new HashMap<String, String>();
	}

	/**
	 * @return the command.
	 */
	public String getCommand() {
		return this.command;
	}

	/**
	 * Sets the specified argument.
	 * 
	 * @param argumentName
	 *            the argument name.
	 * @param value
	 *            the value.
	 * @return the command.
	 */
	public PowerShellCommand setArgument(String argumentName, Object value) {
		// Validate argument name
		if (!argumentName.startsWith(Character.toString(ARGUMENT_PREFIX))) {
			throw new IllegalArgumentException("Argument name must begin with the argument prefix");
		}

		// Convert value to string
		String valueStr = null;
		if (value instanceof Number) {
			valueStr = ((Number) value).toString();
		} else if (value instanceof Boolean) {
			valueStr = ((Boolean) value).toString();
		} else if (value instanceof Character) {
			valueStr = ((Character) value).toString();
		} else {
			valueStr = value.toString();
		}

		// Validate and set value
		if (valueStr.contains(Character.toString(ARGUMENT_PREFIX))) {
			throw new IllegalArgumentException("Value may not contain argument prefix");
		}
		arguments.put(argumentName, valueStr);
		log.debug("Set \"" + argumentName + "\" value to " + valueStr);
		return this;
	}

	/**
	 * @param argumentName
	 *            the argument name.
	 * @return the value of the argument with the specified name,
	 *         <code>null</code> if the argument has not been set.
	 */
	public String getArgument(String argumentName) {
		if (!argumentName.startsWith(Character.toString(ARGUMENT_PREFIX))) {
			throw new IllegalArgumentException("Argument name must begin with the argument prefix");
		}
		return arguments.get(argumentName);
	}

	/**
	 * Executes the command. Once the command has been executed, its arguments
	 * will be cleared so they do not linger in the case the same command with
	 * different arguments is executed. Take note that just because a
	 * <code>PowerShellException</code> was not thrown, it does not mean that
	 * the command did what you expected. A prime example would be a command
	 * being parsed correctly but not being executed as it requires elevation.
	 * 
	 * @param requiresElevation
	 *            <code>true</code> if the PowerShell command should be executed
	 *            under an elevated process, <code>false</code> otherwise.
	 * @return the command result.
	 * @throws PowerShellException
	 *             if a PowerShell error occurs.
	 */
	public synchronized String execute(boolean requiresElevation) throws PowerShellException {
		// Create command
		String command = POWERSHELL_EXECUTABLE + " -EncodedCommand ";
		String encodedCommand = this.command;
		for (String argumentKey : arguments.keySet()) {
			encodedCommand = encodedCommand.replace(argumentKey, arguments.get(argumentKey));
		}
		arguments.clear(); // Clear the arguments
		log.debug("Cleared arguments for PowerShell command \"" + this.command + "\"");
		command += Base64.getEncoder().encodeToString(encodedCommand.getBytes(POWERSHELL_BASE64_CHARSET));

		if (requiresElevation == false) {
			// Create process and execute command
			Process powerShell = null;
			try {
				log.debug("Executing PowerShell command");
				powerShell = Runtime.getRuntime().exec(command);
				powerShell.getOutputStream().close();
				powerShell.waitFor();
				if (powerShell.exitValue() != 0) {
					log.debug("Failed to execute PowerShell command");
					return RESULT_COMMAND_EXECUTION_FAILED;
				}
			} catch (IOException | InterruptedException e) {
				return RESULT_NO_POWERSHELL_INSTALLED;
			}

			// Get result
			try {
				log.debug("Obtaining error and result information");
				String error = ioStr(powerShell.getErrorStream()).trim();
				if (!error.isEmpty()) {
					throw new PowerShellException(error);
				}
				return ioStr(powerShell.getInputStream()).trim();
			} catch (IOException e) {
				log.warn("Failed to get result of PowerShell command");
				return RESULT_COMMAND_SUCCEEDED_FAILED_TO_GET_RESULT;
			}
		} else {
			try {
				// Create server
				ServerSocket administrativePowerShellServerSocket = new ServerSocket(0);
				administrativePowerShellServerSocket.setSoTimeout(POWERSHELL_ADMINISTRATIVE_TIMEOUT);
				int state = 0;
				long password = new Random().nextLong();
				long startTime = System.currentTimeMillis();
				log.debug("Created PowerShell administrative server socket with password " + password + " on port "
						+ administrativePowerShellServerSocket.getLocalPort());

				// Create process and execute command to create client process
				log.debug("Executing administrative PowerShell command");
				String administrativeCommand = PowerShellCommand.POWERSHELL_EXECUTABLE
						+ " Start-Process -Verb runAs javaw.exe \'" + "-cp \"$path\" "
						+ PowerShellAdministrativeClient.class.getName() + " "
						+ administrativePowerShellServerSocket.getLocalPort() + " " + password + " " + command
						+ END_OF_TEXT + "\'";
				if (getRunningJarFile() != null) {
					administrativeCommand = administrativeCommand.replace("$path",
							getRunningJarFile().getAbsolutePath());
				} else {
					administrativeCommand = administrativeCommand.replace("$path",
							getRunningLocation().getAbsolutePath());
				}
				Process administrativePowerShell = Runtime.getRuntime().exec(administrativeCommand);
				administrativePowerShell.getOutputStream().close();
				administrativePowerShell.getErrorStream().close();
				administrativePowerShell.getInputStream().close();
				administrativePowerShell.waitFor();
				if (administrativePowerShell.exitValue() != 0) {
					log.debug("Failed to execute administrative PowerShell command");
					administrativePowerShellServerSocket.close();
					return RESULT_COMMAND_EXECUTION_FAILED;
				}

				// Begin connection loop
				Socket administrativePowerShellConnection = null;
				DataInputStream administrativePowerShellDataIn = null;
				while (System.currentTimeMillis() - startTime <= POWERSHELL_ADMINISTRATIVE_TIMEOUT) {
					Thread.sleep(0, 1); // Lower CPU usage
					if (administrativePowerShellConnection != null && administrativePowerShellDataIn != null) {
						if (administrativePowerShellDataIn.available() >= Long.BYTES && state == 0) {
							long givenPassword = administrativePowerShellDataIn.readLong();
							if (password == givenPassword) {
								state = 1;
							}
							log.debug("Administrative PowerShell client has authenticated");
						} else if (administrativePowerShellDataIn.available() >= 3 && state == 1) {
							String errorResponse = administrativePowerShellDataIn.readUTF().trim();
							if (errorResponse.length() > 0) {
								administrativePowerShellServerSocket.close();
								return errorResponse;
							}
							state = 2;
							log.debug("Administrative PowerShell client has sent error information");
						} else if (administrativePowerShellDataIn.available() >= 3 && state == 2) {
							administrativePowerShellServerSocket.close();
							log.debug("Administrative PowerShell client has sent result information");
							return administrativePowerShellDataIn.readUTF().trim();
						}
					} else {
						// Wait for connection
						administrativePowerShellConnection = administrativePowerShellServerSocket.accept();
						administrativePowerShellDataIn = new DataInputStream(
								administrativePowerShellConnection.getInputStream());
						log.debug("Administrative PowerShell client connected on port "
								+ administrativePowerShellServerSocket.getLocalPort());
					}
				}
				administrativePowerShellServerSocket.close();
				log.debug("Destroyed adminstrative PowerShell server with password " + password + " on port "
						+ administrativePowerShellServerSocket.getLocalPort());
				return RESULT_ADMINISTRATIVE_EXECUTION_FAILED;
			} catch (IOException | InterruptedException e) {
				return RESULT_ADMINISTRATIVE_EXECUTION_FAILED;
			}
		}
	}

	/**
	 * Executes the command. Once the command has been executed, its arguments
	 * will be cleared so they do not linger in the case the same command with
	 * different arguments is executed. Take note that just because a
	 * <code>PowerShellException</code> was not thrown, it does not mean that
	 * the command did what you expected. A prime example would be a command
	 * being parsed correctly but not being executed as it requires elevation.
	 * 
	 * @return the command result.
	 * @throws PowerShellException
	 *             if a PowerShell error occurs.
	 */
	public synchronized String execute() throws PowerShellException {
		return this.execute(false);
	}

	@Override
	public String toString() {
		return "PowerShellCommand [command=" + command + "]";
	}

}
