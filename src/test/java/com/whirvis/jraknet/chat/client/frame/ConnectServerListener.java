/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2020 "Whirvis" Trent Summerlin
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
package com.whirvis.jraknet.chat.client.frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetTest;
import com.whirvis.jraknet.chat.client.ChatClient;

/**
 * Used to listen for presses to the connect/disconnect button in the frame.
 * <p>
 * When a press to the connect/disconnect button has been detected, it will
 * client to connect/disconnect.
 *
 * @author "Whirvis" Trent Summerlin
 * @since JRakNet v2.0.0
 */
public final class ConnectServerListener implements ActionListener {

	private final ChatFrame frame;
	private final ChatClient client;

	/**
	 * Constructs a <code>ConnectServerListener</code>.
	 * 
	 * @param frame
	 *            the frame that the listener belongs to.
	 * @param client
	 *            the client that the listener will signal.
	 * @throws NullPointerException
	 *             if the <code>frame</code> or <code>client</code> are
	 *             <code>null</code>.
	 */
	protected ConnectServerListener(ChatFrame frame, ChatClient client) throws NullPointerException {
		if (frame == null) {
			throw new NullPointerException("Chat frame cannot be null");
		} else if (client == null) {
			throw new NullPointerException("Chat client cannot be null");
		}
		this.frame = frame;
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if (frame.connected == false) {
				frame.setInstructions(ChatClient.INSTRUCTIONS_CONNECTING);
				client.connect(RakNet.parseAddressPassive(frame.txtServerAddress.getText(),
						RakNetTest.WHIRVIS_DEVELOPMENT_PORT));
			} else {
				frame.txtServerAddress.setText("");
				frame.txtClientUsername.setText("");
				client.disconnect("Client disconnected");
			}
		} catch (Exception e1) {
			client.caughtError(e1);
		}
	}

}
