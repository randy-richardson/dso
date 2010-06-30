/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;

import java.util.SortedSet;

public class NullObjectIDManager implements ObjectIDManager {

  public Runnable getObjectIDReader(final SyncObjectIdSet rv) {
    return returnDummy(rv);
  }

  private Runnable returnDummy(final SyncObjectIdSet rv) {
    // a dummy one, just stop populating and return
    return new Runnable() {
      public void run() {
        rv.stopPopulating(new ObjectIDSet());
        return;
      }
    };
  }

  public Runnable getMapsObjectIDReader(final SyncObjectIdSet rv) {
    return returnDummy(rv);
  }

<<<<<<< .working
  public boolean deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) {
    return true;
=======
  public Runnable getEvictableObjectIDReader(final SyncObjectIdSet rv) {
    return returnDummy(rv);
>>>>>>> .merge-right.r15747
  }

<<<<<<< .working
  public boolean put(PersistenceTransaction tx, ManagedObject mo) {
    return true;
=======
  public OperationStatus deleteAll(final PersistenceTransaction tx, final SortedSet<ObjectID> oidsToDelete,
                                   final SyncObjectIdSet extantMapTypeOidSet,
                                   final SyncObjectIdSet extantEvictableOidSet) {
    return OperationStatus.SUCCESS;
>>>>>>> .merge-right.r15747
  }

  public OperationStatus put(final PersistenceTransaction tx, final ManagedObject mo) {
    return OperationStatus.SUCCESS;
  }

<<<<<<< .working
  public boolean putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) {
    return true;
=======
  public OperationStatus putAll(final PersistenceTransaction tx, final SortedSet<ManagedObject> managedObjects) {
    return OperationStatus.SUCCESS;
>>>>>>> .merge-right.r15747
  }
}
