package net.sf.ehcache.store;

public class PartitionedStore {
	  private TimeExpiryMemoryStore                  memoryStore;
	  private Object[]                         locks;

	  public PartitionedStore(TimeExpiryMemoryStore memoryStore, int concurrency) {
		this.memoryStore = memoryStore;
	    this.locks = new Object[concurrency];
	    for (int i = 0; i < concurrency; i++) {
	      this.locks[i] = new Object();
	    }
	  }
	  
	  public Object[] getLocks() {
		  return locks;
	  }
	  
	  public TimeExpiryMemoryStore getMemoryStore() {
		  return memoryStore;
	  }
	  
	  
}
