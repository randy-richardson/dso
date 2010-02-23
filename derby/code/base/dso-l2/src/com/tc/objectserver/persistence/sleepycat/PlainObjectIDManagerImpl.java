/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.CursorConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.TCObjectDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;

import java.util.Set;

public class PlainObjectIDManagerImpl extends SleepycatPersistorBase implements ObjectIDManager {
  private static final TCLogger                logger = TCLogging.getTestingLogger(PlainObjectIDManagerImpl.class);

  private final TCObjectDatabase               objectDB;
  private final PersistenceTransactionProvider ptp;
  private final CursorConfig                   dBCursorConfig;
  private final boolean                        isMeasurePerf;

  public PlainObjectIDManagerImpl(TCObjectDatabase objectDB, PersistenceTransactionProvider ptp) {
    this.objectDB = objectDB;
    this.ptp = ptp;
    this.dBCursorConfig = new CursorConfig();
    this.dBCursorConfig.setReadCommitted(true);

    isMeasurePerf = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_MEASURE_PERF, false);

  }

  public Runnable getObjectIDReader(SyncObjectIdSet rv) {
    return new ObjectIdReader(rv);
  }

  public Runnable getMapsObjectIDReader(SyncObjectIdSet rv) {
    return null;
  }

  public boolean deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) {
    return true;
  }

  public boolean put(PersistenceTransaction tx, ManagedObject mo) {
    return true;
  }

  public void prePutAll(Set<ObjectID> oidSet, ManagedObject mo) {
    return;
  }

  public boolean putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) {
    return true;
  }

  /*
   * the old/slow reading object-Ids at server restart
   */
  private class ObjectIdReader implements Runnable {
    protected final SyncObjectIdSet set;
    long                            startTime;

    public ObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void run() {
      if (isMeasurePerf) startTime = System.currentTimeMillis();
      PersistenceTransaction tx = ptp.newTransaction();

      ObjectIDSet tmp = objectDB.getAllObjectIds(tx);
      if (isMeasurePerf) {
        long elapse_time = System.currentTimeMillis() - startTime;
        double avg_time = ((double) tmp.size()) / elapse_time;
        logger.info("MeasurePerf: reading " + tmp.size() + " OIDs took " + elapse_time + "ms avg(1000 objs):"
                    + avg_time + " ms");
      }
      set.stopPopulating(tmp);
    }
  }
}
