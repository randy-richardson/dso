/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.DBEnvironment;
import com.tc.objectserver.persistence.TCBytesToBytesDatabase;
import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.TCDatabaseEntry;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet;
import com.tc.util.OidLongArray;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.sequence.MutableSequence;

import java.util.Set;

public final class FastObjectIDManagerImpl extends SleepycatPersistorBase implements ObjectIDManager {
  private static final TCLogger                logger              = TCLogging
                                                                       .getTestingLogger(FastObjectIDManagerImpl.class);
  private final static int                     SEQUENCE_BATCH_SIZE = 50000;
  private final static int                     MINIMUM_WAIT_TIME   = 1000;
  private final byte                           PERSIST_COLL        = (byte) 1;
  private final byte                           NOT_PERSIST_COLL    = (byte) 0;
  private final byte                           ADD_OBJECT_ID       = (byte) 0;
  private final byte                           DEL_OBJECT_ID       = (byte) 1;
  // property
  private final int                            checkpointMaxLimit;
  private final int                            checkpointMaxSleep;
  private final int                            longsPerDiskEntry;
  private final int                            longsPerStateEntry;
  private final boolean                        isMeasurePerf;
  private final Object                         checkpointLock      = new Object();
  private final Object                         objectIDUpdateLock  = new Object();

  private final TCBytesToBytesDatabase         objectOidStoreDB;
  private final TCBytesToBytesDatabase         mapsOidStoreDB;
  private final TCBytesToBytesDatabase         oidStoreLogDB;
  private final PersistenceTransactionProvider ptp;
  private final CheckpointRunner               checkpointThread;
  private final MutableSequence                sequence;
  private long                                 nextSequence;
  private long                                 endSequence;
  private final ManagedObjectPersistor         managedObjectPersistor;

  public FastObjectIDManagerImpl(DBEnvironment env, PersistenceTransactionProvider ptp, MutableSequence sequence,
                                 ManagedObjectPersistor managedObjectPersistor) throws TCDatabaseException {
    this.managedObjectPersistor = managedObjectPersistor;
    this.objectOidStoreDB = env.getObjectOidStoreDatabase();
    this.mapsOidStoreDB = env.getMapsOidStoreDatabase();
    this.oidStoreLogDB = env.getOidStoreLogDatabase();
    this.ptp = ptp;

    checkpointMaxLimit = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXLIMIT);
    checkpointMaxSleep = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXSLEEP);
    longsPerDiskEntry = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_LONGS_PERDISKENTRY);
    longsPerStateEntry = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_MAPDB_LONGS_PERDISKENTRY);
    isMeasurePerf = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_MEASURE_PERF, false);

    this.sequence = sequence;
    nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
    endSequence = nextSequence + SEQUENCE_BATCH_SIZE;

    // process left over from previous run
    processPreviousRunToCompressedStorage();

    // start checkpoint thread
    checkpointThread = new CheckpointRunner(checkpointMaxSleep);
    checkpointThread.setDaemon(true);
    checkpointThread.start();
  }

  /*
   * A thread to read in ObjectIDs from compressed DB at server restart
   */
  public Runnable getObjectIDReader(SyncObjectIdSet objectIDSet) {
    return new OidObjectIdReader(objectOidStoreDB, objectIDSet);
  }

  /*
   * A thread to read in MapType ObjectIDs from compressed DB at server restart
   */
  public Runnable getMapsObjectIDReader(SyncObjectIdSet objectIDSet) {
    return new OidObjectIdReader(mapsOidStoreDB, objectIDSet);
  }

  public void stopCheckpointRunner() {
    checkpointThread.quit();
  }

  private synchronized long nextSeqID() {
    if (nextSequence == endSequence) {
      nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
      endSequence = nextSequence + SEQUENCE_BATCH_SIZE;
    }
    return (nextSequence++);
  }

  /*
   * Log key to make log records ordered in time sequence
   */
  private byte[] makeLogKey(boolean isAdd) {
    byte[] rv = new byte[OidLongArray.BYTES_PER_LONG + 1];
    Conversion.writeLong(nextSeqID(), rv, 0);
    rv[OidLongArray.BYTES_PER_LONG] = isAdd ? ADD_OBJECT_ID : DEL_OBJECT_ID;
    return rv;
  }

  /*
   * Log ObjectID+MapType in db value
   */
  private byte[] makeLogValue(ManagedObject mo) {
    byte[] rv = new byte[OidLongArray.BYTES_PER_LONG + 1];
    Conversion.writeLong(mo.getID().toLong(), rv, 0);
    rv[OidLongArray.BYTES_PER_LONG] = isPersistStateObject(mo) ? PERSIST_COLL : NOT_PERSIST_COLL;
    return rv;
  }

  private boolean isPersistStateObject(ManagedObject mo) {
    return PersistentCollectionsUtil.isPersistableCollectionType(mo.getManagedObjectState().getType());
  }

  private boolean isAddOper(byte[] logKey) {
    return (logKey[OidLongArray.BYTES_PER_LONG] == ADD_OBJECT_ID);
  }

  /*
   * Log the change of an ObjectID, added or deleted. Later, flush to BitsArray OidDB by checkpoint thread.
   */
  private boolean logObjectID(PersistenceTransaction tx, byte[] oids, boolean isAdd) throws TCDatabaseException {
    boolean rtn;
    try {
      rtn = this.oidStoreLogDB.putNoOverwrite(tx, makeLogKey(isAdd), oids) == Status.SUCCESS;
    } catch (Exception t) {
      throw new TCDatabaseException(t.getMessage());
    }
    return rtn;
  }

  private boolean logAddObjectID(PersistenceTransaction tx, ManagedObject mo) throws TCDatabaseException {
    return logObjectID(tx, makeLogValue(mo), true);
  }

  /*
   * Flush out oidLogDB to bitsArray on disk.
   */
  boolean flushToCompressedStorage(StoppedFlag stoppedFlag, int maxProcessLimit) {
    boolean isAllFlushed = true;
    synchronized (objectIDUpdateLock) {
      if (stoppedFlag.isStopped()) return isAllFlushed;
      OidBitsArrayMapDiskStoreImpl oidStoreMap = new OidBitsArrayMapDiskStoreImpl(longsPerDiskEntry,
                                                                                  this.objectOidStoreDB, ptp);
      OidBitsArrayMapDiskStoreImpl mapOidStoreMap = new OidBitsArrayMapDiskStoreImpl(longsPerStateEntry,
                                                                                     this.mapsOidStoreDB, ptp);
      PersistenceTransaction tx = null;
      try {
        tx = ptp.newTransaction();
        TCDatabaseCursor<byte[], byte[]> cursor = oidStoreLogDB.openCursorUpdatable(tx);
        TCDatabaseEntry<byte[], byte[]> entry = new TCDatabaseEntry<byte[], byte[]>();
        int changes = 0;
        try {
          while (cursor.getNext(entry)) {

            if (stoppedFlag.isStopped()) {
              cursor.close();
              cursor = null;
              abortOnError(tx);
              return isAllFlushed;
            }

            boolean isAddOper = isAddOper(entry.getKey());
            byte[] oids = entry.getValue();
            int offset = 0;
            while (offset < oids.length) {
              ObjectID objectID = new ObjectID(Conversion.bytes2Long(oids, offset));
              if (isAddOper) {
                oidStoreMap.getAndSet(objectID);
                if (oids[offset + OidLongArray.BYTES_PER_LONG] == PERSIST_COLL) {
                  mapOidStoreMap.getAndSet(objectID);
                }
              } else {
                oidStoreMap.getAndClr(objectID);
                if (oids[offset + OidLongArray.BYTES_PER_LONG] == PERSIST_COLL) {
                  mapOidStoreMap.getAndClr(objectID);
                }
              }

              offset += OidLongArray.BYTES_PER_LONG + 1;
              ++changes;
            }
            cursor.delete();

            if (maxProcessLimit > 0 && changes >= maxProcessLimit) {
              cursor.close();
              cursor = null;
              isAllFlushed = false;
              break;
            }
          }
        } catch (Exception e) {
          throw new TCDatabaseException(e.getMessage());
        } finally {
          if (cursor != null) cursor.close();
          cursor = null;
        }
        oidStoreMap.flushToDisk(tx);
        mapOidStoreMap.flushToDisk(tx);

        tx.commit();
        logger.debug("Checkpoint updated " + changes + " objectIDs");
      } catch (TCDatabaseException e) {
        logger.error("Error ojectID checkpoint: " + e);
        abortOnError(tx);
      }
    }
    return isAllFlushed;
  }

  private void processPreviousRunToCompressedStorage() {
    flushToCompressedStorage(new StoppedFlag(), Integer.MAX_VALUE);
  }

  private boolean checkpointToCompressedStorage(StoppedFlag stoppedFlag, int maxProcessLimit) {
    return flushToCompressedStorage(stoppedFlag, maxProcessLimit);
  }

  static class StoppedFlag {
    private volatile boolean isStopped = false;

    public boolean isStopped() {
      return isStopped;
    }

    public void setStopped(boolean stopped) {
      this.isStopped = stopped;
    }
  }

  /*
   * Periodically flush oid from oidLogDB to oidDB
   */
  private class CheckpointRunner extends Thread {
    private final int         maxSleep;
    private final StoppedFlag stoppedFlag = new StoppedFlag();

    public CheckpointRunner(int maxSleep) {
      super("ObjectID-CompressedStorage Checkpoint");
      this.maxSleep = maxSleep;
    }

    public void quit() {
      stoppedFlag.setStopped(true);
    }

    @Override
    public void run() {
      int currentwait = maxSleep;
      int maxProcessLimit = checkpointMaxLimit;
      while (!stoppedFlag.isStopped()) {
        // Sleep for enough changes or specified time-period
        synchronized (checkpointLock) {
          try {
            checkpointLock.wait(currentwait);
          } catch (InterruptedException e) {
            //
          }
        }

        if (stoppedFlag.isStopped()) break;
        boolean isAllFlushed = checkpointToCompressedStorage(stoppedFlag, maxProcessLimit);

        if (isAllFlushed) {
          // All flushed, wait longer for next time
          currentwait += currentwait;
          if (currentwait > maxSleep) {
            currentwait = maxSleep;
            maxProcessLimit = checkpointMaxLimit;
          }
        } else {
          // reduce wait time to catch up
          currentwait = currentwait / 2;
          // at least wait 1 second
          if (currentwait < MINIMUM_WAIT_TIME) {
            currentwait = MINIMUM_WAIT_TIME;
            maxProcessLimit = -1; // unlimited
          }
        }
      }
    }
  }

  /*
   * fast way to load object-Ids at server restart by reading them from bits array
   */
  private class OidObjectIdReader implements Runnable {
    private long                         startTime;
    private int                          counter = 0;
    private final TCBytesToBytesDatabase oidDB;
    private final SyncObjectIdSet        syncObjectIDSet;

    public OidObjectIdReader(TCBytesToBytesDatabase oidDB, SyncObjectIdSet syncObjectIDSet) {
      this.oidDB = oidDB;
      this.syncObjectIDSet = syncObjectIDSet;
    }

    public void run() {
      if (isMeasurePerf) startTime = System.currentTimeMillis();

      ObjectIDSet tmp = new ObjectIDSet();
      PersistenceTransaction tx = ptp.newTransaction();
      TCDatabaseCursor<byte[], byte[]> cursor = null;
      try {
        cursor = oidDB.openCursor(tx);
        TCDatabaseEntry<byte[], byte[]> entry = new TCDatabaseEntry<byte[], byte[]>();
        while (cursor.getNext(entry)) {
          OidLongArray bitsArray = new OidLongArray(entry.getKey(), entry.getValue());
          makeObjectIDFromBitsArray(bitsArray, tmp);
        }
        cursor.close();
        cursor = null;

        safeCommit(tx);
        if (isMeasurePerf) {
          logger.info("MeasurePerf: done");
        }
      } catch (Throwable t) {
        logger.error("Error Reading Object IDs", t);
      } finally {
        safeClose(cursor);
        syncObjectIDSet.stopPopulating(tmp);
      }
    }

    private void makeObjectIDFromBitsArray(OidLongArray entry, ObjectIDSet tmp) {
      long oid = entry.getKey();
      long[] ary = entry.getArray();
      for (int j = 0; j < ary.length; ++j) {
        long bit = 1L;
        long bits = ary[j];
        for (int i = 0; i < OidLongArray.BITS_PER_LONG; ++i) {
          if ((bits & bit) != 0) {
            tmp.add(new ObjectID(oid));
            if (isMeasurePerf && ((++counter % 1000) == 0)) {
              long elapse_time = System.currentTimeMillis() - startTime;
              long avg_time = elapse_time / (counter / 1000);
              logger.info("MeasurePerf: reading " + counter + " OIDs took " + elapse_time + "ms avg(1000 objs):"
                          + avg_time + " ms");
            }
          }
          bit <<= 1;
          ++oid;
        }
      }
    }

    protected void safeCommit(PersistenceTransaction tx) {
      if (tx == null) return;
      try {
        tx.commit();
      } catch (Throwable t) {
        logger.error("Error Committing Transaction", t);
      }
    }

    protected void safeClose(TCDatabaseCursor c) {
      if (c == null) return;

      try {
        c.close();
      } catch (Throwable e) {
        logger.error("Error closing cursor", e);
      }
    }
  }

  public boolean put(PersistenceTransaction tx, ManagedObject mo) throws TCDatabaseException {
    return (logAddObjectID(tx, mo));
  }

  public void prePutAll(Set<ObjectID> oidSet, ManagedObject mo) {
    oidSet.add(mo.getID());
  }

  private boolean doAll(PersistenceTransaction tx, Set<ObjectID> oidSet, boolean isAdd) throws TCDatabaseException {
    boolean status = true;
    int size = oidSet.size();
    if (size == 0) return status;

    byte[] oids = new byte[size * (OidLongArray.BYTES_PER_LONG + 1)];
    int offset = 0;
    for (ObjectID objectID : oidSet) {
      Conversion.writeLong(objectID.toLong(), oids, offset);
      oids[offset + OidLongArray.BYTES_PER_LONG] = managedObjectPersistor.containsMapType(objectID) ? PERSIST_COLL
          : NOT_PERSIST_COLL;
      offset += OidLongArray.BYTES_PER_LONG + 1;
    }
    try {
      status = logObjectID(tx, oids, isAdd);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
    return status;
  }

  public boolean putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    return doAll(tx, oidSet, true);
  }

  public boolean deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    return doAll(tx, oidSet, false);
  }

}
