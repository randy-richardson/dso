/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;

public interface ClientGlobalTransactionManager extends GlobalTransactionManager {
  public void setLowWatermark(GlobalTransactionID lowWatermark);

  public void flush(LockID lockID);

  public boolean startApply(NodeID nodeID, TransactionID transactionID, GlobalTransactionID globalTransactionID);

  /**
   * Returns the number of transactions currently being accounted for.
   */
  public int size();

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback);
}
