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
package com.whirvis.jraknet.protocol.message.acknowledge;

import java.util.ArrayList;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;

/**
 * An <code>ACK</code> packet.
 * <p>
 * This packet is sent when a packet that requires an acknowledgement receipt is
 * received. This enables for servers and clients to know when the other side
 * has received their message, which can be crucial during the login process.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v1.0.0
 * @see Record
 */
public class AcknowledgedPacket extends RakNetPacket {

	/**
	 * The record is unranged.
	 */
	public static final int RANGED = 0x00;

	/**
	 * The record is ranged.
	 */
	public static final int UNRANGED = 0x01;

	/**
	 * The records containing the sequence IDs.
	 */
	public Record[] records;

	/**
	 * Creates an <code>ACK</code> packet to be encoded.
	 * 
	 * @param acknowledge <code>true</code> if the records inside the packet are
	 *                    acknowledged, <code>false</code> if the records are not
	 *                    acknowledged.
	 * @see #encode()
	 */
	protected AcknowledgedPacket(boolean acknowledge) {
		super(acknowledge ? ID_ACK : ID_NACK);
	}

	/**
	 * Creates an <code>ACK</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public AcknowledgedPacket() {
		this(true);
	}

	/**
	 * Creates an <code>ACK</code> packet to be decoded.
	 * 
	 * @param packet the original packet whose data will be read from in the
	 *               {@link #decode()} method.
	 */
	public AcknowledgedPacket(Packet packet) {
		super(packet);
	}

	/**
	 * Returns whether or not the records inside the packet are acknowledged.
	 * 
	 * @return <code>true</code> if the records inside the packet are acknowledged,
	 *         <code>false</code> if the records are not acknowledged.
	 */
	public boolean isAcknowledgement() {
		return this.getId() == ID_ACK;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Before encoding, all records will be condensed. This means that all records
	 * that can be converted to ranged records will be converted to ranged records,
	 * making them use less memory. The <code>records</code> field will be updated
	 * with these condensed records.
	 */
	@Override
	public void encode() {
		this.records = Record.condense(records);
		this.writeUnsignedShort(records.length);
		for (Record record : records) {
			this.writeUnsignedByte(record.isRanged() ? RANGED : UNRANGED);
			this.writeTriadLE(record.getIndex());
			if (record.isRanged()) {
				this.writeTriadLE(record.getEndIndex());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * After decoding is finished, all records will be expanded. This means that all
	 * ranged records will be converted to single records, making it easier to cycle
	 * through them. The <code>records</code> field will be updated with these
	 * expanded records.
	 */
	@Override
	public void decode() {
		ArrayList<Record> records = new ArrayList<Record>();
		int size = this.readUnsignedShort();
		for (int i = 0; i < size; i++) {
			boolean ranged = this.readUnsignedByte() == RANGED;
			if (ranged == false) {
				records.add(new Record(this.readTriadLE()));
			} else {
				records.add(new Record(this.readTriadLE(), this.readTriadLE()));
			}
		}
		this.records = Record.simplify(records);
	}

}
