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
  private final SortedSet<ObjectID> objectsToDelete = new ObjectIDSet();
  private volatile Sink             deleteObjectSink;

  public synchronized void deleteObjects(SortedSet<ObjectID> objects) {
    if (!objects.isEmpty()) {
      objectsToDelete.addAll(objects);
      deleteObjects();
    }
  }

  public synchronized SortedSet<ObjectID> getObjectsToDelete() {
    SortedSet<ObjectID> oids = new ObjectIDSet(objectsToDelete);
    objectsToDelete.clear();
    return oids;
  }

  public synchronized void deleteObjectsIfNecessary() {
    if (objectsToDelete.size() > 0) {
      deleteObjects();
    }
  }

  private void deleteObjects() {
    deleteObjectSink.addLossy(new DeleteObjectContext());
  }

  public void setDeleteObjectSink(Sink deleteObjectSink) {
    this.deleteObjectSink = deleteObjectSink;
  }
}
