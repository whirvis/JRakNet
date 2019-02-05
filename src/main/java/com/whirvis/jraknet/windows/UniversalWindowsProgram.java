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
package com.whirvis.jraknet.windows;

/**
 * Represents a universal Windows program. This is mainly meant to be used to
 * give universal Windows programs loopback exemption so users can connect to
 * JRakNet servers on the same machine. This class can safely be used on other
 * machines that are not running on the Windows 10 operating system (and is
 * actually encouraged if the game exists as a universal Windows program on
 * Windows, such as Minecraft) without risking crashes due to incompatibilities.
 * However, if the machine is not running Windows 10 then this class is
 * guaranteed to behave differently (code intentionally not running,
 * intentionally giving different results, etc.)
 * 
 * @author Trent "Whirvis" Summerlin
 */
public class UniversalWindowsProgram {

	public static final UniversalWindowsProgram MINECRAFT = new UniversalWindowsProgram(
			"Microsoft.MinecraftUWP_8wekyb3d8bbwe");

	/**
	 * @return <code>true</code> if the machine is currently running on the
	 *         Windows 10 operating system, <code>false</code> otherwise.
	 */
	public static final boolean isWindows10() {
		return System.getProperty("os.name").equalsIgnoreCase("Windows 10");
	}

	// PowerShell commands
	private static final String APPLICATION_ARGUMENT = PowerShellCommand.ARGUMENT_PREFIX + "application";
	private static final PowerShellCommand CHECKNETISOLATION_LOOPBACKEXEMPT_ADD = new PowerShellCommand(
			"CheckNetIsolation LoopbackExempt -a -n=\"" + APPLICATION_ARGUMENT + "\"");
	private static final PowerShellCommand CHECKNETISOLATION_LOOPBACKEXEMPT_DELETE = new PowerShellCommand(
			"CheckNetIsolation LoopbackExempt -d -n=\"" + APPLICATION_ARGUMENT + "\"");
	private static final PowerShellCommand CHECKNETISOLATION_LOOPBACKEXEMPT_SHOW = new PowerShellCommand(
			"CheckNetIsolation LoopbackExempt -s");

	private final String application;

	public UniversalWindowsProgram(String application) {
		this.application = application;
	}

	/**
	 * @return the application.
	 */
	public String getApplication() {
		return this.application;
	}

	/**
	 * @return <code>true</code> if the application is loopback exempt,
	 *         <code>false</code> otherwise.
	 */
	public boolean isLoopbackExempt() {
		if (isWindows10()) {
			String exemptedAppContainers = CHECKNETISOLATION_LOOPBACKEXEMPT_SHOW.execute();
			return exemptedAppContainers.toLowerCase().contains(this.getApplication().toLowerCase());
		} else {
			return true; // Already exempted on non-Windows 10 machine
		}
	}

	/**
	 * Makes the application loopback exempt, allowing it to connect with
	 * applications on the same machine.
	 * 
	 * @return <code>true</code> if the application was successfully made
	 *         exempt, <code>false</code> otherwise.
	 */
	public boolean addLoopbackExempt() {
		if (isWindows10() && !isLoopbackExempt()) {
			return CHECKNETISOLATION_LOOPBACKEXEMPT_ADD.setArgument(APPLICATION_ARGUMENT, this.getApplication())
					.execute(true).equals(PowerShellCommand.RESULT_OK);
		} else {
			return true; // Already exempted on non-Windows 10 machine
		}
	}

	/**
	 * Makes the application loopback unexempt, preventing it from connecting
	 * with applications on the same machine.
	 * 
	 * @return <code>true</code> if the application was successfully made
	 *         unexempt, <code>false</code> otherwise.
	 */
	public boolean deleteLoopbackExempt() {
		if (isWindows10()) {
			if (!isLoopbackExempt()) {
				return true; // Already unexempted
			}
			return CHECKNETISOLATION_LOOPBACKEXEMPT_DELETE.setArgument(APPLICATION_ARGUMENT, this.getApplication())
					.execute(true).equals(PowerShellCommand.RESULT_OK);
		} else {
			return false; // Cannot be unexempted on non-Windows 10 machine
		}
	}

	@Override
	public String toString() {
		return "UniversalWindowsProgram [application=" + application + "]";
	}

}
