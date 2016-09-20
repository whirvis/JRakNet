package net.marfgamer.raknet.util.map;

import java.util.HashMap;
import java.util.Map;

public class IntMap<T> extends HashMap<Integer, T> implements Map<Integer, T> {

	private static final long serialVersionUID = 4324132003573381634L;

	public boolean containsKey(int key) {
		return super.containsKey(new Integer(key));
	}

	public boolean containsValue(Object value) {
		return super.containsValue(value);
	}

	public T get(int key) {
		return super.get(new Integer(key));
	}

	public T put(int key, T value) {
		return super.put(new Integer(key), value);
	}

	public T remove(int key) {
		return super.remove(new Integer(key));
	}

}
