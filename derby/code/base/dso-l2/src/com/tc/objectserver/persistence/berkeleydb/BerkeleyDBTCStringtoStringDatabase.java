/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.berkeleydb;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.persistence.TCDatabaseEntry;
import com.tc.objectserver.persistence.TCStringToStringDatabase;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.Conversion;

public class BerkeleyDBTCStringtoStringDatabase extends AbstractBerkeleyDatabase implements TCStringToStringDatabase {

  public BerkeleyDBTCStringtoStringDatabase(Database db) {
    super(db);
  }

  public Status get(TCDatabaseEntry<String, String> entry, PersistenceTransaction tx) {
    DatabaseEntry dkey = new DatabaseEntry();
    dkey.setData(Conversion.string2Bytes(entry.getKey()));
    DatabaseEntry dvalue = new DatabaseEntry();
    OperationStatus status = this.db.get(pt2nt(tx), dkey, dvalue, LockMode.DEFAULT);
    if (status.equals(OperationStatus.SUCCESS)) {
      entry.setValue(Conversion.bytes2String(dvalue.getData()));
      return Status.SUCCESS;
    } else if (status.equals(OperationStatus.NOTFOUND)) { return Status.NOT_FOUND; }
    return Status.NOT_SUCCESS;
  }

  public Status put(String key, String value, PersistenceTransaction tx) {
    DatabaseEntry dkey = new DatabaseEntry();
    dkey.setData(Conversion.string2Bytes(key));
    DatabaseEntry dvalue = new DatabaseEntry();
    dvalue.setData(Conversion.string2Bytes(value));
    return this.db.put(pt2nt(tx), dkey, dvalue).equals(OperationStatus.SUCCESS) ? Status.SUCCESS : Status.NOT_SUCCESS;
  }

  public Status delete(String key, PersistenceTransaction tx) {
    DatabaseEntry dkey = new DatabaseEntry();
    dkey.setData(Conversion.string2Bytes(key));
    OperationStatus status = this.db.delete(pt2nt(tx), dkey);
    if (status.equals(OperationStatus.SUCCESS)) {
      return Status.SUCCESS;
    } else if (status.equals(OperationStatus.NOTFOUND)) { return Status.NOT_FOUND; }
    return Status.NOT_SUCCESS;
  }

}
