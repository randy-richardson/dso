/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.SyncObjectIdSet;

import java.util.SortedSet;

public interface ObjectIDManager {

  public boolean put(PersistenceTransaction tx, ManagedObject mo) throws TCDatabaseException;

  public OperationStatus putAll(PersistenceTransaction tx, SortedSet<ManagedObject> managedObjects)
      throws TCDatabaseException;

<<<<<<< .working
  public boolean putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException;
=======
  public OperationStatus deleteAll(PersistenceTransaction tx, SortedSet<ObjectID> oidsToDelete,
                                   SyncObjectIdSet extantMapTypeOidSet, SyncObjectIdSet extantEvictableOidSet)
      throws TCDatabaseException;
>>>>>>> .merge-right.r15747

<<<<<<< .working
  public boolean deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException;
=======
  public Runnable getObjectIDReader(SyncObjectIdSet objectIDSet);
>>>>>>> .merge-right.r15747

  public Runnable getMapsObjectIDReader(SyncObjectIdSet objectIDSet);

  public Runnable getEvictableObjectIDReader(SyncObjectIdSet objectIDSet);

}
