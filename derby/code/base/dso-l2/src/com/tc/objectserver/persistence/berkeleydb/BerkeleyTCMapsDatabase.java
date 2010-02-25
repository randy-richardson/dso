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
import com.tc.objectserver.persistence.TCMapsDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;
import com.tc.util.Conversion;

public class BerkeleyTCMapsDatabase extends BerkeleyTCBytesBytesDatabase implements TCMapsDatabase {

  public BerkeleyTCMapsDatabase(Database db) {
    super(db);
  }

  public int deleteCollection(long objectID, PersistenceTransaction tx) throws TCDatabaseException {
    int written = 0;
    Cursor c = db.openCursor(pt2nt(tx), CursorConfig.READ_COMMITTED);
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

  private boolean partialMatch(byte[] idbytes, byte[] key) {
    if (key.length < idbytes.length) return false;
    for (int i = 0; i < idbytes.length; i++) {
      if (idbytes[i] != key[i]) return false;
    }
    return true;
  }
}
