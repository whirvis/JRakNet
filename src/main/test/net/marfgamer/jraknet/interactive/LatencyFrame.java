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
 * Copyright (c) 2016, 2017 Whirvis "MarfGamer" Ardenaur
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
package net.marfgamer.jraknet.interactive;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import net.marfgamer.jraknet.session.RakNetClientSession;

/**
 * The frame used to visualize the latency test.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class LatencyFrame extends JFrame {

	private static final long serialVersionUID = 9127496840159114268L;
	private static final int FRAME_WIDTH = 375;
	private static final int FRAME_HEIGHT = 235;

	private final JTextPane txtPnClientLatencies;

	public LatencyFrame() {
		// Frame settings
		setResizable(false);
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setTitle("JRakNet Latency Test");

		// Content settings
		getContentPane().setLayout(null);

		// Client latencies
		JTextPane txtpnClientLatencies = new JTextPane();
		txtpnClientLatencies.setEditable(false);
		txtpnClientLatencies.setText("Client latencies");
		txtpnClientLatencies.setBackground(UIManager.getColor("Button.background"));
		txtpnClientLatencies.setBounds(10, 10, 350, 20);
		getContentPane().add(txtpnClientLatencies);

		// The list of the connected clients
		txtPnClientLatencies = new JTextPane();
		txtPnClientLatencies.setToolTipText("This is the list of the connected clients and their latencies");
		txtPnClientLatencies.setEditable(false);
		txtPnClientLatencies.setBackground(UIManager.getColor("Button.background"));
		txtPnClientLatencies.setBounds(10, 30, 350, 165);
		getContentPane().add(txtPnClientLatencies);
	}

	/**
	 * Updates the text in the JFrame according to the connecting clients.
	 * 
	 * @param sessions
	 *            the sessions of the clients connected to the server.
	 */
	public void updatePaneText(RakNetClientSession[] sessions) {
		StringBuilder discoverString = new StringBuilder();
		for (int i = 0; i < sessions.length; i++) {
			RakNetClientSession session = sessions[i];
			discoverString.append((session.isJRakNet() ? "JRakNet" : "Vanilla") + " client " + session.getAddress()
					+ ": " + session.getLatency() + "MS" + (i + 1 < sessions.length ? "\n" : ""));
		}
		txtPnClientLatencies.setText(discoverString.toString());
	}

}
