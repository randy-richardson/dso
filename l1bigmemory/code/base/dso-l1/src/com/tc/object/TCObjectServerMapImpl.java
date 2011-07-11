/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.GlobalLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.Set;

public class TCObjectServerMapImpl<L> extends TCObjectLogical implements TCObject, TCObjectServerMap<L> {

  private final static TCLogger        logger           = TCLogging.getLogger(TCObjectServerMapImpl.class);

  private static final boolean         EVICTOR_LOGGING  = TCPropertiesImpl
                                                            .getProperties()
                                                            .getBoolean(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED);

  private static final Object[]        NO_ARGS          = new Object[] {};

  static {
    boolean deprecatedProperty = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED);
    if (!deprecatedProperty) {
      // trying to disable is not supported
      logger.warn("The property '" + TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED
                  + "' has been deprecated, set the localCacheEnabled to false in config to disable local caching.");
    }
  }

  private final GroupID                groupID;
  private final ClientObjectManager    objectManager;
  private final RemoteServerMapManager serverMapManager;
  private final Manager                manager;
  private final ServerMapLocalCache    cache;
  private volatile int                 maxInMemoryCount = 0;
  private volatile boolean             invalidateOnChange;
  private volatile boolean             localCacheEnabled;

  private L1ServerMapLocalCacheStore   serverMapLocalStore;

  public TCObjectServerMapImpl(final Manager manager, final ClientObjectManager objectManager,
                               final RemoteServerMapManager serverMapManager, final ObjectID id, final Object peer,
                               final TCClass tcc, final boolean isNew,
                               final GlobalLocalCacheManager globalLocalCacheManager) {
    super(id, peer, tcc, isNew);
    this.groupID = new GroupID(id.getGroupID());
    this.objectManager = objectManager;
    this.serverMapManager = serverMapManager;
    this.manager = manager;
    this.cache = globalLocalCacheManager.getOrCreateLocalCache(id, objectManager, manager, localCacheEnabled);
    if (serverMapLocalStore != null) {
      logger.debug(getObjectID() + ": Setting serverMapLocalStore in constructor");
      cache.setupLocalStore(serverMapLocalStore);
    } else {
      logger.debug(getObjectID() + ": serverMapLocalStore not initialized yet (in constructor)");
    }
  }

  public void initialize(final int maxTTISeconds, final int maxTTLSeconds, final int targetMaxInMemoryCount,
                         final int targetMaxTotalCount, final boolean invalidateOnChangeFlag,
                         final boolean localCacheEnabledFlag) {
    this.maxInMemoryCount = targetMaxInMemoryCount;
    this.invalidateOnChange = invalidateOnChangeFlag;
    this.localCacheEnabled = localCacheEnabledFlag;
  }

  /**
   * Does a logical put and updates the local cache
   * 
   * @param map ServerTCMap
   * @param lockID Lock under which this change is made
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPut(final TCServerMap map, final L lockID, final Object key, final Object value) {
    invokeLogicalPut(map, key, value);
    this.cache.addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value, MapOperationType.PUT);
  }

  public void doClear(TCServerMap map) {
    logicalInvoke(SerializationUtil.CLEAR, SerializationUtil.PUT_SIGNATURE, NO_ARGS);
  }

  /**
   * Does a logical put and updates the local cache but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPutUnlocked(TCServerMap map, Object key, Object value) {
    ObjectID valueID = invokeLogicalPut(map, key, value);

    if (!invalidateOnChange || valueID.isNull()) {
      this.cache.addIncoherentValueToCache(key, value, MapOperationType.PUT);
    } else {
      this.cache.addEventualValueToCache(valueID, key, valueID, MapOperationType.PUT);
    }
  }

  public boolean doLogicalPutIfAbsentUnlocked(TCServerMap map, Object key, Object value) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null && item.getValue() != null) {
      // Item already present
      return false;
    }

    ObjectID valueID = invokeLogicalPutIfAbsent(map, key, value);

    if (!invalidateOnChange || valueID.isNull()) {
      this.cache.addIncoherentValueToCache(key, value, MapOperationType.PUT);
    } else {
      this.cache.addEventualValueToCache(valueID, key, value, MapOperationType.PUT);
    }

    return true;
  }

  public boolean doLogicalReplaceUnlocked(TCServerMap map, Object key, Object current, Object newValue) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null && current != item.getValue()) {
      // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
      // and coz of DEV-5462
      return false;
    }
    ObjectID valueID = invokeLogicalReplace(map, key, current, newValue);

    if (!invalidateOnChange || valueID.isNull()) {
      this.cache.addIncoherentValueToCache(key, newValue, MapOperationType.PUT);
    } else {
      this.cache.addEventualValueToCache(valueID, key, newValue, MapOperationType.PUT);
    }

    return true;
  }

  /**
   * Does a logic remove and removes from the local cache if present
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is removed
   * @param key Key Object
   */
  public void doLogicalRemove(final TCServerMap map, final L lockID, final Object key) {
    invokeLogicalRemove(map, key);

    this.cache.addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, null, MapOperationType.REMOVE);
  }

  /**
   * Does a two arg logical remove and removes from the local cache if present but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   */
  public void doLogicalRemoveUnlocked(TCServerMap map, Object key) {
    invokeLogicalRemove(map, key);

    if (!invalidateOnChange) {
      this.cache.addIncoherentValueToCache(key, null, MapOperationType.REMOVE);
    } else {
      this.cache.addEventualValueToCache(ObjectID.NULL_ID, key, null, MapOperationType.REMOVE);
    }
  }

  /**
   * Does a two arg logical remove and removes from the local cache if present but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @return
   */
  public boolean doLogicalRemoveUnlocked(TCServerMap map, Object key, Object value) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null && value != item.getValue()) {
      // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
      // and coz of DEV-5462
      return false;
    }

    invokeLogicalRemove(map, key, value);

    if (!invalidateOnChange) {
      this.cache.addIncoherentValueToCache(key, null, MapOperationType.REMOVE);
    } else {
      this.cache.addEventualValueToCache(ObjectID.NULL_ID, key, null, MapOperationType.REMOVE);
    }

    return true;
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param map Map Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @param lockID Lock under which this call is made
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValue(final TCServerMap map, final L lockID, final Object key) {
    final AbstractLocalCacheStoreValue item = this.cache.getCoherentLocalValue(key);
    if (item != null) { return item.getValue(); }

    final Object value = getValueForKeyFromServer(map, key);
    this.cache.addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value, MapOperationType.GET);

    return value;
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap outside a lock context.
   * 
   * @param map Map Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValueUnlocked(TCServerMap map, Object key) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null) return item.getValue();

    final Object value = getValueForKeyFromServer(map, key);

    if (invalidateOnChange) {
      // Null values (i.e. cache misses) & literal values are not cached locally
      if (value != null && !LiteralValues.isLiteralInstance(value)) {
        this.cache.addEventualValueToCache(objectManager.lookupExistingObjectID(value), key, value,
                                           MapOperationType.GET);
      }
    } else {
      this.cache.addIncoherentValueToCache(key, value, MapOperationType.GET);
    }
    return value;
  }

  private AbstractLocalCacheStoreValue getValueUnlockedFromCache(Object key) {
    if (invalidateOnChange) {
      return this.cache.getCoherentLocalValue(key);
    } else {
      return this.cache.getLocalValue(key);
    }
  }

  private Object getValueForKeyFromServer(final TCServerMap map, final Object key) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getValueForKeyInMap is not supported in a non-shared ServerMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    Object portableKey = key;
    if (key instanceof Manageable) {
      final TCObject keyObject = ((Manageable) key).__tc_managed();
      if (keyObject == null) { throw new UnsupportedOperationException(
                                                                       "Key is portable, but not shared. This is currently not supported with ServerMap. Map ID = "
                                                                           + mapID + " key = " + key); }
      portableKey = keyObject.getObjectID();
    }

    if (!LiteralValues.isLiteralInstance(portableKey)) {
      // formatter
      throw new UnsupportedOperationException(
                                              "Key is not portable. It needs to be a liternal or portable and shared for ServerTCMap. Key = "
                                                  + portableKey + " map id = " + mapID);
    }

    final Object value = this.serverMapManager.getMappingForKey(mapID, portableKey);

    if (value instanceof ObjectID) {
      try {
        return this.objectManager.lookupObject((ObjectID) value);
      } catch (final ClassNotFoundException e) {
        logger.warn("Got ClassNotFoundException for objectId: " + value + ". Ignoring exception and returning null");
        return null;
      } catch (TCObjectNotFoundException e) {
        logger.warn("Got TCObjectNotFoundException for objectId: " + value + ". Ignoring exception and returning null");
        return null;
      }
    } else {
      return value;
    }
  }

  /**
   * Returns a snapshot of keys for the giver ServerTCMap
   * 
   * @param map ServerTCMap
   * @return set Set return snapshot of keys
   */
  public Set keySet(final TCServerMap map) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException("keySet is not supported in a non-shared ServerMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    return this.serverMapManager.getAllKeys(mapID);
  }

  /**
   * Returns total size of an array of ServerTCMap.
   * <P>
   * The list of TCServerMaps passed in need not contain this TCServerMap, this is only a pass thru method that calls
   * getAllSize on the RemoteServerMapManager and is provided as a convenient way of batching the size calls at the
   * higher level.
   * 
   * @param maps ServerTCMap[]
   * @return long for size of map.
   */
  public long getAllSize(final TCServerMap[] maps) {
    final ObjectID[] mapIDs = new ObjectID[maps.length];
    for (int i = 0; i < maps.length; ++i) {
      TCServerMap map = maps[i];
      final TCObject tcObject = map.__tc_managed();
      if (tcObject == null) { throw new UnsupportedOperationException(
                                                                      "getSize is not supported in a non-shared ServerMap"); }
      mapIDs[i] = tcObject.getObjectID();
    }
    return this.serverMapManager.getAllSize(mapIDs);
  }

  public int getLocalSize() {
    return this.cache.size();
  }

  /**
   * Clears local cache of all entries. It is not immediate as all associated locks needs to be recalled.
   * 
   * @param map ServerTCMap
   */
  public void clearLocalCache(final TCServerMap map) {
    this.cache.clear();
  }

  /**
   * Clears local cache of all entries. It is not immediate as all associated locks needs to be recalled. This method
   * will wait until lock recall is complete.
   * 
   * @param map ServerTCMap
   */
  public void clearAllLocalCacheInline(final TCServerMap map) {
    this.cache.inlineClearAllLocalCache();
  }

  public void removeFromLocalCache(Object key) {
    this.cache.removeFromLocalCache(key);
  }

  @Override
  protected boolean isEvictable() {
    return true;
  }

  /**
   * Called by the memory manager
   */
  @Override
  protected int clearReferences(final Object pojo, final int toClear) {
    if (this.maxInMemoryCount > 0) {
      // don't clear, let target capacity eviction handle this.
      return 0;
    } else {
      if (EVICTOR_LOGGING) {
        logEviction("Memory Manager requesting eviction: toClear=" + toClear);
      }
      return this.cache.evictCachedEntries(toClear);
    }
  }

  private void logEviction(final String msg) {
    logger.info("ServerMap Eviction: " + getObjectID() + " : " + msg);
  }

  public Set getLocalKeySet() {
    return this.cache.getKeySet();
  }

  public Object getValueFromLocalCache(final Object key) {
    AbstractLocalCacheStoreValue cachedItem = this.cache.getLocalValue(key);
    if (cachedItem != null) {
      return cachedItem.getValue();
    } else {
      return null;
    }
  }

  /**
   * Shares the entry and calls logicalPut.
   * 
   * @return ObjectID of the value
   */
  private ObjectID invokeLogicalPut(final TCServerMap map, final Object key, final Object value) {
    return invokeLogicalPutInternal(map, key, value, false);
  }

  /**
   * Shares the entry and calls logicalPutIfAbsent.
   * 
   * @return ObjectID of the value
   */
  private ObjectID invokeLogicalPutIfAbsent(final TCServerMap map, final Object key, final Object value) {
    return invokeLogicalPutInternal(map, key, value, true);
  }

  private ObjectID invokeLogicalPutInternal(TCServerMap map, Object key, Object value, boolean putIfAbsent) {
    final Object[] parameters = new Object[] { key, value };

    shareObject(key);
    ObjectID valueID = shareObject(value);

    if (putIfAbsent) {
      logicalInvoke(SerializationUtil.PUT_IF_ABSENT, SerializationUtil.PUT_IF_ABSENT_SIGNATURE, parameters);
    } else {
      logicalInvoke(SerializationUtil.PUT, SerializationUtil.PUT_SIGNATURE, parameters);
    }
    return valueID;
  }

  private ObjectID invokeLogicalReplace(final TCServerMap map, final Object key, final Object current,
                                        final Object newValue) {
    final Object[] parameters = new Object[] { key, current, newValue };

    shareObject(key);
    shareObject(current);
    ObjectID valueID = shareObject(newValue);

    logicalInvoke(SerializationUtil.REPLACE_IF_VALUE_EQUAL, SerializationUtil.REPLACE_IF_VALUE_EQUAL_SIGNATURE,
                  parameters);
    return valueID;
  }

  private ObjectID shareObject(Object param) {
    boolean isLiteral = LiteralValues.isLiteralInstance(param);
    if (!isLiteral) {
      TCObject tcObject = this.objectManager.lookupOrCreate(param, this.groupID);
      return tcObject.getObjectID();
    }
    return ObjectID.NULL_ID;
  }

  private void invokeLogicalRemove(final TCServerMap map, final Object key) {
    logicalInvoke(SerializationUtil.REMOVE, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { key });
  }

  private void invokeLogicalRemove(final TCServerMap map, final Object key, final Object value) {
    logicalInvoke(SerializationUtil.REMOVE_IF_VALUE_EQUAL, SerializationUtil.REMOVE_IF_VALUE_EQUAL_SIGNATURE,
                  new Object[] { key, value });
  }

  public void addMetaData(MetaDataDescriptor mdd) {
    this.objectManager.getTransactionManager().addMetaDataDescriptor(this, (MetaDataDescriptorInternal) mdd);
  }

  public void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore) {
    // this is called from CDSMDso.__tc_managed(tco)
    this.serverMapLocalStore = serverMapLocalStore;
    if (cache != null) {
      logger.debug(getObjectID()
                   + ": Setting up serverMapLocalStore in setupLocalStore as serverMapLocalCache is not null");
      cache.setupLocalStore(serverMapLocalStore);
    } else {
      logger.debug(getObjectID()
                   + ": NOT setting up serverMapLocalStore in setupLocalStore as serverMapLocalCache IS null");
    }
  }
}
