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
 * Copyright (c) 2016, 2017 MarfGamer
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import net.marfgamer.jraknet.UtilityTest;
import net.marfgamer.jraknet.client.RakNetClient;
import net.marfgamer.jraknet.client.discovery.DiscoveryMode;
import net.marfgamer.jraknet.identifier.MCPEIdentifier;
import net.marfgamer.jraknet.util.RakNetUtils;

/**
 * The frame used to visualize the broadcast test
 *
 * @author MarfGamer
 */
public class BroadcastFrame extends JFrame {

    private static final long serialVersionUID = -313407802523891773L;
    private static final int FRAME_WIDTH = 500;
    private static final int FRAME_HEIGHT = 235;
    private static final String[] discoveryModeOptions = new String[] { "All Connections", "Open Connections",
	    "No discovery" };

    private final JTextPane txtPnDiscoveredMcpeServerList;

    public BroadcastFrame(RakNetClient client) {
	// Frame settings
	setResizable(false);
	setSize(FRAME_WIDTH, FRAME_HEIGHT);
	setTitle("JRakNet Broadcast Test");
	setIconImage(FrameResources.RAKNET_ICON.getImage());

	// Content settings
	getContentPane().setLayout(null);

	// Discovered MCPE Servers
	JTextPane txtpnDiscoveredMcpeServers = new JTextPane();
	txtpnDiscoveredMcpeServers.setEditable(false);
	txtpnDiscoveredMcpeServers.setBackground(UIManager.getColor("Button.background"));
	txtpnDiscoveredMcpeServers.setText("Discovered servers");
	txtpnDiscoveredMcpeServers.setBounds(10, 10, 350, 20);
	getContentPane().add(txtpnDiscoveredMcpeServers);

	// How the client will discover servers on the local network
	JComboBox<String> comboBoxDiscoveryType = new JComboBox<String>();
	comboBoxDiscoveryType.setToolTipText(
		"Changing this will update how the client will discover servers, by default it will look for any possible connection on the network");
	comboBoxDiscoveryType.setModel(new DefaultComboBoxModel<String>(discoveryModeOptions));
	comboBoxDiscoveryType.setBounds(370, 10, 115, 20);
	comboBoxDiscoveryType.addActionListener(new RakNetBroadcastDiscoveryTypeListener(client));
	getContentPane().add(comboBoxDiscoveryType);

	// Used to update the discovery port
	JTextField textFieldDiscoveryPort = new JTextField();
	textFieldDiscoveryPort.setBounds(370, 45, 115, 20);
	textFieldDiscoveryPort.setText(Integer.toString(client.getDiscoveryPort()));
	getContentPane().add(textFieldDiscoveryPort);
	textFieldDiscoveryPort.setColumns(10);
	JButton btnUpdatePort = new JButton("Update Port");
	btnUpdatePort.setBounds(370, 76, 114, 23);
	btnUpdatePort.addActionListener(new RakNetBroadcastUpdatePortListener(client, textFieldDiscoveryPort));
	getContentPane().add(btnUpdatePort);

	// The text containing the discovered MCPE servers
	txtPnDiscoveredMcpeServerList = new JTextPane();
	txtPnDiscoveredMcpeServerList.setToolTipText("This is the list of the discovered servers on the local network");
	txtPnDiscoveredMcpeServerList.setEditable(false);
	txtPnDiscoveredMcpeServerList.setBackground(UIManager.getColor("Button.background"));
	txtPnDiscoveredMcpeServerList.setBounds(10, 30, 350, 165);
	getContentPane().add(txtPnDiscoveredMcpeServerList);
    }

    /**
     * Used to listen for discovery mode changes and then update the client
     * accordingly
     *
     * @author MarfGamer
     */
    private class RakNetBroadcastDiscoveryTypeListener implements ActionListener {

	private final RakNetClient client;

	public RakNetBroadcastDiscoveryTypeListener(RakNetClient client) {
	    this.client = client;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent e) {
	    // 0 is all connections, 1 is open connections, anything else is
	    // none
	    int index = ((JComboBox<String>) e.getSource()).getSelectedIndex();
	    if (index == 0) {
		client.setDiscoveryMode(DiscoveryMode.ALL_CONNECTIONS);
	    } else if (index == 1) {
		client.setDiscoveryMode(DiscoveryMode.OPEN_CONNECTIONS);
	    } else {
		client.setDiscoveryMode(DiscoveryMode.NONE);
	    }
	}

    }

    /**
     * Used to listen for discovery port changes and then update the client
     * accordingly
     *
     * @author MarfGamer
     */
    private class RakNetBroadcastUpdatePortListener implements ActionListener {

	private final RakNetClient client;
	private final JTextField textFieldDiscoveryPort;

	public RakNetBroadcastUpdatePortListener(RakNetClient client, JTextField textFieldDiscoveryPort) {
	    this.client = client;
	    this.textFieldDiscoveryPort = textFieldDiscoveryPort;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    int newDiscoveryPort = RakNetUtils.parseIntPassive(textFieldDiscoveryPort.getText());
	    if (newDiscoveryPort > -1) {
		client.setDiscoveryPort(newDiscoveryPort);
	    } else {
		textFieldDiscoveryPort.setText(Integer.toString(client.getDiscoveryPort()));
	    }
	}

    }

    /**
     * Updates the text in the JFrame according to the discovered servers
     * 
     * @param identifiers
     *            The identifiers of the discovered servers
     */
    public void updatePaneText(MCPEIdentifier[] identifiers) {
	StringBuilder discoverString = new StringBuilder();
	for (int i = 0; i < identifiers.length; i++) {
	    MCPEIdentifier identifier = identifiers[i];
	    discoverString
		    .append(UtilityTest.formatMCPEIdentifier(identifier) + (i + 1 < identifiers.length ? "\n" : ""));

	}
	txtPnDiscoveredMcpeServerList.setText(discoverString.toString());
    }
}
