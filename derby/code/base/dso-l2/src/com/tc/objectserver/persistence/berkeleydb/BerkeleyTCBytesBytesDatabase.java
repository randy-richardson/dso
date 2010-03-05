/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.persistence.TCBytesBytesDatabase;
import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

public class BerkeleyTCBytesBytesDatabase extends AbstractBerkeleyDatabase implements TCBytesBytesDatabase {
  public BerkeleyTCBytesBytesDatabase(Database db) {
    super(db);
  }

  public boolean delete(byte[] key, PersistenceTransaction tx) {
    DatabaseEntry entry = new DatabaseEntry();
    entry.setData(key);
    OperationStatus status = this.db.delete(pt2nt(tx), entry);
    if (!OperationStatus.SUCCESS.equals(status) && !OperationStatus.NOTFOUND.equals(status)) { return false; }
    return true;
  }

  public byte[] get(byte[] key, PersistenceTransaction tx) {
    DatabaseEntry entry = new DatabaseEntry();
    entry.setData(key);
    DatabaseEntry value = new DatabaseEntry();
    OperationStatus status = db.get(pt2nt(tx), entry, value, LockMode.DEFAULT);
    if (OperationStatus.SUCCESS.equals(status)) { return value.getData(); }
    return null;
  }

  public boolean put(byte[] key, byte[] val, PersistenceTransaction tx) {
    DatabaseEntry entryKey = new DatabaseEntry();
    entryKey.setData(key);
    DatabaseEntry entryValue = new DatabaseEntry();
    entryValue.setData(key);
    if (!OperationStatus.SUCCESS.equals(this.db.put(pt2nt(tx), entryKey, entryValue))) { return false; }
    return true;
  }

  public TCDatabaseCursor openCursor(PersistenceTransaction tx) {
    Cursor cursor = this.db.openCursor(pt2nt(tx), CursorConfig.READ_COMMITTED);
    return new BerkeleyTCDatabaseCursor(cursor);
  }

  public boolean putNoOverwrite(PersistenceTransaction tx, byte[] key, byte[] value) {
    DatabaseEntry entryKey = new DatabaseEntry();
    entryKey.setData(key);
    DatabaseEntry entryValue = new DatabaseEntry();
    entryValue.setData(key);
    OperationStatus status = this.db.putNoOverwrite(pt2nt(tx), entryValue, entryKey);
    return status.equals(OperationStatus.SUCCESS);
  }

  public TCDatabaseCursor<byte[], byte[]> openCursorUpdatable(PersistenceTransaction tx) {
    return openCursor(tx);
  }
}
