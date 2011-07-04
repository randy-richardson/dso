/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import org.mockito.Mockito;

import com.tc.exception.ImplementMe;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallHelper;
import com.tc.object.locks.LongLockID;
import com.tc.object.locks.Notify;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.GlobalLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreIncoherentValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionContext;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ServerMapLocalCacheImplTest extends TestCase {
  private volatile ServerMapLocalCacheImpl cache;
  private final ObjectID                   mapID       = new ObjectID(50000);
  private final int                        maxInMemory = 1000;
  private L1ServerMapLocalCacheStore       cacheIDStore;
  private TestLocksRecallHelper            locksRecallHelper;
  private GlobalLocalCacheManagerImpl      globalLocalCacheManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setLocalCache(null, null, maxInMemory);
  }

  public void setLocalCache(CountDownLatch latch1, CountDownLatch latch2, int maxElementsInMemory) {
    setLocalCache(latch1, latch2, maxElementsInMemory, new TestLocksRecallHelper());
  }

  public void setLocalCache(CountDownLatch latch1, CountDownLatch latch2, int maxElementsInMemory,
                            TestLocksRecallHelper testLocksRecallHelper) {
    locksRecallHelper = testLocksRecallHelper;
    globalLocalCacheManager = new GlobalLocalCacheManagerImpl(locksRecallHelper);
    locksRecallHelper.setGlobalLocalCacheManager(globalLocalCacheManager);
    final ClientTransaction clientTransaction = new MyClientTransaction(latch1, latch2);
    ClientObjectManager com = Mockito.mock(ClientObjectManager.class);
    ClientTransactionManager ctm = Mockito.mock(ClientTransactionManager.class);
    Mockito.when(com.getTransactionManager()).thenReturn(ctm);
    Mockito.when(ctm.getCurrentTransaction()).thenReturn(clientTransaction);
    cache = (ServerMapLocalCacheImpl) globalLocalCacheManager.getOrCreateLocalCache(mapID, com, null, true);
    cache.setupLocalStore(new L1ServerMapLocalCacheStoreHashMap(maxElementsInMemory));
    cacheIDStore = cache.getL1ServerMapLocalCacheStore();
  }

  public void testGetMapID() throws Exception {
    Assert.assertEquals(mapID, cache.getMapID());
  }

  private void assertValueType(AbstractLocalCacheStoreValue value, LocalCacheValueType type) {
    for (LocalCacheValueType t : LocalCacheValueType.values()) {
      if (t == type) {
        Assert.assertTrue(t.isValueOfType(value));
      } else {
        Assert.assertFalse(t.isValueOfType(value));
      }
    }
  }

  private void assertEventualValue(Object expectedValue, ObjectID expectedObjectId, AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.EVENTUAL);
    Assert.assertTrue(value instanceof LocalCacheStoreEventualValue);
    try {
      value.asEventualValue();
    } catch (Throwable t) {
      Assert.fail("Should be able to retrieve value as Eventual value: " + t);
    }
    try {
      value.asStrongValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    try {
      value.asIncoherentValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }

    Assert.assertEquals(expectedValue, value.getValue());
    Assert.assertEquals(expectedObjectId, value.getId());
    Assert.assertEquals(expectedObjectId, value.asEventualValue().getObjectId());
  }

  private void assertIncoherentValue(Object expectedValue, AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.INCOHERENT);
    Assert.assertTrue(value instanceof LocalCacheStoreIncoherentValue);
    try {
      value.asIncoherentValue();
    } catch (Throwable t) {
      Assert.fail("Should be able to retrieve value as Incoherent value: " + t);
    }
    try {
      value.asStrongValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    try {
      value.asEventualValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    Assert.assertEquals(expectedValue, value.getValue());
    Assert.assertEquals(null, value.getId());
  }

  private void assertStrongValue(Object expectedValue, LockID expectedLockId, AbstractLocalCacheStoreValue value) {
    assertValueType(value, LocalCacheValueType.STRONG);
    Assert.assertTrue(value instanceof LocalCacheStoreStrongValue);
    try {
      value.asStrongValue();
    } catch (Throwable t) {
      Assert.fail("Should be able to retrieve value as Strong value: " + t);
    }
    try {
      value.asIncoherentValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    try {
      value.asEventualValue();
      fail("Should have failed");
    } catch (ClassCastException ignored) {
      // expected
    }
    Assert.assertEquals(expectedValue, value.getValue());
    Assert.assertEquals(expectedLockId, value.getId());
    Assert.assertEquals(expectedLockId, value.asStrongValue().getLockId());
  }

  public void testAddEventualValueToCache() throws Exception {

    for (int i = 0; i < 50; i++) {
      cache.addEventualValueToCache(new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    // TODO
    // Assert.assertEquals(50, cache.size());
    // Assert.assertEquals(50, cacheIDStore.size());
  }

  public void testAddEventualValueRemove1() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addEventualValueToCache(new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    // Assert.assertEquals(50, cache.size());

    // REMOVE
    for (int i = 0; i < 25; i++) {
      cache.addEventualValueToCache(ObjectID.NULL_ID, "key" + i, null, MapOperationType.REMOVE);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    // Assert.assertEquals(25, cache.size());
  }

  public void testAddEventualValueRemove2() throws Exception {
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);
    setLocalCache(latch1, latch2, this.maxInMemory);

    // GET - add to the local cache
    cache.addEventualValueToCache(new ObjectID(1), "key1", "value1", MapOperationType.GET);
    AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key1");
    assertEventualValue("value1", new ObjectID(1), value);
    List list = (List) cacheIDStore.get(new ObjectID(1));
    Assert.assertEquals(1, list.size());
    Assert.assertEquals("key1", list.get(0));

    // REMOVE
    cache.addEventualValueToCache(ObjectID.NULL_ID, "key1", null, MapOperationType.REMOVE);

    value = cache.getCoherentLocalValue("key1");
    Assert.assertEquals(null, value.getValue());
    Assert.assertEquals(ObjectID.NULL_ID, value.asEventualValue().getId());
    // Assert.assertEquals(1, cache.size());
    Assert.assertNull(cacheIDStore.get(new ObjectID(1)));

    latch1.countDown();
    latch2.await();

    value = cache.getCoherentLocalValue("key1");
    Assert.assertNull(value);
    Assert.assertNull(cacheIDStore.get(new ObjectID(1)));
  }

  public void testAddIncoherentValueToCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addIncoherentValueToCache("key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getLocalValue("key" + i);
      assertIncoherentValue("value" + i, value);
    }
    Assert.assertEquals(50, cache.size());

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
    }
    Assert.assertEquals(0, cache.size());
  }

  public void testFlush() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addEventualValueToCache(new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    // Flush
    for (int i = 0; i < 25; i++) {
      cache.removeEntriesForObjectId(new ObjectID(i));
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

  }

  public void testRecalledLocks() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addStrongValueToCache(new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertNotNull(list);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    Set<LockID> evictLocks = new HashSet<LockID>();
    for (int i = 0; i < 25; i++) {
      LockID lockID = new LongLockID(i);
      evictLocks.add(lockID);
      globalLocalCacheManager.removeEntriesForLockId(lockID);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new LongLockID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }
  }

  public void testRemovedEntries() throws Exception {
    Test2LocksRecallHelper test2LocksRecallHelper = new Test2LocksRecallHelper();
    setLocalCache(null, null, maxInMemory, test2LocksRecallHelper);

    for (int i = 0; i < 50; i++) {
      cache.addStrongValueToCache(new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertNotNull(list);
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }

    Set<LockID> evictLocks = new HashSet<LockID>();
    for (int i = 0; i < 25; i++) {
      LockID lockID = new LongLockID(i);
      evictLocks.add(lockID);
      cache.removeFromLocalCache("key" + i);
    }

    Assert.assertEquals(evictLocks, test2LocksRecallHelper.lockIdsToEvict);

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new LongLockID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertStrongValue("value" + i, new LongLockID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new LongLockID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }
  }

  public void testPinEntry() throws Exception {
    //
  }

  public void testUnpinEntry() throws Exception {
    //
  }

  public void testEvictFromLocalCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addEventualValueToCache(new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 25; i++) {
      cache.removeFromLocalCache("key" + i);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }
  }

  public void testAddAllObjectIDsToValidate() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addStrongValueToCache(new LongLockID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    Map<ObjectID, ObjectIDSet> map = new HashMap<ObjectID, ObjectIDSet>();
    cache.addAllObjectIDsToValidate(map);

    Assert.assertEquals(0, map.size());

    for (int i = 50; i < 100; i++) {
      cache.addEventualValueToCache(new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }
    cache.addAllObjectIDsToValidate(map);

    Assert.assertEquals(1, map.size());

    ObjectIDSet set = map.get(this.mapID);
    Assert.assertEquals(50, set.size());
    for (int i = 50; i < 100; i++) {
      Assert.assertTrue(set.contains(new ObjectID(i)));
    }
  }

  public void testSize() throws Exception {
    //
  }

  public void testClearAllLocalCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addEventualValueToCache(new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    cache.clear();

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }
  }

  public void testRemoveFromLocalCache() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addEventualValueToCache(new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 0; i < 25; i++) {
      cache.removeFromLocalCache("key" + i);
    }

    for (int i = 0; i < 25; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      Assert.assertNull(value);
      Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
    }

    for (int i = 25; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      assertEventualValue("value" + i, new ObjectID(i), value);
    }

    for (int i = 25; i < 50; i++) {
      List list = (List) cacheIDStore.get(new ObjectID(i));
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("key" + i, list.get(0));
    }
  }

  public void testEvictCachedEntries() throws Exception {
    for (int i = 0; i < 50; i++) {
      cache.addEventualValueToCache(new ObjectID(i), "key" + i, "value" + i, MapOperationType.PUT);
    }

    cache.evictCachedEntries(25);

    int evicted = 0;
    int notEvicted = 0;
    for (int i = 0; i < 50; i++) {
      AbstractLocalCacheStoreValue value = cache.getCoherentLocalValue("key" + i);
      if (value != null) {
        assertEventualValue("value" + i, new ObjectID(i), value);
        List list = (List) cacheIDStore.get(new ObjectID(i));
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("key" + i, list.get(0));
        notEvicted++;
      } else {
        Assert.assertNull(cacheIDStore.get(new ObjectID(i)));
        evicted++;
      }
    }

    Assert.assertEquals(25, evicted);
    Assert.assertEquals(25, notEvicted);
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

  private static class TestLocksRecallHelper implements LocksRecallHelper {
    private volatile Set<LockID>             lockIds;
    private volatile GlobalLocalCacheManager globalLocalCacheManager;

    public void setGlobalLocalCacheManager(GlobalLocalCacheManager globalLocalCacheManagerParam) {
      this.globalLocalCacheManager = globalLocalCacheManagerParam;
    }

    public void initiateLockRecall(Set<LockID> locks) {
      this.lockIds = locks;
      for (LockID id : lockIds) {
        globalLocalCacheManager.removeEntriesForLockId(id);
      }
    }

    public void recallLocksInline(Set<LockID> locks) {
      this.lockIds = locks;
      for (LockID id : lockIds) {
        globalLocalCacheManager.removeEntriesForLockId(id);
      }
    }

  }

  private static class Test2LocksRecallHelper extends TestLocksRecallHelper {
    private volatile Set<LockID> lockIdsToEvict = new HashSet<LockID>();

    @Override
    public void initiateLockRecall(Set<LockID> locks) {
      for (LockID id : locks) {
        lockIdsToEvict.add(id);
      }
    }

    @Override
    public void recallLocksInline(Set<LockID> locks) {
      for (LockID id : locks) {
        lockIdsToEvict.add(id);
      }
    }

  }

  public static enum LocalCacheValueType {
    STRONG() {

      @Override
      public boolean isValueOfType(AbstractLocalCacheStoreValue value) {
        return value.isStrongConsistentValue();
      }

    },
    EVENTUAL() {

      @Override
      public boolean isValueOfType(AbstractLocalCacheStoreValue value) {
        return value.isEventualConsistentValue();
      }
    },
    INCOHERENT() {

      @Override
      public boolean isValueOfType(AbstractLocalCacheStoreValue value) {
        return value.isIncoherentValue();
      }
    };

    public abstract boolean isValueOfType(AbstractLocalCacheStoreValue value);
  }

}
