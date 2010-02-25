/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

public interface TCStringToStringDatabase {
  public Status get(TCDatabaseEntry<String, String> key, PersistenceTransaction tx);

  public Status delete(String key, PersistenceTransaction tx);

  public boolean put(String key, String value, PersistenceTransaction tx);
}
