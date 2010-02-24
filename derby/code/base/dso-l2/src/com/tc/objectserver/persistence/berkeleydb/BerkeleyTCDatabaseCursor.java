/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.TCDatabaseEntry;

public class BerkeleyTCDatabaseCursor implements TCDatabaseCursor<byte[], byte[]> {
  private final Cursor cursor;

  public BerkeleyTCDatabaseCursor(Cursor cursor) {
    this.cursor = cursor;
  }

  public void close() {
    cursor.close();
  }

  public void delete() {
    cursor.delete();
  }

  public boolean getNext(TCDatabaseEntry<byte[], byte[]> entry) {
    DatabaseEntry entryKey = new DatabaseEntry();
    DatabaseEntry entryValue = new DatabaseEntry();
    OperationStatus status = cursor.getNext(entryKey, entryValue, LockMode.DEFAULT);
    entry.setKey(entryKey.getData()).setValue(entryValue.getData());
    return status.equals(OperationStatus.SUCCESS);
  }

}
