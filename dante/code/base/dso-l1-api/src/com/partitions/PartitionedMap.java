package com.partitions;

import java.util.Map;

public class PartitionedMap extends PartitionedBase{
	private Map partitions[];
	public PartitionedMap(Class mapClassName, Class parameterTypes[], Object args[], String rootName) 
			throws Exception {
		partitions = (Map[])init(mapClassName, java.util.Map.class,parameterTypes,args,rootName);
	}
	
    public Object put(Object key, Object value) {
    	int position = positionOf(key);
    	synchronized(partitions[position]) {
    		return partitions[position].put(key, value);
    	}
    }
    
    public Object get(Object key) {
    	int position = positionOf(key);
    	synchronized(partitions[position]) {
    		return partitions[position].get(key);
    	}
    }
    
}
