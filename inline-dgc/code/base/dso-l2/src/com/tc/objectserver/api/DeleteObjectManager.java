/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.async.api.Sink;
import com.tc.object.ObjectID;

import java.util.SortedSet;

public interface DeleteObjectManager {
  public void deleteObjects(SortedSet<ObjectID> objects);

  public SortedSet<ObjectID> getObjectsToDelete();

  public void deleteObjectsIfNecessary();

  public void setDeleteObjectSink(Sink deleteObjectSink);
}
