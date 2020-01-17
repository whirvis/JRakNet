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
package com.whirvis.jraknet.windows;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A command that can be executed in the Windows PowerShell environment under
 * the Windows 10 operating system.
 * <p>
 * Commands can be created and executed on devices that do not have Windows
 * PowerShell. However, they will ultimately not be executed.
 * 
 * @author Trent Summerlin
 * @since JRakNet v2.10.0
 */
public final class PowerShellCommand {

	private static final String POWERSHELL_EXECUTABLE = "powershell.exe";
	private static final Charset POWERSHELL_BASE64_CHARSET = Charset.forName("UTF-16LE");
	private static final char END_OF_TEXT = (char) 0x03;
	private static final int POWERSHELL_ADMINISTRATIVE_TIMEOUT = 10000;
	private static final int AUTHENTICATION_FAILURE = 0x00;
	private static final int AUTHENTICATION_SUCCESS = 0x01;
	private static final int STATE_AUTHENTICATION = 0x00;
	private static final int STATE_ERROR_RESULT = 0x01;
	private static final int STATE_RESULT = 0x02;

	/**
	 * The argument prefix.
	 */
	public static final String ARGUMENT_PREFIX = "$";

	/**
	 * Command execution was successful.
	 */
	public static final String RESULT_OK = "OK.";

	/**
	 * PowerShell is not installed on the host machine.
	 */
	public static final String RESULT_NO_POWERSHELL_INSTALLED = "No PowerShell installed.";

	/**
	 * Command execution was a failure.
	 */
	public static final String RESULT_COMMAND_EXECUTION_FAILED = "Command execution failed.";

	/**
	 * Command execution was successful, however getting the results was a
	 * failure.
	 */
	public static final String RESULT_COMMAND_SUCCEEDED_FAILED_TO_GET_RESULT = "Command succeeded, but failed to get result.";

	/**
	 * Executing with administrative privileges was a failure.
	 */
	public static final String RESULT_ADMINISTRATIVE_EXECUTION_FAILED = "Failed to execute with administrative privileges.";

	/**
	 * Returns the location the program is being run at.
	 * 
	 * @return the location the program is being run at, <code>null</code> if
	 *         determining the location was a failure.
	 */
	private static File getRunningLocation() {
		try {
			return new File(PowerShellCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			return null; // Failed to determine location
		}
	}

	/**
	 * Returns the currently running JAR file.
	 * 
	 * @return the currently running JAR file, <code>null</code> if the
	 *         application is not being run from a JAR.
	 * @see #getRunningLocation()
	 */
	private static File getRunningJarFile() {
		File runningJar = getRunningLocation();
		if (runningJar.isDirectory() || !runningJar.getName().endsWith(".jar")) {
			return null; // Not a JAR file
		}
		return runningJar;
	}

	/**
	 * Converts the specified {@link InputStream} to a string. This will result
	 * in the closing of the stream, as all available data will be read from it
	 * during conversion.
	 * 
	 * @param in
	 *            the stream to convert.
	 * @return the converted string.
	 * @throws NullPointerException
	 *             if the <code>in</code> stream is <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	private static String ioStr(InputStream in) throws NullPointerException, IOException {
		if (in == null) {
			throw new NullPointerException("Input stream cannot be null");
		}
		String str = new String();
		String next = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		while ((next = reader.readLine()) != null) {
			str += next + "\n";
		}
		reader.close();
		if (str.length() > 1) {
			return str.substring(0, str.length() - 1);
		}
		return str;
	}

	private static int commandIndex;

	private final Logger logger;
	private final String command;
	private final HashMap<String, String> arguments;

	/**
	 * Creates a PowerShell command that can be executed.
	 * 
	 * @param command
	 *            the command string. To allow for the use of arguments, use
	 *            {@value #ARGUMENT_PREFIX} before the argument name.
	 */
	public PowerShellCommand(String command) {
		this.logger = LogManager.getLogger("PowerShellCommand-" + commandIndex++);
		this.command = command;
		this.arguments = new HashMap<String, String>();
	}

	/**
	 * Returns the command string.
	 * 
	 * @return the command string.
	 */
	public String getCommand() {
		return this.command;
	}

	/**
	 * Sets an argument.
	 * 
	 * @param argumentName
	 *            the argument name.
	 * @param value
	 *            the value.
	 * @return the command.
	 * @throws IllegalArgumentException
	 *             if the <code>argumentName</code> does not begin with
	 *             {@value #ARGUMENT_PREFIX}.
	 */
	public PowerShellCommand setArgument(String argumentName, Object value) throws IllegalArgumentException {
		if (!argumentName.startsWith(ARGUMENT_PREFIX)) {
			throw new IllegalArgumentException("Argument name must begin with the argument prefix");
		}
		StringBuilder valueStr = new StringBuilder();
		valueStr.append(value == null ? "null" : value);
		if (valueStr.toString().contains(ARGUMENT_PREFIX)) {
			throw new IllegalArgumentException("Value may not contain argument prefix");
		}
		arguments.put(argumentName, valueStr.toString());
		logger.debug("Set \"" + argumentName + "\" value to " + valueStr.toString());
		return this;
	}

	/**
	 * Returns the value of the argument.
	 * 
	 * @param argumentName
	 *            the argument name.
	 * @return the value of the argument, <code>null</code> if the argument has
	 *         not been set.
	 * @throws IllegalArgumentException
	 *             if the <code>argumentName</code> does not begin with the
	 *             {@value #ARGUMENT_PREFIX} character.
	 */
	public String getArgument(String argumentName) throws IllegalArgumentException {
		if (!argumentName.startsWith(ARGUMENT_PREFIX)) {
			throw new IllegalArgumentException("Argument name must begin with the argument prefix");
		}
		return arguments.get(argumentName);
	}

	/**
	 * Executes the command. Once the command has been executed, its arguments
	 * will be cleared so they do not linger in the case the same command with
	 * different arguments is executed.
	 * <p>
	 * Take note that a <code>PowerShellException</code> not being thrown is not
	 * an indication that the command actually executed. Rather, it just means
	 * that the execution of this method in particular did not fail. The main
	 * case of this is being a command not being executed either because the
	 * machine is not running on Windows 10 or that it requires elevation but
	 * the user declined.
	 * 
	 * @param requiresElevation
	 *            <code>true</code> if the PowerShell command should be executed
	 *            under an elevated process, <code>false</code> otherwise.
	 * @return the execution result.
	 * @throws PowerShellException
	 *             if a PowerShell error occurs.
	 */
	public synchronized String execute(boolean requiresElevation) throws PowerShellException {
		// Create encoded command with arguments
		String command = POWERSHELL_EXECUTABLE + " -EncodedCommand ";
		String encodedCommand = this.command;
		for (String argumentKey : arguments.keySet()) {
			encodedCommand = encodedCommand.replace(argumentKey, arguments.get(argumentKey));
		}
		arguments.clear();
		command += Base64.getEncoder().encodeToString(encodedCommand.getBytes(POWERSHELL_BASE64_CHARSET));

		if (requiresElevation == false) {
			// Create process and execute command
			Process powerShell = null;
			try {
				powerShell = Runtime.getRuntime().exec(command);
				powerShell.getOutputStream().close();
				powerShell.waitFor();
				if (powerShell.exitValue() != 0) {
					return RESULT_COMMAND_EXECUTION_FAILED;
				}
			} catch (IOException | InterruptedException e) {
				return RESULT_NO_POWERSHELL_INSTALLED;
			}

			// Get result
			try {
				String error = ioStr(powerShell.getErrorStream()).trim();
				if (!error.isEmpty()) {
					throw new PowerShellException(error);
				}
				return ioStr(powerShell.getInputStream()).trim();
			} catch (IOException e) {
				return RESULT_COMMAND_SUCCEEDED_FAILED_TO_GET_RESULT;
			}
		} else {
			try {
				// Create server
				logger.debug("Creating PowerShell administrative server...");
				ServerSocket server = new ServerSocket(0);
				server.setSoTimeout(POWERSHELL_ADMINISTRATIVE_TIMEOUT);
				int state = 0;
				long password = new Random().nextLong();
				long startTime = System.currentTimeMillis();
				logger.debug("Created PowerShell administrative server  with password " + password + " on port "
						+ server.getLocalPort());

				// Create client process
				logger.debug("Executing administrative PowerShell command...");
				String administrativeCommand = PowerShellCommand.POWERSHELL_EXECUTABLE
						+ " Start-Process -Verb runAs javaw.exe \'" + "-cp \"$path\" "
						+ PowerShellAdministrativeClient.class.getName() + " " + server.getLocalPort() + " " + password
						+ " " + command + END_OF_TEXT + "\'";
				if (getRunningJarFile() != null) {
					administrativeCommand = administrativeCommand.replace("$path",
							getRunningJarFile().getAbsolutePath());
				} else {
					administrativeCommand = administrativeCommand.replace("$path",
							getRunningLocation().getAbsolutePath());
				}
				Process powerShell = Runtime.getRuntime().exec(administrativeCommand);
				powerShell.getOutputStream().close();
				powerShell.getErrorStream().close();
				powerShell.getInputStream().close();
				powerShell.waitFor();
				if (powerShell.exitValue() != 0) {
					logger.debug("Failed to execute administrative PowerShell command");
					server.close();
					return RESULT_COMMAND_EXECUTION_FAILED;
				}
				logger.debug("Executed administrative PowerShell command");

				// Wait for connection
				logger.debug("Waiting for connection from administrative PowerShell client...");
				Socket connection = null;
				try {
					connection = server.accept();
				} catch (IOException e) {
					server.close();
					return RESULT_ADMINISTRATIVE_EXECUTION_FAILED;
				}
				DataInputStream connectionIn = new DataInputStream(connection.getInputStream());
				DataOutputStream connectionOut = new DataOutputStream(connection.getOutputStream());
				logger.debug("Administrative PowerShell client connected, waiting for password...");
				while (System.currentTimeMillis() - startTime <= POWERSHELL_ADMINISTRATIVE_TIMEOUT) {
					Thread.sleep(0, 1); // Lower CPU usage
					if (state == STATE_AUTHENTICATION && connectionIn.available() >= Long.BYTES) {
						long givenPassword = connectionIn.readLong();
						if (password == givenPassword) {
							connectionOut.writeInt(AUTHENTICATION_SUCCESS);
							connectionOut.flush();
							state = STATE_ERROR_RESULT;
							logger.debug(
									"Administrative PowerShell client has authenticated, waiting for error results...");
						} else {
							connectionOut.writeInt(AUTHENTICATION_FAILURE);
							connectionOut.flush();
							logger.error("Administrative PowerShell client failed to authenticate");
						}
					} else if (state == STATE_ERROR_RESULT && connectionIn.available() >= 3) {
						String errorResponse = connectionIn.readUTF().trim();
						if (errorResponse.length() > 0) {
							server.close();
							return errorResponse;
						}
						state = STATE_RESULT;
						logger.debug("Administrative PowerShell client has sent error results, waiting for results...");
					} else if (state == STATE_RESULT && connectionIn.available() >= 3) {
						server.close();
						return connectionIn.readUTF().trim();
					}
				}
				connection.close();
				server.close();
				logger.debug("Destroyed adminstrative PowerShell server");
				return RESULT_ADMINISTRATIVE_EXECUTION_FAILED;
			} catch (IOException | InterruptedException e) {
				return RESULT_ADMINISTRATIVE_EXECUTION_FAILED;
			}
		}
	}

	/**
	 * Executes the command. Once the command has been executed, its arguments
	 * will be cleared so they do not linger in the case the same command with
	 * different arguments is executed.
	 * <p>
	 * Take note that a <code>PowerShellException</code> not being thrown is not
	 * an indication that the command actually executed. Rather, it just means
	 * that the execution of this method in particular did not fail. The main
	 * case of this is being a command not being executed either because the
	 * machine is not running on Windows 10 or that it requires elevation but
	 * the user declined.
	 * 
	 * @return the execution result.
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
