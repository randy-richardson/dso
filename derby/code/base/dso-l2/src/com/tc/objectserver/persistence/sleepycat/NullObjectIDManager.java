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

import java.util.Set;

public class NullObjectIDManager implements ObjectIDManager {

  public Runnable getObjectIDReader(final SyncObjectIdSet rv) {
    // a dummy one, just stop populating and return
    return new Runnable() {
      public void run() {
        rv.stopPopulating(new ObjectIDSet());
        return;
      }
    };
  }

  public Runnable getMapsObjectIDReader(final SyncObjectIdSet rv) {
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

}
