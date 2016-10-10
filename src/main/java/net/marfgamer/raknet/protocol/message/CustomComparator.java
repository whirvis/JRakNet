package net.marfgamer.raknet.protocol.message;

import java.util.Comparator;

public class CustomComparator implements Comparator<CustomPacket> {
	
    @Override
    public int compare(CustomPacket c1, CustomPacket c2) {
        Long sendTime1 = c1.sendTime;
        Long sendTime2 = c2.sendTime;
        return sendTime1.compareTo(sendTime2);
    }
    
}
