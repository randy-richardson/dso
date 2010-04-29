/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

public interface TCBytesToBytesDatabase {
  public Status put(byte[] key, byte[] val, PersistenceTransaction tx);

  public byte[] get(byte[] key, PersistenceTransaction tx);

  public Status delete(byte[] key, PersistenceTransaction tx);

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx);
  
  public TCDatabaseCursor<byte[], byte[]> openCursorUpdatable(PersistenceTransaction tx);

  public Status putNoOverwrite(PersistenceTransaction tx, byte[] key, byte[] value);
}
