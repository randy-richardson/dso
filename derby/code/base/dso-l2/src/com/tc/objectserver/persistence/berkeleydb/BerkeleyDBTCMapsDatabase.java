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
import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.TCDatabaseEntry;
import com.tc.objectserver.persistence.TCMapsDatabase;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;
import com.tc.util.Conversion;

public class BerkeleyDBTCMapsDatabase extends BerkeleyDBTCBytesBytesDatabase implements TCMapsDatabase {

  public BerkeleyDBTCMapsDatabase(Database db) {
    super(db);
  }

  public TCDatabaseCursor openCursor(PersistenceTransaction tx, long objectID) {
    Cursor cursor = this.db.openCursor(pt2nt(tx), CursorConfig.READ_UNCOMMITTED);
    return new BerkeleyMapsTCDatabaseCursor(cursor, objectID);
  }

  public Status delete(long id, byte[] key, PersistenceTransaction tx) {
    return super.delete(key, tx);
  }

  public Status put(long id, byte[] key, byte[] value, PersistenceTransaction tx) {
    return super.put(key, value, tx);
  }

  public int deleteCollection(long objectID, PersistenceTransaction tx) throws TCDatabaseException {
    int written = 0;
    Cursor c = db.openCursor(pt2nt(tx), CursorConfig.READ_UNCOMMITTED);
    byte idb[] = Conversion.long2Bytes(objectID);
    DatabaseEntry key = new DatabaseEntry();
    key.setData(idb);
    DatabaseEntry value = new DatabaseEntry();
    value.setPartial(0, 0, true);
    try {
      if (c.getSearchKeyRange(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        do {
          if (partialMatch(idb, key.getData())) {
            written += key.getSize();
            c.delete();
          } else {
            break;
          }
        } while (c.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS);
      }
    } catch (Exception t) {
      throw new TCDatabaseException(t.getMessage());
    } finally {
      c.close();
    }
    return written;
  }

  private static boolean partialMatch(byte[] idbytes, byte[] key) {
    if (key.length < idbytes.length) return false;
    for (int i = 0; i < idbytes.length; i++) {
      if (idbytes[i] != key[i]) return false;
    }
    return true;
  }

  private static class BerkeleyMapsTCDatabaseCursor extends BerkeleyDBTCDatabaseCursor {
    private boolean isInit        = false;
    private boolean noMoreMatches = false;
    private long    objectID;

    public BerkeleyMapsTCDatabaseCursor(Cursor cursor, long objectID) {
      super(cursor);
      this.objectID = objectID;
    }

    @Override
    public boolean getNext(TCDatabaseEntry<byte[], byte[]> entry) {
      if (!isInit && !getSearchKeyRange(entry)) { return false; }

      if (noMoreMatches) { return false; }

      if (!isInit) {
        isInit = true;
      } else if (!super.getNext(entry)) { return false; }

      byte idb[] = Conversion.long2Bytes(objectID);

      if (partialMatch(idb, entry.getKey())) {
        return true;
      } else {
        noMoreMatches = true;
        return false;
      }
    }

    public boolean getSearchKeyRange(TCDatabaseEntry<byte[], byte[]> entry) {
      DatabaseEntry entryKey = new DatabaseEntry();
      DatabaseEntry entryValue = new DatabaseEntry();
      entryKey.setData(entry.getKey());
      OperationStatus status = cursor.getSearchKeyRange(entryKey, entryValue, LockMode.DEFAULT);
      entry.setKey(entryKey.getData()).setValue(entryValue.getData());
      return status.equals(OperationStatus.SUCCESS);
    }
  }

  public long count() {
    return this.db.count();
  }
}
