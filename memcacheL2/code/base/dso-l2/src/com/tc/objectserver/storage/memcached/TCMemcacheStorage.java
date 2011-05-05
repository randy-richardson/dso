/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.memcached;

import com.tc.exception.ImplementMe;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.objectserver.impl.ServerMapEvictionTransactionBatchContext;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.objectserver.tx.MemcacheDNA;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManagerEventListener;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.util.sequence.ObjectIDSequence;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.storage.CacheStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class TCMemcacheStorage implements CacheStorage<Key, LocalCacheElement>, ServerTransactionManagerEventListener {

  private static final String     MEMCACHE_ROOT_NAME = "MEMCACHE-GLOBAL-CACHE";
  private final ObjectManagerImpl objectManager;
  private volatile ObjectID       memcacheRootID;
  private volatile ManagedObject  memcacheRootMO;
  private final NodeID            localNodeID;
  private final AtomicLong        opsVersions        = new AtomicLong();

  /**
   * 1. Create a root map once <br>
   * 2. get an objectID from sequenceProvider <br>
   * 3. create a server txn with a new root
   * 
   * @param objectStore
   * @param transactionBatchManager
   * @param serverTransactionFactory
   * @param groupCommManager
   * @param transactionManager
   * @param objectManager
   */
  public TCMemcacheStorage(ObjectIDSequence objectStore, GroupManager groupCommManager,
                           ServerTransactionFactory serverTransactionFactory,
                           TransactionBatchManagerImpl transactionBatchManager,
                           ServerTransactionManagerImpl transactionManager, ObjectManagerImpl objectManager) {

    localNodeID = groupCommManager.getLocalNodeID();
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();

    transactionManager.addRootListener(this);
    ServerTransaction txn = new ServerTransactionFactory().createMemcacheRootTxn(localNodeID,
                                                                                 objectStore.nextObjectIDBatch(1),
                                                                                 MEMCACHE_ROOT_NAME);
    final TransactionBatchContext batchContext = new ServerMapEvictionTransactionBatchContext(localNodeID, txn,
                                                                                              serializer);
    transactionBatchManager.processTransactions(batchContext);
    this.objectManager = objectManager;
  }

  public LocalCacheElement putIfAbsent(Key key, LocalCacheElement value) {
    return null;
  }

  public boolean remove(Object key, Object value) {
    return false;
  }

  public boolean replace(Key key, LocalCacheElement oldValue, LocalCacheElement newValue) {
    return false;
  }

  public LocalCacheElement replace(Key key, LocalCacheElement value) {
    return null;
  }

  public int size() {
    return 0;
  }

  public boolean isEmpty() {
    return false;
  }

  public boolean containsKey(Object key) {
    return false;
  }

  public boolean containsValue(Object value) {
    return false;
  }

  public LocalCacheElement get(Object key) {
    LocalCacheElement element = (LocalCacheElement) ((ConcurrentDistributedServerMapManagedObjectState) getMemcacheRootMO()
        .getManagedObjectState()).getMap().get(key);
    if (element != null) System.out.println("XXX GET key:" + element.getKey() + "; value: " + element.getData());
    return element;
  }

  public LocalCacheElement put(Key key, LocalCacheElement value) {
    getMemcacheRootMO().apply(new MemcacheDNA(memcacheRootID, new Object[] { key, value }, SerializationUtil.PUT,
                                              opsVersions.incrementAndGet()), TransactionID.NULL_ID,
                              new ApplyTransactionInfo(), null, true);
    System.out.println("XXX PUT key: " + key + "; value: " + value.getData());
    return new LocalCacheElement();
  }

  public LocalCacheElement remove(Object key) {
    return null;
  }

  public void putAll(Map<? extends Key, ? extends LocalCacheElement> t) {
    throw new ImplementMe();
  }

  public void clear() {
    throw new ImplementMe();
  }

  public Set<Key> keySet() {
    throw new ImplementMe();
  }

  public Collection<LocalCacheElement> values() {
    throw new ImplementMe();
  }

  public Set<java.util.Map.Entry<Key, LocalCacheElement>> entrySet() {
    throw new ImplementMe();
  }

  public long getMemoryCapacity() {
    throw new ImplementMe();
  }

  public long getMemoryUsed() {
    throw new ImplementMe();
  }

  public int capacity() {
    throw new ImplementMe();
  }

  public void close() throws IOException {
    throw new ImplementMe();

  }

  public void rootCreated(String name, ObjectID id) {
    if (name.equals(MEMCACHE_ROOT_NAME)) {
      System.out.println("XXX root created " + name + " - " + id);
      this.memcacheRootID = id;
      this.memcacheRootMO = this.objectManager.getObjectByID(id);
    }
  }

  ManagedObject getMemcacheRootMO() {
    if (this.memcacheRootMO == null) {
      this.memcacheRootMO = this.objectManager.getObjectByID(this.memcacheRootID);
    }
    return this.memcacheRootMO;
  }

}
