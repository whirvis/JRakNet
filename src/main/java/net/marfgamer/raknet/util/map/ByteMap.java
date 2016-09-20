package net.marfgamer.raknet.util.map;

import java.util.HashMap;
import java.util.Map;

public class ByteMap<T> extends HashMap<Byte, T> implements Map<Byte, T> {

	private static final long serialVersionUID = 4324132003573381634L;

	public boolean containsKey(byte key) {
		return super.containsKey(new Byte(key));
	}

	public boolean containsValue(Object value) {
		return super.containsValue(value);
	}

	public T get(byte key) {
		return super.get(new Byte(key));
	}

	public T put(byte key, T value) {
		return super.put(new Byte(key), value);
	}

	public T remove(byte key) {
		return super.remove(new Byte(key));
	}

}
