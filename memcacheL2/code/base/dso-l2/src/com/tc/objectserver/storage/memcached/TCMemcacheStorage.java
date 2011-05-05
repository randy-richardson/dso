/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.memcached;

import org.jboss.netty.buffer.ChannelBuffers;

import com.tc.exception.ImplementMe;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNAInternal;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.objectserver.impl.ServerMapEvictionTransactionBatchContext;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.objectserver.managedobject.TDCSerializedEntryManagedObjectState;
import com.tc.objectserver.tx.MemcacheCDSMDNA;
import com.tc.objectserver.tx.MemcacheElementDNA;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManagerEventListener;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.util.sequence.ObjectIDSequence;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.storage.CacheStorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class TCMemcacheStorage implements CacheStorage<Key, LocalCacheElement>, ServerTransactionManagerEventListener {

  private static final String               MEMCACHE_ROOT_NAME = "MEMCACHE-GLOBAL-CACHE";
  private final ObjectManagerImpl           objectManager;
  private final ObjectIDSequence            oidSequence;
  private volatile ObjectID                 memcacheRootID;
  private final NodeID                      localNodeID;
  private final AtomicLong                  opsVersions        = new AtomicLong();
  private final ObjectStringSerializerImpl  serializer;
  private final TransactionBatchManagerImpl transactionBatchManager;
  private final ServerTransactionFactory    serverTxnFactory;

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
    this.serializer = new ObjectStringSerializerImpl();
    this.transactionBatchManager = transactionBatchManager;
    this.serverTxnFactory = new ServerTransactionFactory();
    this.objectManager = objectManager;
    this.oidSequence = objectStore;

    // transactionManager.addRootListener(this);
    memcacheRootID = new ObjectID(objectStore.nextObjectIDBatch(1));
    ServerTransaction txn = this.serverTxnFactory.createMemcacheRootTxn(localNodeID, memcacheRootID.toLong(),
                                                                        MEMCACHE_ROOT_NAME);

    applyMemCacheTxn(txn);
  }

  private void applyMemCacheTxn(ServerTransaction txn) {
    final TransactionBatchContext batchContext = new ServerMapEvictionTransactionBatchContext(localNodeID, txn,
                                                                                              serializer);

    this.transactionBatchManager.processTransactions(batchContext);
  }

  private byte[] getSerializedLocalCacheElement(LocalCacheElement element) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream stream = new ObjectOutputStream(byteArrayOutputStream);
      stream.writeObject(element);
      byteArrayOutputStream.flush();
      stream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    byte[] b = byteArrayOutputStream.toByteArray();

    getDeserializedLocalCacheElement(b);
    return byteArrayOutputStream.toByteArray();
  }

  private LocalCacheElement getDeserializedLocalCacheElement(byte[] bytes) {
    if (bytes == null) { return null; }

    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
    try {
      ObjectInputStream stream = new ObjectInputStream(byteArrayInputStream);
      LocalCacheElement element = (LocalCacheElement) stream.readObject();
      stream.close();
      return element;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
    ManagedObject mo = getMemcacheRootMO();
    ManagedObject valueMO = null;
    try {
      ObjectID valueID = (ObjectID) ((ConcurrentDistributedServerMapManagedObjectState) mo.getManagedObjectState())
          .getMap().get(key);
      if (valueID == null) { return new LocalCacheElement((Key) key); }

      // System.out.println("GET OID: " + valueID);
      valueMO = this.objectManager.getObjectByID(valueID);

      LocalCacheElement element = new LocalCacheElement((Key) key);
      element.setData(ChannelBuffers.wrappedBuffer(((TDCSerializedEntryManagedObjectState) valueMO
          .getManagedObjectState()).value));

      // System.out.println("XXX GET key:" + element.getKey() + "; value: " + element.getData());
      return element;
    } finally {
      this.objectManager.releaseReadOnly(mo);
      if (valueMO != null) this.objectManager.releaseReadOnly(valueMO);
    }
  }

  /**
   * 1. create oid for value <br>
   * 2. create dna for serialized entry <br>
   * 3. create dna for CDSM put <br>
   * 4. create a server txn with both changes and apply
   */
  public LocalCacheElement put(Key key, LocalCacheElement value) {
    byte[] b = value.getData().array();
    ObjectID valueOID = new ObjectID(this.oidSequence.nextObjectIDBatch(1));
    DNAInternal entryCreate = new MemcacheElementDNA(valueOID, opsVersions.incrementAndGet(), b);
    DNAInternal cdsmPut = new MemcacheCDSMDNA(memcacheRootID, new Object[] { key, valueOID }, SerializationUtil.PUT,
                                              opsVersions.incrementAndGet());

    // List changes = new ArrayList();
    // changes.add(cdsmPut);
    // applyMemCacheTxn(this.serverTxnFactory.createMemcacheElementTxn(localNodeID, changes));
    //
    // List changes2 = new ArrayList();
    // changes2.add(entryCreate);
    // applyMemCacheTxn(this.serverTxnFactory.createMemcacheElementTxn(localNodeID, changes2));

    List changes = new ArrayList();
    changes.add(entryCreate);
    changes.add(cdsmPut);
    applyMemCacheTxn(this.serverTxnFactory.createMemcacheElementTxn(localNodeID, changes));

    // System.out.println("XXX PUT key: " + key + "; value: " + value.getData() + " -- OID: " + valueOID);
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
    // if (name.equals(MEMCACHE_ROOT_NAME)) {
    // System.out.println("XXX root created " + name + " - " + id);
    // this.memcacheRootID = id;
    // this.memcacheRootMO = this.objectManager.getObjectByID(id);
    // }
  }

  ManagedObject getMemcacheRootMO() {
    return this.objectManager.getObjectByID(this.memcacheRootID);
  }

}
