package net.marfgamer.raknet.util.map;

import java.util.HashMap;
import java.util.Map;

public class LongMap<T> extends HashMap<Long, T> implements Map<Long, T> {

	private static final long serialVersionUID = 4324132003573381634L;
	
	public boolean containsKey(long key) {
		return super.containsKey(new Long(key));
	}
	
	public boolean containsValue(Object value) {
		return super.containsValue(value);
	}
	
	public T get(long key) {
		return super.get(new Long(key));
	}

	public T put(long key, T value) {
		return super.put(new Long(key), value);
	}

	public T remove(long key) {
		return super.remove(new Long(key));
	}

}
