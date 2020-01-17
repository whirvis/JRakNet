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
package com.whirvis.jraknet.interactive;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import com.whirvis.jraknet.peer.RakNetClientPeer;

/**
 * The frame used to visualize the latency test.
 *
 * @author Trent Summerlin
 * @since JRakNEt v2.0.0
 */
public final class LatencyFrame extends JFrame {

	private static final long serialVersionUID = 9127496840159114268L;

	/**
	 * The width of the latency test frame.
	 */
	private static final int FRAME_WIDTH = 375;

	/**
	 * The height of the latency test frame.
	 */
	private static final int FRAME_HEIGHT = 235;

	private final JTextPane txtPnClientLatencies;

	/**
	 * Creates a latency test frame.
	 */
	protected LatencyFrame() {
		// Frame and content settings
		this.setResizable(false);
		this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		this.setTitle("JRakNet Latency Test");
		this.getContentPane().setLayout(null);

		// Client latencies
		JTextPane txtpnClientLatencies = new JTextPane();
		txtpnClientLatencies.setEditable(false);
		txtpnClientLatencies.setText("Client latencies");
		txtpnClientLatencies.setBackground(UIManager.getColor("Button.background"));
		txtpnClientLatencies.setBounds(10, 10, 350, 20);
		this.getContentPane().add(txtpnClientLatencies);

		// The list of the connected clients
		txtPnClientLatencies = new JTextPane();
		txtPnClientLatencies.setToolTipText("This is the list of the connected clients and their latencies");
		txtPnClientLatencies.setEditable(false);
		txtPnClientLatencies.setBackground(UIManager.getColor("Button.background"));
		txtPnClientLatencies.setBounds(10, 30, 350, 165);
		this.getContentPane().add(txtPnClientLatencies);
	}

	/**
	 * Updates the text in the frame with the specified clients.
	 * 
	 * @param clients
	 *            the clients connected to the server.
	 */
	protected void updatePaneText(RakNetClientPeer[] clients) {
		StringBuilder latencyBuilder = new StringBuilder();
		for (int i = 0; i < clients.length; i++) {
			latencyBuilder.append(clients[i].getConnectionType().getName() + " client " + clients[i].getAddress() + ": "
					+ clients[i].getLatency() + "MS" + (i + 1 < clients.length ? "\n" : ""));
		}
		txtPnClientLatencies.setText(latencyBuilder.toString());
	}

}
