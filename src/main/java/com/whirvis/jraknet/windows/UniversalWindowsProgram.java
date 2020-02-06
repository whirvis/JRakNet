/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 Trent Summerlin
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

import java.util.Objects;

/**
 * A universal Windows program.
 * <p>
 * Mainly meant to be used to give universal Windows programs loopback exemption
 * so users can connect to JRakNet servers on the same machine. This class can
 * safely be used on other machines that are not running on the Windows 10
 * operating system without risking crashes due to incompatibilities. However,
 * if the machine is not running Windows 10 then this class is guaranteed to
 * behave differently with code intentionally not running or giving different
 * results.
 * 
 * @author Trent Summerlin
 * @since JRakNet v2.10.0
 */
public final class UniversalWindowsProgram {

	/**
	 * The Minecraft Universal Windows Program.
	 */
	public static final UniversalWindowsProgram MINECRAFT = new UniversalWindowsProgram(
			"Microsoft.MinecraftUWP_8wekyb3d8bbwe");

	private static final String APPLICATION_ARGUMENT = PowerShellCommand.ARGUMENT_PREFIX + "application";
	private static final PowerShellCommand CHECKNETISOLATION_LOOPBACKEXEMPT_ADD = new PowerShellCommand(
			"CheckNetIsolation LoopbackExempt -a -n=\"" + APPLICATION_ARGUMENT + "\"");
	private static final PowerShellCommand CHECKNETISOLATION_LOOPBACKEXEMPT_DELETE = new PowerShellCommand(
			"CheckNetIsolation LoopbackExempt -d -n=\"" + APPLICATION_ARGUMENT + "\"");
	private static final PowerShellCommand CHECKNETISOLATION_LOOPBACKEXEMPT_SHOW = new PowerShellCommand(
			"CheckNetIsolation LoopbackExempt -s");

	/**
	 * Returns whether or not the machine is currently running on the Windows 10
	 * operating system.
	 * 
	 * @return <code>true</code> if the machine is currently running on the
	 *         Windows 10 operating system, <code>false</code> otherwise.
	 */
	public static boolean isWindows10() {
		return System.getProperty("os.name").equalsIgnoreCase("Windows 10");
	}

	private final String applicationId;

	/**
	 * Creates a Universal Windows Program.
	 * 
	 * @param applicationId
	 *            the application ID.
	 */
	public UniversalWindowsProgram(String applicationId) {
		this.applicationId = applicationId;
	}

	/**
	 * Returns the application ID.
	 * 
	 * @return the application ID.
	 */
	public String getApplicationId() {
		return this.applicationId;
	}

	/**
	 * Returns whether or not the application is loopback exempt.
	 * <p>
	 * The term "loopback exempt" means that an application is exempt from the
	 * rule that it cannot connect to a server running on the same machine as it
	 * is.
	 * 
	 * @return <code>true</code> if the application is loopback exempt,
	 *         <code>false</code> otherwise.
	 * @throws PowerShellException
	 *             if a PowerShell error occurs.
	 */
	public boolean isLoopbackExempt() throws PowerShellException {
		if (!isWindows10()) {
			return true; // Already exempted on non-Windows 10 machine
		}
		return CHECKNETISOLATION_LOOPBACKEXEMPT_SHOW.execute().toLowerCase()
				.contains(this.getApplicationId().toLowerCase());
	}

	/**
	 * Sets whether or not the application is loopback exempt.
	 * <p>
	 * The term "loopback exempt" means that an application is exempt from the
	 * rule that it cannot connect to a server running on the same machine as it
	 * is.
	 * 
	 * @param exempt
	 *            <code>true</code> if the application is loopback exempt,
	 *            <code>false</code> otherwise.
	 * @return <code>true</code> if making the application loopback exempt was
	 *         successful, <code>false</code> otherwise. A success means that
	 *         the machine is not running on Windows 10 (no code needed to be
	 *         executed), the exemption status was successfully changed, or that
	 *         the <code>exempt</code> value is already what is now.
	 * @throws PowerShellException
	 *             if a PowerShell error occurs.
	 */
	public boolean setLoopbackExempt(boolean exempt) throws PowerShellException {
		if (!isWindows10()) {
			return true; // Not running on Windows 10
		}
		boolean exempted = isLoopbackExempt();
		if (exempt == true && exempted == false) {
			return CHECKNETISOLATION_LOOPBACKEXEMPT_ADD.setArgument(APPLICATION_ARGUMENT, this.getApplicationId())
					.execute(true).equals(PowerShellCommand.RESULT_OK);
		} else if (exempt == false && exempted == true) {
			return CHECKNETISOLATION_LOOPBACKEXEMPT_DELETE.setArgument(APPLICATION_ARGUMENT, this.getApplicationId())
					.execute(true).equals(PowerShellCommand.RESULT_OK);
		}
		return true; // No operation executed
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicationId);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof UniversalWindowsProgram)) {
			return false;
		}
		UniversalWindowsProgram uwp = (UniversalWindowsProgram) o;
		return Objects.equals(applicationId, uwp.applicationId);
	}

	@Override
	public String toString() {
		return "UniversalWindowsProgram [applicationId=" + applicationId + "]";
	}

}
