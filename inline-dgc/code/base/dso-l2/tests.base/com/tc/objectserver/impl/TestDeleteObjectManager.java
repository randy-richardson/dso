/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.DeleteObjectManager;
import com.tc.util.TCCollections;

import java.util.SortedSet;

public class TestDeleteObjectManager implements DeleteObjectManager {

  public void deleteObjects(SortedSet<ObjectID> objects) {
    // do nothing
  }

  public SortedSet<ObjectID> getObjectsToDelete() {
    return TCCollections.EMPTY_OBJECT_ID_SET;
  }

  public void deleteObjectsIfNecessary() {
    // do nothing
  }

  public void setDeleteObjectSink(Sink deleteObjectSink) {
    // do nothing
  }

}
