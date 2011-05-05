/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.memcached;

import com.tc.exception.ImplementMe;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.objectserver.impl.ServerMapEvictionTransactionBatchContext;
import com.tc.objectserver.tx.ServerTransaction;
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

public class TCMemcacheStorage implements CacheStorage<Key, LocalCacheElement> {

  private final ObjectIDSequence objectIDSequence;

  /**
   * 1. Create a root map once <br>
   * 2. get an objectID from sequenceProvider <br>
   * 3. create a server txn with a new root
   * 
   * @param objectStore
   * @param transactionBatchManager
   * @param serverTransactionFactory
   * @param groupCommManager
   */
  public TCMemcacheStorage(ObjectIDSequence objectStore, GroupManager groupCommManager,
                           ServerTransactionFactory serverTransactionFactory,
                           TransactionBatchManagerImpl transactionBatchManager) {

    this.objectIDSequence = objectStore;

    final NodeID localNodeID = groupCommManager.getLocalNodeID();
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();

    ServerTransaction txn = new ServerTransactionFactory().createMemcacheRootTxn(localNodeID,
                                                                                 objectStore.nextObjectIDBatch(1));
    final TransactionBatchContext batchContext = new ServerMapEvictionTransactionBatchContext(localNodeID, txn,
                                                                                              serializer);
    transactionBatchManager.processTransactions(batchContext);
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
    return null;
  }

  public LocalCacheElement put(Key key, LocalCacheElement value) {
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

}
