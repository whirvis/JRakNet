/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetTest;
import com.whirvis.jraknet.discovery.Discovery;
import com.whirvis.jraknet.discovery.DiscoveryMode;
import com.whirvis.jraknet.identifier.MinecraftIdentifier;

/**
 * The frame used to visualize the broadcast test.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class BroadcastFrame extends JFrame {

	/**
	 * Listens for discovery mode changes and then update the client accordingly.
	 *
	 * @author Whirvis T. Wheatley
	 * @since JRakNet v2.0.0
	 */
	private final class RakNetBroadcastDiscoveryTypeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = ((JComboBox<?>) e.getSource()).getSelectedIndex();
			if (index == 0) {
				Discovery.setDiscoveryMode(DiscoveryMode.ALL_CONNECTIONS);
			} else if (index == 1) {
				Discovery.setDiscoveryMode(DiscoveryMode.OPEN_CONNECTIONS);
			} else {
				Discovery.setDiscoveryMode(DiscoveryMode.DISABLED);
			}
		}

	}

	/**
	 * Listens for discovery port changes and then update the client accordingly.
	 *
	 * @author Whirvis T. Wheatley
	 * @since JRakNet v2.0.0
	 */
	private final class RakNetBroadcastUpdatePortListener implements ActionListener {

		private final JTextField textFieldDiscoveryPort;

		/**
		 * Constructs a <code>RakNetBroadcastUpdatePortListener</code>.
		 * 
		 * @param textFieldDiscoveryPort the text field containing the discovery port.
		 */
		public RakNetBroadcastUpdatePortListener(JTextField textFieldDiscoveryPort) {
			this.textFieldDiscoveryPort = textFieldDiscoveryPort;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int newDiscoveryPort = RakNet.parseIntPassive(textFieldDiscoveryPort.getText());
			if (newDiscoveryPort >= 0x0000 && newDiscoveryPort <= 0xFFFF) {
				Discovery.setPorts(newDiscoveryPort);
			} else {
				textFieldDiscoveryPort.setText(Integer.toString(Discovery.getPorts()[0]));
			}
		}

	}

	private static final long serialVersionUID = -313407802523891773L;

	/**
	 * The width of the broadcast test frame.
	 */
	private static final int FRAME_WIDTH = 500;

	/**
	 * The height of the broadcast test frame.
	 */
	private static final int FRAME_HEIGHT = 235;

	/**
	 * The discovery mode options that can be chosen from.
	 */
	private static final String[] DISCOVERY_MODE_OPTIONS = new String[] { "All Connections", "Open Connections",
			"No discovery" };

	private final JTextPane txtPnDiscoveredMcpeServerList;

	/**
	 * Creates a broadcast test frame.
	 */
	protected BroadcastFrame() {
		// Frame and content settings
		this.setResizable(false);
		this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		this.setTitle("JRakNet Broadcast Test");
		this.getContentPane().setLayout(null);

		// Discovered MCPE Servers
		JTextPane txtpnDiscoveredMcpeServers = new JTextPane();
		txtpnDiscoveredMcpeServers.setEditable(false);
		txtpnDiscoveredMcpeServers.setBackground(UIManager.getColor("Button.background"));
		txtpnDiscoveredMcpeServers.setText("Discovered servers");
		txtpnDiscoveredMcpeServers.setBounds(10, 10, 350, 20);
		this.getContentPane().add(txtpnDiscoveredMcpeServers);

		// How the client will discover servers on the local network
		JComboBox<String> comboBoxDiscoveryType = new JComboBox<String>();
		comboBoxDiscoveryType.setToolTipText(
				"Changing this will update how the client will discover servers, by default it will look for any possible connection on the network");
		comboBoxDiscoveryType.setModel(new DefaultComboBoxModel<String>(DISCOVERY_MODE_OPTIONS));
		comboBoxDiscoveryType.setBounds(370, 10, 115, 20);
		comboBoxDiscoveryType.addActionListener(new RakNetBroadcastDiscoveryTypeListener());
		this.getContentPane().add(comboBoxDiscoveryType);

		// Used to update the discovery port
		JTextField textFieldDiscoveryPort = new JTextField();
		textFieldDiscoveryPort.setBounds(370, 45, 115, 20);
		textFieldDiscoveryPort.setText(Integer.toString(Discovery.getPorts()[0]));
		this.getContentPane().add(textFieldDiscoveryPort);
		textFieldDiscoveryPort.setColumns(10);
		JButton btnUpdatePort = new JButton("Update Port");
		btnUpdatePort.setBounds(370, 76, 114, 23);
		btnUpdatePort.addActionListener(new RakNetBroadcastUpdatePortListener(textFieldDiscoveryPort));
		this.getContentPane().add(btnUpdatePort);

		// The text containing the discovered MCPE servers
		txtPnDiscoveredMcpeServerList = new JTextPane();
		txtPnDiscoveredMcpeServerList.setToolTipText("This is the list of the discovered servers on the local network");
		txtPnDiscoveredMcpeServerList.setEditable(false);
		txtPnDiscoveredMcpeServerList.setBackground(UIManager.getColor("Button.background"));
		txtPnDiscoveredMcpeServerList.setBounds(10, 30, 350, 165);
		this.getContentPane().add(txtPnDiscoveredMcpeServerList);
	}

	/**
	 * Updates the text in the frame according with the specified identifiers.
	 * 
	 * @param identifiers the identifiers.
	 */
	protected void updatePaneText(MinecraftIdentifier[] identifiers) {
		StringBuilder discoveryBuilder = new StringBuilder();
		for (int i = 0; i < identifiers.length; i++) {
			discoveryBuilder
					.append(RakNetTest.formatMCPEIdentifier(identifiers[i]) + (i + 1 < identifiers.length ? "\n" : ""));
		}
		txtPnDiscoveredMcpeServerList.setText(discoveryBuilder.toString());
	}
}
