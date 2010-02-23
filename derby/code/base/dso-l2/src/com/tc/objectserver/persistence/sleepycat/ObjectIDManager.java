/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.SyncObjectIdSet;

import java.util.Set;

public interface ObjectIDManager {

  public boolean put(PersistenceTransaction tx, ManagedObject mo) throws TCDatabaseException;

  public void prePutAll(Set<ObjectID> oidSet, ManagedObject mo);

  public boolean putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException;

  public boolean deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException;

  public Runnable getObjectIDReader(SyncObjectIdSet objectIDSet);
  
  public Runnable getMapsObjectIDReader(SyncObjectIdSet objectIDSet);
}
