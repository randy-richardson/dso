/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.TCBytesToBytesDatabase;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.util.Conversion;
import com.tc.util.OidBitsArrayMap;
import com.tc.util.OidBitsArrayMapImpl;
import com.tc.util.OidLongArray;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class OidBitsArrayMapDiskStoreImpl extends OidBitsArrayMapImpl implements OidBitsArrayMap {

  private static final TCLogger                logger = TCLogging.getTestingLogger(FastObjectIDManagerImpl.class);

  private final TCBytesToBytesDatabase         oidDB;
  private final int                            auxKey;
  private final PersistenceTransactionProvider ptp;

  /*
   * Compressed bits array for ObjectIDs, backed up by a database. If null database, then only in-memory representation.
   */
  public OidBitsArrayMapDiskStoreImpl(int longsPerDiskUnit, TCBytesToBytesDatabase oidDB,
                                      PersistenceTransactionProvider ptp) {
    this(longsPerDiskUnit, oidDB, 0, ptp);
  }

  /*
   * auxKey: (main key + auxKey) to store different data entry to same db.
   */
  public OidBitsArrayMapDiskStoreImpl(int longsPerDiskUnit, TCBytesToBytesDatabase oidDB, int auxKey,
                                      PersistenceTransactionProvider ptp) {
    super(longsPerDiskUnit);
    this.oidDB = oidDB;
    this.auxKey = auxKey;
    this.ptp = ptp;
  }

  @Override
  protected OidLongArray loadArray(long oid, int lPerDiskUnit, long mapIndex) {
    OidLongArray longAry = null;
    try {
      if (oidDB != null) {
        longAry = readDiskEntry(ptp.newTransaction(), oid);
      }
    } catch (TCDatabaseException e) {
      logger.error("Reading object ID " + oid + ":" + e.getMessage());
    }
    if (longAry == null) longAry = super.loadArray(oid, lPerDiskUnit, mapIndex);
    return longAry;
  }

  OidLongArray readDiskEntry(PersistenceTransaction txn, long oid) throws TCDatabaseException {
    try {
      long aryIndex = oidIndex(oid);
      byte[] val = oidDB.get(Conversion.long2Bytes(aryIndex + auxKey), txn);
      if (val != null) { return new OidLongArray(aryIndex, val); }
      return null;
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    } finally {
      if (txn != null) {
        txn.commit();
      }
    }
  }

  void writeDiskEntry(PersistenceTransaction txn, OidLongArray bits) throws TCDatabaseException {
    byte[] key = bits.keyToBytes(auxKey);

    try {
      if (!bits.isZero()) {
        if (this.oidDB.put(key, bits.arrayToBytes(), txn) != Status.SUCCESS) {
          //
          throw new TCDatabaseException("Failed to update oidDB at " + bits.getKey());
        }
      } else {
        // OperationStatus.NOTFOUND happened if added and then deleted in the same batch
        if (this.oidDB.delete(key, txn) != Status.SUCCESS) {
          //
          throw new TCDatabaseException("Failed to delete oidDB at " + bits.getKey());
        }
      }
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  /*
   * flush in-memory entry to disk
   */
  public void flushToDisk(PersistenceTransaction tx) throws TCDatabaseException {
    Iterator<Map.Entry<Long, OidLongArray>> i = map.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry<Long, OidLongArray> entry = i.next();
      OidLongArray ary = entry.getValue();
      writeDiskEntry(tx, ary);
      if (ary.isZero()) i.remove();
    }
    map.clear();
  }

  // for testing
  TreeMap<Long, OidLongArray> getMap() {
    return map;
  }

  // for testing
  int getAuxKey() {
    return auxKey;
  }

}
