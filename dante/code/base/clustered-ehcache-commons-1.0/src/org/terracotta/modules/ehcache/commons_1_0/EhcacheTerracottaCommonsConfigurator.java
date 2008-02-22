package org.terracotta.modules.ehcache.commons_1_0;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.TransparencyClassSpec;

public abstract class EhcacheTerracottaCommonsConfigurator extends TerracottaConfiguratorModule implements IConstants {

  protected void addInstrumentation(final BundleContext context) {
		super.addInstrumentation(context);

    // find the bundle that contains the replacement classes
    Bundle bundle = getExportedBundle(context, getExportedBundleName());
    Bundle thisBundle = getExportedBundle(context, COMMON_EHCACHE_BUNDLE_NAME);
    if (null == bundle) {
      throw new RuntimeException("Couldn't find bundle with symbolic name '"+getExportedBundleName()+"' during the instrumentation configuration of the bundle '"+context.getBundle().getSymbolicName()+"'.");
    }
    
    // setup the replacement classes
    addClassReplacement(bundle, CACHE_CLASS_NAME_DOTS, CACHETC_CLASS_NAME_DOTS);
    addClassReplacement(bundle, MEMORYSTOREEVICTIONPOLICY_CLASS_NAME_DOTS, MEMORYSTOREEVICTIONPOLICYTC_CLASS_NAME_DOTS);

    // setup the class resources
    addExportedBundleClass(thisBundle, "net.sf.ehcache.store.TimeExpiryMemoryStore");
    addExportedBundleClass(thisBundle, "net.sf.ehcache.store.TimeExpiryMemoryStore$SpoolingTimeExpiryMap");
    addExportedBundleClass(thisBundle, "org.terracotta.modules.ehcache.commons_1_0.util.Util");
    addExportedTcJarClass("com.tcclient.ehcache.TimeExpiryMap");
    addExportedTcJarClass("com.tcclient.ehcache.TimeExpiryMap$EntriesIterator");
    addExportedTcJarClass("com.tcclient.ehcache.TimeExpiryMap$EntrySetWrapper");
    addExportedTcJarClass("com.tcclient.ehcache.TimeExpiryMap$EntryWrapper");
    addExportedTcJarClass("com.tcclient.ehcache.TimeExpiryMap$KeySetWrapper");
    addExportedTcJarClass("com.tcclient.ehcache.TimeExpiryMap$KeysIterator");
    addExportedTcJarClass("com.tcclient.ehcache.TimeExpiryMap$ValuesCollectionWrapper");
    addExportedTcJarClass("com.tcclient.ehcache.TimeExpiryMap$ValuesIterator");
    addExportedTcJarClass("com.tcclient.cache.CacheConfig");
    addExportedTcJarClass("com.tcclient.cache.CacheData");
    addExportedTcJarClass("com.tcclient.cache.CacheDataStore");
    addExportedTcJarClass("com.tcclient.cache.CacheEntryInvalidator");
    addExportedTcJarClass("com.tcclient.cache.CacheInvalidationTimer");
    addExportedTcJarClass("com.tcclient.cache.CacheInvalidationTimer$EvictionRunner");
    addExportedTcJarClass("com.tcclient.cache.Expirable");
    addExportedTcJarClass("com.tcclient.cache.Lock");
    addExportedTcJarClass("com.tcclient.cache.Timestamp");
    addExportedTcJarClass("com.tcclient.cache.GlobalKeySet");
    addExportedTcJarClass("com.tc.util.Util");
    
    // explicitly excluding autolocking
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.CacheData.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.CacheDataStore.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.Lock.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.Timestamp.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.ehcache..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.store.TimeExpiryMemoryStore.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.store.TimeExpiryMemoryStore$SpoolingTimeExpiryMap.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.Cache.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.Ehcache.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.Statistics.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.Status.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.bootstrap.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.config.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.constructs.asynchronous.*.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.constructs.blocking.*.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.constructs.concurrent.ConcurrencyUtil.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.constructs.concurrent.Sync.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.distribution.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.event.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.hibernate.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.jcache.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.management.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.store.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.util.*..*(..)");
    
    configHelper.addAutolock("* net.sf.ehcache.constructs.concurrent.Mutex.acquire(..)", ConfigLockLevel.WRITE);
    configHelper.addAutolock("* net.sf.ehcache.constructs.concurrent.Mutex.attempt(..)", ConfigLockLevel.WRITE);
    configHelper.addAutolock("* net.sf.ehcache.constructs.concurrent.Mutex.release(..)", ConfigLockLevel.WRITE);

    // perform the rest of the configuration
    configHelper.addIncludePattern("com.tcclient.cache.*", false, false, false);
    configHelper.addIncludePattern("com.tcclient.ehcache.*", false, false, false);
    TransparencyClassSpec spec = configHelper.getOrCreateSpec("com.tcclient.cache.CacheDataStore");
    spec.setHonorTransient(true);
    /** Changed as an extention to coresident L1. on load is not required, since during faulting 
     * CacheTC instance is initialized, it will initialize cache store as well. 
     */
//    spec.setCallMethodOnLoad("initialize");
    /** Done coresident L1 changes **/
    spec.addDistributedMethodCall("stopInvalidatorThread", "()V", false);
    spec = configHelper.getOrCreateSpec("com.tcclient.cache.CacheData");
    spec.setCallConstructorOnLoad(true);
    spec.setHonorTransient(true);
    
    ClassAdapterFactory factory = new EhcacheMemoryStoreAdapter();
    spec = configHelper.getOrCreateSpec(MEMORYSTORE_CLASS_NAME_DOTS);
    spec.setCustomClassAdapter(factory);
    
    // autolocking
    configHelper.addAutolock(" * com.tcclient.cache.GlobalKeySet.*(..)", ConfigLockLevel.WRITE);

    /** Changed as an extention to coresident L1 **/
    addExportedBundleClass(thisBundle,"net.sf.ehcache.store.PartitionedStore");
    spec = configHelper.getOrCreateSpec("net.sf.ehcache.Cache");
    spec.setHonorTransient(true);
    spec = configHelper.getOrCreateSpec(MEMORYSTORE_CLASS_NAME_DOTS);
    /** net.sf.ehcache.Cache instance is not partitioned across multiple L2s. Per cache there is
     * only one instance that resides on first L2. Each partitioned store has a reference to 
     * MemoryStore that internally refers net.sf.ehcache.Cache. Since each difference instance of
     * PartitionedStore resides on differnt L2, the contained MemoryStore can not have a pesistent
     * reference to net.sf.ehcache.Cache, which is managed by only one of L2. Hence 'cache'reference
     * inside MemoryStore is declared as transient and while net.sf.ehcache.Cache is initialized, it
     * initializes MemoryStore.cache with its own instance.
     */
    spec.addTransient("cache");
    spec.addTransient("status");
    configHelper.addAutolock("* net.sf.ehcache.CacheManager.*(..)", ConfigLockLevel.WRITE);
    spec = configHelper.getOrCreateSpec(CACHE_CLASS_NAME_DOTS);
    spec.setHonorTransient(true);
    /** Done coresident L1 changes **/
	}
  
  protected abstract String getExportedBundleName();
}
