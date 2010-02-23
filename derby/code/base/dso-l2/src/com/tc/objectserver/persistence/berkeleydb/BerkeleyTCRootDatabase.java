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
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.TCRootDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.util.Conversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BerkeleyTCRootDatabase extends AbstractBerkeleyDatabase implements TCRootDatabase {
  private final Database     rootDB;
  private final CursorConfig rootDBCursorConfig = new CursorConfig();

  public BerkeleyTCRootDatabase(Database rootDB) {
    this.rootDB = rootDB;
    this.rootDBCursorConfig.setReadCommitted(true);
  }

  public long get(byte[] rootName, PersistenceTransaction tx) {
    OperationStatus status = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      key.setData(rootName);

      status = this.rootDB.get(pt2nt(tx), key, value, LockMode.DEFAULT);
      if (OperationStatus.SUCCESS.equals(status)) { return Conversion.bytes2Long(value.getData()); }
      if (OperationStatus.NOTFOUND.equals(status)) { return ObjectID.NULL_ID.toLong(); }
      throw new DBException("Could not retrieve root");
    } catch (Throwable t) {
      throw new DBException(t);
    }
  }

  public List<byte[]> getRootNames(PersistenceTransaction tx) {
    List<byte[]> rv = new ArrayList<byte[]>();
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = this.rootDB.openCursor(pt2nt(tx), this.rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(key.getData());
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public Set<ObjectID> getRootIds(PersistenceTransaction tx) {
    Set<ObjectID> rv = new HashSet<ObjectID>();
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = this.rootDB.openCursor(pt2nt(tx), this.rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(new ObjectID(Conversion.bytes2Long(value.getData())));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public Map<byte[], Long> getRootNamesToId(PersistenceTransaction tx) {
    Map<byte[], Long> rv = new HashMap<byte[], Long>();
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = this.rootDB.openCursor(pt2nt(tx), this.rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.put(key.getData(), Conversion.bytes2Long(value.getData()));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public boolean put(byte[] rootName, long id, PersistenceTransaction tx) {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    key.setData(rootName);
    value.setData(Conversion.long2Bytes(id));

    OperationStatus status = this.rootDB.put(pt2nt(tx), key, value);
    if (!status.equals(OperationStatus.SUCCESS)) return false;
    return true;
  }

  public Database getDatabase() {
    return rootDB;
  }
}
