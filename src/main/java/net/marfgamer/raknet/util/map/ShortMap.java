package net.marfgamer.raknet.util.map;

import java.util.HashMap;
import java.util.Map;

public class ShortMap<T> extends HashMap<Short, T> implements Map<Short, T> {

	private static final long serialVersionUID = 4324132003573381634L;
	
	public boolean containsKey(short key) {
		return super.containsKey(new Short(key));
	}
	
	public boolean containsValue(Object value) {
		return super.containsValue(value);
	}
	
	public T get(short key) {
		return super.get(new Short(key));
	}

	public T put(short key, T value) {
		return super.put(new Short(key), value);
	}

	public T remove(short key) {
		return super.remove(new Short(key));
	}

}