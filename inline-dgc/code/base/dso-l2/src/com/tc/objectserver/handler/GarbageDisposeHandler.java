/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.GarbageDisposalContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.persistence.api.ManagedObjectStore;

import java.util.SortedSet;

public class GarbageDisposeHandler extends AbstractEventHandler {

  private final GarbageCollectionInfoPublisher publisher;
  private ManagedObjectStore                   objectStore;

  public GarbageDisposeHandler(final GarbageCollectionInfoPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (!(context instanceof GarbageDisposalContext)) throw new AssertionError("Unknown context type: "
                                                                               + context.getClass().getName());

    final GarbageDisposalContext garbageDisposalContext = (GarbageDisposalContext) context;
    if (garbageDisposalContext instanceof GCResultContext) {
      doDGCDeletes((GCResultContext) garbageDisposalContext);
    } else {
      doGarbageDisposal(garbageDisposalContext);
    }
  }

  private void doDGCDeletes(GCResultContext gcResultContext) {
    final GarbageCollectionInfo gcInfo = gcResultContext.getGCInfo();
    final SortedSet<ObjectID> sortedGarbage = gcResultContext.getGarbageIDs();

    this.publisher.fireGCDeleteEvent(gcInfo);
    gcInfo.setActualGarbageCount(sortedGarbage.size());
    final long start = System.currentTimeMillis();

    doGarbageDisposal(gcResultContext);

    final long elapsed = System.currentTimeMillis() - start;
    gcInfo.setDeleteStageTime(elapsed);
    final long elapsedTime = System.currentTimeMillis() - gcInfo.getStartTime();
    gcInfo.setElapsedTime(elapsedTime);
    gcInfo.setEndObjectCount(this.objectStore.getObjectCount());
    this.publisher.fireGCCompletedEvent(gcInfo);

  }

  private void doGarbageDisposal(GarbageDisposalContext garbageDisposalContext) {
    this.objectStore.removeAllObjectsByIDNow(garbageDisposalContext.getGarbageIDs());
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.objectStore = scc.getObjectStore();
  }
}
