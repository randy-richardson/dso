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
import com.sleepycat.je.Transaction;
import com.tc.objectserver.persistence.TCDatabaseConstants;
import com.tc.objectserver.persistence.TCLongDatabase;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.util.Conversion;

import java.util.HashSet;
import java.util.Set;

public class BerkeleyDBTCLongDatabase extends AbstractBerkeleyDatabase implements TCLongDatabase {
  public static final DatabaseEntry value = new DatabaseEntry();
  private final CursorConfig        cursorConfig;

  public BerkeleyDBTCLongDatabase(Database db) {
    super(db);
    value.setData(Conversion.long2Bytes(0));
    this.cursorConfig = new CursorConfig();
    this.cursorConfig.setReadCommitted(true);
  }

  public boolean contains(long key, PersistenceTransaction tx) {
    DatabaseEntry entryKey = new DatabaseEntry();
    entryKey.setData(Conversion.long2Bytes(key));
    OperationStatus status = db.get(pt2nt(tx), entryKey, value, LockMode.DEFAULT);
    return status.equals(OperationStatus.SUCCESS);
  }

  public Set<Long> getAllKeys(PersistenceTransaction tx) {
    Set<Long> set = new HashSet<Long>();
    DatabaseEntry key = new DatabaseEntry();
    Cursor cursor;
    try {
      cursor = db.openCursor(pt2nt(tx), cursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        set.add(Conversion.bytes2Long(key.getData()));
      }
      cursor.close();
      tx.commit();
    } catch (Exception e) {
      e.printStackTrace();
      throw new DBException(e);
    }
    return set;
  }

  public Status put(long key, PersistenceTransaction tx) {
    DatabaseEntry entryKey = new DatabaseEntry();
    entryKey.setData(Conversion.long2Bytes(key));
    OperationStatus status = db.put(pt2nt(tx), entryKey, value);
    return status.equals(OperationStatus.SUCCESS) ? Status.SUCCESS : Status.NOT_SUCCESS;
  }

  public TCDatabaseConstants.Status delete(long key, PersistenceTransaction tx) {
    Transaction realTx = pt2nt(tx);
    DatabaseEntry entryKey = new DatabaseEntry();
    entryKey.setData(Conversion.long2Bytes(key));
    OperationStatus status = db.delete(realTx, entryKey);
    if (status.equals(OperationStatus.SUCCESS)) {
      return TCDatabaseConstants.Status.SUCCESS;
    } else if (status.equals(OperationStatus.NOTFOUND)) {
      return TCDatabaseConstants.Status.NOT_FOUND;
    } else {
      return TCDatabaseConstants.Status.NOT_SUCCESS;
    }
  }
}
