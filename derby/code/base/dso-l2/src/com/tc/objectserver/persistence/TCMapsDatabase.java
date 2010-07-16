/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

public interface TCMapsDatabase {
  public Status put(long id, byte[] key, byte[] value, PersistenceTransaction tx);

  public Status delete(long id, byte[] key, PersistenceTransaction tx);

  /**
   * Returns no of bytes written
   */
  public int deleteCollection(long id, PersistenceTransaction tx) throws TCDatabaseException;

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx, long objectID);
  
  public long count();
}
