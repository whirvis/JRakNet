package net.marfgamer.raknet.util.map;

import java.util.ArrayList;
import java.util.Collection;

public class ByteArray extends ArrayList<Byte> {

	private static final long serialVersionUID = 2515208032737395193L;

	public boolean add(byte b) {
		return super.add(new Byte(b));
	}

	public boolean addAll(byte[] b) {
		Collection<Byte> c = new ArrayList<Byte>();
		for (int i = 0; i < b.length; i++) {
			c.add(b[i]);
		}
		return super.addAll(c);
	}
	
	public boolean addAll(int index, byte[] b) {
		
	}

}
