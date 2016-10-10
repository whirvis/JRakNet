package net.marfgamer.raknet.protocol.acknowledge;

import java.util.Comparator;

public class RecordComparator implements Comparator<Record> {
	
    @Override
    public int compare(Record r1, Record r2) {
        return r1.getIndex() - r2.getIndex();
    }
    
}

