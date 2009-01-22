/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;

import java.util.List;

public interface ClientGlobalTransactionManager extends GlobalTransactionManager {
  public void setLowWatermark(GlobalTransactionID lowWatermark);

  public void flush(LockID lockID);

  public void unpause();

  public void pause();

  public void starting();

  public void resendOutstanding();

  public List getTransactionSequenceIDs();

  public List getResentTransactionIDs();

  public boolean startApply(NodeID nodeID, TransactionID transactionID, GlobalTransactionID globalTransactionID);

  /**
   * Returns the number of transactions currently being accounted for.
   */
  public int size();

  public void resendOutstandingAndUnpause();

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback);
}
