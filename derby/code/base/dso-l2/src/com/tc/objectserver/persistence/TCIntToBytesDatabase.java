/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.util.Map;

public interface TCIntToBytesDatabase {
  public Status put(int id, byte[] b, PersistenceTransaction tx);

  public byte[] get(int id, PersistenceTransaction tx);

  public Map<Integer, byte[]> getAll(PersistenceTransaction tx);
}
