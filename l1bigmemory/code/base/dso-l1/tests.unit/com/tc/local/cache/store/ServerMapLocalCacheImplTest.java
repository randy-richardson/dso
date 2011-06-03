/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import org.mockito.Mockito;

import com.tc.exception.ImplementMe;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.locks.LockID;
import com.tc.object.locks.Notify;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionContext;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ServerMapLocalCacheImplTest extends TCTestCase {
  private volatile ServerMapLocalCacheImpl cache;
  private final ObjectID                   mapID       = new ObjectID(50000);
  private final int                        maxInMemory = 100;
  private ServerMapLocalCacheIDStore       cacheIDStore;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setLocalCache(null, null, maxInMemory);
  }

  public void setLocalCache(CountDownLatch latch1, CountDownLatch latch2, int maxElementsInMemory) {
    GlobalLocalCacheManager globalLocalCacheManager = new GlobalLocalCacheManagerImpl();
    final ClientTransaction clientTransaction = new MyClientTransaction(latch1, latch2);
    ClientObjectManager com = Mockito.mock(ClientObjectManager.class);
    ClientTransactionManager ctm = Mockito.mock(ClientTransactionManager.class);
    Mockito.when(com.getTransactionManager()).thenReturn(ctm);
    Mockito.when(ctm.getCurrentTransaction()).thenReturn(clientTransaction);
    cache = new ServerMapLocalCacheImpl(mapID, com, null, globalLocalCacheManager, true);
    cache.setupLocalStore(new L1ServerMapLocalCacheStoreHashMap(maxElementsInMemory));
    cacheIDStore = cache.getCacheIDStore();
  }

  public void testGetMapID() throws Exception {
    Assert.assertEquals(mapID, cache.getMapID());
  }

  public void testAddCoherentValueToCache() throws Exception {

    for (int i = 0; i < 50; i++) {
      cache.addCoherentValueToCache(new ObjectID(i), "key" + i, "value" + i, true);
    }

    for (int i = 0; i < 50; i++) {
      LocalCacheStoreValue value = cache.getLocalValue("key" + i);
      Assert.assertEquals("value" + i, value.getValue());
      Assert.assertEquals(new ObjectID(i), value.getID());
    }

    for (int i = 0; i < 50; i++) {
      LocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertEquals("value" + i, value.getValue());
      Assert.assertEquals(new ObjectID(i), value.getID());
    }

    for (int i = 0; i < 50; i++) {
      List list = cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    // TODO
    // Assert.assertEquals(50, cache.size());
    // Assert.assertEquals(50, cacheIDStore.size());
  }

  public void testAddCoherentValueToCacheRemove1() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addCoherentValueToCache(new ObjectID(i), "key" + i, "value" + i, true);
    }

    for (int i = 0; i < 50; i++) {
      LocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertEquals("value" + i, value.getValue());
      Assert.assertEquals(new ObjectID(i), value.getID());
    }

    // Assert.assertEquals(50, cache.size());

    // REMOVE
    for (int i = 0; i < 25; i++) {
      cache.addCoherentValueToCache(ObjectID.NULL_ID, "key" + i, null, true, true);
    }

    for (int i = 0; i < 25; i++) {
      LocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      List list = cacheIDStore.get(new ObjectID(i));
      Assert.assertNull(list);
    }

    for (int i = 25; i < 50; i++) {
      LocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertEquals("value" + i, value.getValue());
      Assert.assertEquals(new ObjectID(i), value.getID());
    }

    for (int i = 25; i < 50; i++) {
      List list = cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    // Assert.assertEquals(25, cache.size());
  }

  public void testAddCoherentValueToCacheRemove2() throws Exception {
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);
    setLocalCache(latch1, latch2, this.maxInMemory);

    // GET - add to the local cache
    cache.addCoherentValueToCache(new ObjectID(1), "key1", "value1", false);
    LocalCacheStoreValue value = cache.getCoherentLocalValue("key1");
    List list = cacheIDStore.get(new ObjectID(1));
    Assert.assertEquals(1, list.size());
    Assert.assertEquals("key1", list.get(0));

    // REMOVE
    cache.addCoherentValueToCache(ObjectID.NULL_ID, "key1", null, true, true);

    value = cache.getCoherentLocalValue("key1");
    Assert.assertEquals(null, value.getValue());
    Assert.assertEquals(ObjectID.NULL_ID, value.getID());
    // Assert.assertEquals(1, cache.size());
    list = cacheIDStore.get(new ObjectID(1));
    Assert.assertNull(list);

    latch1.countDown();
    latch2.await();

    value = cache.getCoherentLocalValue("key1");
    Assert.assertNull(value);
    list = cacheIDStore.get(new ObjectID(1));
    Assert.assertNull(list);
  }

  public void testAddIncoherentValueToCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addIncoherentValueToCache("key" + i, "value" + i, true);
    }

    for (int i = 0; i < 50; i++) {
      LocalCacheStoreValue value = cache.getLocalValue("key" + i);
      Assert.assertEquals("value" + i, value.getValue());
      Assert.assertNull(value.getID());
    }
    Assert.assertEquals(50, cache.size());

    for (int i = 0; i < 50; i++) {
      LocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
    }
    Assert.assertEquals(0, cache.size());
  }

  public void testFlush() throws Exception {
    //
  }

  public void testclearForIDsAndRecallLocks() throws Exception {
    //
  }

  public void testPinEntry() throws Exception {
    //
  }

  public void testUnpinEntry() throws Exception {
    //
  }

  public void testEvictFromLocalCache() throws Exception {
    //
  }

  public void testAddAllObjectIDsToValidate() throws Exception {
    //
  }

  public void testSize() throws Exception {
    //
  }

  public void testClearAllLocalCache() throws Exception {
    //
  }

  public void testRemoveFromLocalCache() throws Exception {
    //
  }

  public void testEvictCachedEntries() throws Exception {
    //
  }

  public void testGetKeySet() throws Exception {
    //
  }

  public class MyClientTransaction implements ClientTransaction {
    private final CountDownLatch latch1;
    private final CountDownLatch latch2;

    public MyClientTransaction(CountDownLatch latch1, CountDownLatch latch2) {
      this.latch1 = latch1;
      this.latch2 = latch2;
    }

    public void addDmiDescriptor(DmiDescriptor dd) {
      throw new ImplementMe();
    }

    public void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
      throw new ImplementMe();
    }

    public void addNotify(Notify notify) {
      throw new ImplementMe();
    }

    public void addTransactionCompleteListener(TransactionCompleteListener l) {
      if (latch1 == null) {
        callDefault(l);
      } else {
        callLatched(l);
      }
    }

    private void callDefault(TransactionCompleteListener l) {
      ThreadUtil.reallySleep(1);
      l.transactionComplete(null);
    }

    public void callLatched(final TransactionCompleteListener l) {
      Runnable runnable = new Runnable() {
        public void run() {
          try {
            latch1.await();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }

          l.transactionComplete(null);

          latch2.countDown();
        }
      };

      Thread t = new Thread(runnable, "invoke txn complete");
      t.start();
    }

    public void arrayChanged(TCObject source, int startPos, Object array, int length) {
      throw new ImplementMe();

    }

    public void createObject(TCObject source) {
      throw new ImplementMe();

    }

    public void createRoot(String name, ObjectID rootID) {
      throw new ImplementMe();

    }

    public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
      throw new ImplementMe();

    }

    public List getAllLockIDs() {
      throw new ImplementMe();
    }

    public Map getChangeBuffers() {
      throw new ImplementMe();
    }

    public List getDmiDescriptors() {
      throw new ImplementMe();
    }

    public TxnType getEffectiveType() {
      throw new ImplementMe();
    }

    public LockID getLockID() {
      throw new ImplementMe();
    }

    public TxnType getLockType() {
      throw new ImplementMe();
    }

    public Map getNewRoots() {
      throw new ImplementMe();
    }

    public List getNotifies() {
      throw new ImplementMe();
    }

    public int getNotifiesCount() {
      throw new ImplementMe();
    }

    public Collection getReferencesOfObjectsInTxn() {
      throw new ImplementMe();
    }

    public SequenceID getSequenceID() {
      throw new ImplementMe();
    }

    public List getTransactionCompleteListeners() {
      throw new ImplementMe();
    }

    public TransactionID getTransactionID() {
      throw new ImplementMe();
    }

    public boolean hasChanges() {
      throw new ImplementMe();
    }

    public boolean hasChangesOrNotifies() {
      throw new ImplementMe();
    }

    public boolean isConcurrent() {
      throw new ImplementMe();
    }

    public boolean isNull() {
      throw new ImplementMe();
    }

    public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
      throw new ImplementMe();

    }

    public void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName) {
      throw new ImplementMe();

    }

    public void setAlreadyCommitted() {
      throw new ImplementMe();

    }

    public void setSequenceID(SequenceID sequenceID) {
      throw new ImplementMe();

    }

    public void setTransactionContext(TransactionContext transactionContext) {
      throw new ImplementMe();

    }

    public void setTransactionID(TransactionID tid) {
      throw new ImplementMe();

    }

  }

}
