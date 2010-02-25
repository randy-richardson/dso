/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

public interface TCMapsDatabase {
  public boolean put(byte[] key, byte[] value, PersistenceTransaction tx);

  public boolean delete(byte[] key, PersistenceTransaction tx);

  /**
   * Returns no of bytes written
   */
  public int deleteCollection(long objectID, PersistenceTransaction tx) throws TCDatabaseException;

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx);
}
