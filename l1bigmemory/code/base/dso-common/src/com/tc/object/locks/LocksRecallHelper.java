/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import java.util.Set;

public interface LocksRecallHelper {

  /**
   * Locks are recalled asynchronously
   */
  void initiateLockRecall(Set<LockID> lockIds);

  /**
   * Locks are recalled synchronously
   */
  void recallLocksInline(Set<LockID> lockIds);

}
