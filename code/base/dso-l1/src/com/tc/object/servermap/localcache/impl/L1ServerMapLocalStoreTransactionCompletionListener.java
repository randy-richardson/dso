/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionID;

/**
 * To be used only when a transaction is completed.
 */
public class L1ServerMapLocalStoreTransactionCompletionListener implements TransactionCompleteListener {
  private final ServerMapLocalCache          serverMapLocalCache;
  private final Object                       key;
  private final TransactionCompleteOperation transactionCompleteOperation;
  private final AbstractLocalCacheStoreValue value;

  public L1ServerMapLocalStoreTransactionCompletionListener(ServerMapLocalCache serverMapLocalCache, Object key,
                                                            AbstractLocalCacheStoreValue value,
                                                            TransactionCompleteOperation onCompleteOperation) {
    this.serverMapLocalCache = serverMapLocalCache;
    this.key = key;
    this.transactionCompleteOperation = onCompleteOperation;
    this.value = value;
  }

  public void transactionComplete(TransactionID txnID) {
    serverMapLocalCache.unpinEntry(key, value);
    if (transactionCompleteOperation == TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY) {
      // TODO: could this be a race or a problem ?
      // It could be a problem actually
      serverMapLocalCache.removeFromLocalCache(key);
    }
  }

  public static enum TransactionCompleteOperation {
    UNPIN_ENTRY, UNPIN_AND_REMOVE_ENTRY;
  }
}
