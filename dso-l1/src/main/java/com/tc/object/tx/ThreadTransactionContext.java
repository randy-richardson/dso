/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.object.tx;

import com.tc.object.locks.LockID;
import com.tc.util.Assert;
import com.tc.util.Stack;

import java.util.ArrayList;
import java.util.List;

/**
 * We have two concepts. Transactions which carry changes/creates etc... And the locks that are associated with those
 * transactions. Transactions need to be created and then continued until the next transaction exit (in other words not
 * just until the next begin) if we are to maintain proper semantics/ordering. The Locks on the other hand work more
 * like a stack. When a block is entered the locks get pushed onto the stack and associated with the current transaction
 * and when they are exited they are removed from the current transaction. This class maintains both the current
 * transaction and the stack of contexts that are associated with the thread.
 */
public class ThreadTransactionContext {
  private final Stack       transactionStack = new Stack();
  private ClientTransaction currentTransaction;

  public void setCurrentTransaction(ClientTransaction tx) {
    Assert.eval(tx != null);
    this.currentTransaction = tx;
  }

  public ClientTransaction getCurrentTransaction() {
    return currentTransaction;
  }

  public ClientTransaction popCurrentTransaction() {
    transactionStack.pop();
    ClientTransaction ctx = currentTransaction;
    currentTransaction = null;
    return ctx;
  }

  public TransactionContext peekContext(LockID id) {
    if (transactionStack.isEmpty()) return null;
    int len = transactionStack.size();
    TransactionContext tc = null;
    int i = len - 1;
    for (; i >= 0; i--) {
      tc = (TransactionContext) transactionStack.get(i);
      if (tc.getLockID().equals(id)) { return tc; }
    }
    return null;
  }

  public void popLockContext(LockID id) {
    int len = transactionStack.size();
    boolean found = false;
    TransactionContext tc = null;
    int i = len - 1;
    for (; i >= 0; i--) {
      tc = (TransactionContext) transactionStack.get(i);
      if (tc.getLockID().equals(id)) {
        found = true;
        break;
      }
    }
    if (found) {
      for (int j = i + 1; j < len; j++) {
        tc = (TransactionContext) transactionStack.get(j);
        tc.removeLock(id);
      }
      transactionStack.remove(i);
    }
  }

  public ClientTransaction popCurrentTransaction(LockID id) {
    popLockContext(id);
    ClientTransaction ctx = currentTransaction;
    currentTransaction = null;
    return ctx;
  }

  public void pushContext(final LockID id, final TxnType lockTxType, final TxnType effectiveTxType) {
    transactionStack.push(new TransactionContextImpl(id, lockTxType, effectiveTxType, getAllLockIDs(id)));
  }

  private List getAllLockIDs(LockID id) {
    List lids = new ArrayList(transactionStack.size() + 1);
    lids.add(id);
    for (int i = 0, n = transactionStack.size(); i < n; i++) {
      TransactionContext tc = (TransactionContext) transactionStack.get(i);
      lids.add(tc.getLockID());
    }
    return lids;
  }

  public TransactionContext peekContext() {
    if (transactionStack.isEmpty()) return null;
    return (TransactionContext) transactionStack.peek();
  }

}
