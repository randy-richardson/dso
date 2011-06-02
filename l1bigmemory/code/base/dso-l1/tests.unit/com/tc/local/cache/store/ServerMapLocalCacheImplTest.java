/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.local.cache.store.ServerMapLocalCacheImpl.TransactionCompletionAdaptor;
import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ServerMapLocalCacheImplTest extends TCTestCase {
  private volatile ServerMapLocalCacheImpl cache;
  private final ObjectID                   mapID       = new ObjectID(50000);
  private final int                        maxInMemory = 100;
  private ServerMapLocalCacheIDStore       cacheIDStore;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TransactionCompletionAdaptor adaptor = new MyTransactionCompletionAdaptor();
    GlobalLocalCacheManager globalLocalCacheManager = new GlobalLocalCacheManagerImpl();
    cache = new ServerMapLocalCacheImpl(mapID, adaptor, globalLocalCacheManager, maxInMemory, true);
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

    Assert.assertEquals(50, cache.size());
    Assert.assertEquals(50, cacheIDStore.size());
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

    Assert.assertEquals(50, cache.size());

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

    Assert.assertEquals(25, cache.size());
  }

  public void testAddCoherentValueToCacheRemove2() throws Exception {
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);
    setLocalCacheForRemove2(latch1, latch2);

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
    Assert.assertEquals(1, cache.size());
    list = cacheIDStore.get(new ObjectID(1));
    Assert.assertNull(list);

    latch1.countDown();
    latch2.await();

    value = cache.getCoherentLocalValue("key1");
    Assert.assertNull(value);
    list = cacheIDStore.get(new ObjectID(1));
    Assert.assertNull(list);
  }

  private void setLocalCacheForRemove2(final CountDownLatch latch1, final CountDownLatch latch2) {
    TransactionCompletionAdaptor adaptor = new TransactionCompletionAdaptor() {
      public void registerForCallbackOnComplete(final L1ServerMapLocalStoreTransactionCompletionListener listener) {
        Runnable runnable = new Runnable() {
          public void run() {
            try {
              latch1.await();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }

            listener.transactionComplete(null);

            latch2.countDown();
          }
        };

        Thread t = new Thread(runnable, "invoke txn complete");
        t.start();
      }
    };
    GlobalLocalCacheManager globalLocalCacheManager = new GlobalLocalCacheManagerImpl();
    cache = new ServerMapLocalCacheImpl(mapID, adaptor, globalLocalCacheManager, maxInMemory, true);
    cacheIDStore = cache.getCacheIDStore();
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

  private static class MyTransactionCompletionAdaptor implements TransactionCompletionAdaptor {
    public void registerForCallbackOnComplete(
                                              L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
      ThreadUtil.reallySleep(1);
      l1ServerMapLocalStoreTransactionCompletionListener.transactionComplete(null);
    }

  }
}
