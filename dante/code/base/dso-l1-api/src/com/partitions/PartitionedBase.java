package com.partitions;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import com.tc.config.lock.LockLevel;
import com.tc.object.bytecode.ManagerUtil;

public abstract class PartitionedBase {

		private int numPartitions;
		protected Object[] init(Class implementation, Class type, Class parameterTypes[], Object args[], String rootName) throws Exception{
			if(!type.isAssignableFrom(implementation))
				throw new ClassCastException(implementation + " must extend/implement " + type);
			numPartitions = PartitionManager.getNumPartitions();
			Object[] partitions = (Object[])Array.newInstance(type,numPartitions);
			for(int i = 0; i < numPartitions; i++) {
				Constructor cons = implementation.getConstructor(parameterTypes);
				Object partRoot = cons.newInstance(args);
				PartitionManager.setPartition(i);
				ManagerUtil.beginLock(rootName, LockLevel.WRITE);
				try {
					partitions[i] = ManagerUtil.lookupOrCreateRoot(rootName, partRoot);
				} finally {
					ManagerUtil.commitLock(rootName);
				}
			}
			return partitions;
		}
		
	    protected int positionOf(Object key) {
	    	int position = 0;
	        if (key != null) {
	        	position = Math.abs(key.hashCode() % numPartitions);;
	        }
			PartitionManager.setPartition(position);
	        return position;
	    }
}
