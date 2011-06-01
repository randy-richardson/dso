/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionID;

/**
 * To be used only when a transaction is completed.
 */
public class L1ServerMapLocalStoreTransactionCompletionListener implements TransactionCompleteListener {
  private final ServerMapLocalCache serverMapLocalCache;
  private final Object              key;
  private final boolean             removeEntryOnTransactionComplete;

  public L1ServerMapLocalStoreTransactionCompletionListener(ServerMapLocalCache serverMapLocalCache, Object key,
                                                            boolean removeEntryOnTransactionComplete) {
    this.serverMapLocalCache = serverMapLocalCache;
    this.key = key;
    this.removeEntryOnTransactionComplete = removeEntryOnTransactionComplete;
    this.serverMapLocalCache.pinEntry(this.key);
  }

  public void transactionComplete(TransactionID txnID) {
    serverMapLocalCache.unpinEntry(key);
    if (removeEntryOnTransactionComplete) {
      // TODO: could this be a race or a problem ?
      // It could be a problem actually
      serverMapLocalCache.evictFromLocalCache(key, null);
    }
  }
}
