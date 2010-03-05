/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.api.PersistenceTransaction;

public interface TCBytesBytesDatabase {
  public boolean put(byte[] key, byte[] val, PersistenceTransaction tx);

  public byte[] get(byte[] key, PersistenceTransaction tx);

  public boolean delete(byte[] key, PersistenceTransaction tx);

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx);
  
  public TCDatabaseCursor<byte[], byte[]> openCursorUpdatable(PersistenceTransaction tx);

  public boolean putNoOverwrite(PersistenceTransaction tx, byte[] key, byte[] value);
}
