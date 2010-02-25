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
import com.tc.objectserver.persistence.TCIntToBytesDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.Conversion;

import java.util.HashMap;
import java.util.Map;

public class BerkeleyTCIntToBytesDatabase extends AbstractBerkeleyDatabase implements TCIntToBytesDatabase {
  private final CursorConfig cursorConfig = new CursorConfig();

  public BerkeleyTCIntToBytesDatabase(Database db) {
    super(db);
    this.cursorConfig.setReadCommitted(true);
  }

  public byte[] get(int id, PersistenceTransaction tx) {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.int2Bytes(id));
    DatabaseEntry value = new DatabaseEntry();
    OperationStatus status = this.db.get(pt2nt(tx), key, value, LockMode.DEFAULT);
    if (OperationStatus.SUCCESS.equals(status)) { return value.getData(); }
    return null;
  }

  public Map<Integer, byte[]> getAll(PersistenceTransaction tx) {
    Map<Integer, byte[]> allClazzBytes = new HashMap<Integer, byte[]>();
    Cursor cursor = null;
    try {
      cursor = db.openCursor(pt2nt(tx), cursorConfig);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        allClazzBytes.put(new Integer(Conversion.bytes2Int(key.getData())), value.getData());
      }
      cursor.close();
      tx.commit();
    } catch (Exception e) {
      if (cursor != null) {
        cursor.close();
      }
      tx.abort();
    }
    return allClazzBytes;
  }

  public boolean put(int id, byte[] b, PersistenceTransaction tx) {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.int2Bytes(id));
    DatabaseEntry value = new DatabaseEntry();
    value.setData(b);
    OperationStatus status = this.db.put(pt2nt(tx), key, value);
    if (status.equals(OperationStatus.SUCCESS)) return true;
    return false;
  }

}