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
  private final DisposeListener disposeListener;
  private final Object          key;
  private final boolean         removeEntryOnTransactionComplete;

  public L1ServerMapLocalStoreTransactionCompletionListener(DisposeListener disposeListener, Object key,
                                                            boolean removeEntryOnTransactionComplete) {
    this.disposeListener = disposeListener;
    this.key = key;
    this.removeEntryOnTransactionComplete = removeEntryOnTransactionComplete;
    this.disposeListener.pinEntry(this.key);
  }

  public void transactionComplete(TransactionID txnID) {
    disposeListener.unpinEntry(key);
    if (removeEntryOnTransactionComplete) {
      // TODO: could this be a race or a problem ?
      // It could be a problem actually
      disposeListener.evictFromLocalCache(key, null);
    }
  }
}
