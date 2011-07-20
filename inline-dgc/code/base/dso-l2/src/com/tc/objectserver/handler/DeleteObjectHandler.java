/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.DeleteObjectManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.GarbageDisposalContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import java.util.SortedSet;

public class DeleteObjectHandler extends AbstractEventHandler {
  private ObjectManager             objectManager;
  private final DeleteObjectManager deleteObjectManager;

  public DeleteObjectHandler(final DeleteObjectManager deleteObjectManager) {
    this.deleteObjectManager = deleteObjectManager;
  }

  @Override
  public void handleEvent(EventContext context) {
    final SortedSet<ObjectID> objectsToDelete = deleteObjectManager.getObjectsToDelete();
    objectManager.deleteObjects(new GarbageDisposalContext(objectsToDelete));
    deleteObjectManager.deleteObjectsIfNecessary();
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    objectManager = scc.getObjectManager();
  }
}
