/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.DeleteObjectManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.GarbageDisposalContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector;

import java.util.SortedSet;

public class DeleteObjectHandler extends AbstractEventHandler {
  private static final TCLogger logger = TCLogging.getLogger(DeleteObjectHandler.class);
  private ObjectManager         objectManager;
  private GarbageCollector      garbageCollector;
  private DeleteObjectManager   deleteObjectManager;

  @Override
  public void handleEvent(EventContext context) {
    if (!garbageCollector.requestInlineGCDeleteStart()) {
      logger.info("Couldn't get exclusive access to perform inline-dgc. Skipping this round.");
      return;
    }
    final SortedSet<ObjectID> objectsToDelete = deleteObjectManager.nextObjectsToDelete();
    objectManager.deleteObjects(new GarbageDisposalContext(objectsToDelete));
    garbageCollector.notifyGCComplete();
    deleteObjectManager.deleteMoreObjectsIfNecessary();
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    objectManager = scc.getObjectManager();
    garbageCollector = scc.getObjectManager().getGarbageCollector();
    deleteObjectManager = scc.getDeleteObjectManager();
  }
}
