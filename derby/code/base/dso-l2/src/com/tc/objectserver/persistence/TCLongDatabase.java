/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.util.Set;

public interface TCLongDatabase {
  public Status put(long key, PersistenceTransaction tx);

  public Set<Long> getAllKeys(PersistenceTransaction tx);

  public boolean contains(long key, PersistenceTransaction tx);

  public TCDatabaseConstants.Status delete(long key, PersistenceTransaction tx);
}
