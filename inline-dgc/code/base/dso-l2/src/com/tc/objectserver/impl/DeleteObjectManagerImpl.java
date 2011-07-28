/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.DeleteObjectManager;
import com.tc.objectserver.context.DeleteObjectContext;
import com.tc.util.ObjectIDSet;

import java.util.SortedSet;

public class DeleteObjectManagerImpl implements DeleteObjectManager {
  private SortedSet<ObjectID> objectsToDelete = new ObjectIDSet();
  private final Sink          deleteObjectSink;

  public DeleteObjectManagerImpl(Sink deleteObjectSink) {
    this.deleteObjectSink = deleteObjectSink;
  }

  public synchronized void deleteObjects(SortedSet<ObjectID> objects) {
    if (!objects.isEmpty()) {
      objectsToDelete.addAll(objects);
      deleteObjects();
    }
  }

  public synchronized SortedSet<ObjectID> nextObjectsToDelete() {
    SortedSet<ObjectID> temp = objectsToDelete;
    objectsToDelete = new ObjectIDSet();
    return temp;
  }

  public synchronized void deleteMoreObjectsIfNecessary() {
    if (!objectsToDelete.isEmpty()) {
      deleteObjects();
    }
  }

  private void deleteObjects() {
    deleteObjectSink.addLossy(new DeleteObjectContext());
  }
}
