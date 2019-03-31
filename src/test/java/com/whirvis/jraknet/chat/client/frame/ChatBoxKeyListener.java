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
package com.whirvis.jraknet.chat.client.frame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.whirvis.jraknet.chat.TextChannel;
import com.whirvis.jraknet.chat.client.ChatClient;

/**
 * Listens for presses to the enter button in the chat box.
 * <p>
 * When a press to the enter button has been deteced, it will signal the client
 * to send a chat message.
 *
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.0.0
 */
public final class ChatBoxKeyListener implements KeyListener {

	private final ChatFrame frame;
	private final ChatClient client;

	/**
	 * Constructs a <code>ChatBoxKeyListener</code>.
	 * 
	 * @param frame
	 *            the frame that the listener belongs to.
	 * @param client
	 *            the client that the listener will signal.
	 * @throws NullPointerException
	 *             if the <code>frame</code> or <code>client</code> are
	 *             <code>null</code>.
	 */
	protected ChatBoxKeyListener(ChatFrame frame, ChatClient client) throws NullPointerException {
		if (frame == null) {
			throw new NullPointerException("Chat frame cannot be null");
		} else if (client == null) {
			throw new NullPointerException("Chat client cannot be null");
		}
		this.frame = frame;
		this.client = client;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		try {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				TextChannel channel = (TextChannel) frame.cmbTextChannels.getSelectedItem();
				if (channel != null) {
					client.sendChatMessage(frame.txtChatBox.getText(), channel.getChannel());
					frame.txtChatBox.setText("");
				} else {
					frame.displayMessage("Please select a channel first");
				}
			}
		} catch (Exception e1) {
			client.caughtError(e1);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Not used
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Not used
	}

}
