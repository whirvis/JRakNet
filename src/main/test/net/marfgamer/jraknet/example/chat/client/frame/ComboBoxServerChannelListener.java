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
package net.marfgamer.jraknet.example.chat.client.frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.marfgamer.jraknet.example.chat.ServerChannel;
import net.marfgamer.jraknet.example.chat.client.ChatClient;
import net.marfgamer.jraknet.example.chat.client.ChatException;

/**
 * Listens for changes in the current selected channel, used to set the client
 * channel.
 *
 * @author Whirvis "MarfGamer" Ardenaur
 */
public class ComboBoxServerChannelListener implements ActionListener {

	private final ChatFrame frame;
	private final ChatClient client;

	public ComboBoxServerChannelListener(ChatFrame frame, ChatClient client) {
		this.frame = frame;
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ServerChannel serverChannel = (ServerChannel) frame.cmbServerChannels.getSelectedItem();

		try {
			client.setChannel(serverChannel.getChannel());
		} catch (ChatException e1) {
			frame.displayError(e1);
		}
	}

}
