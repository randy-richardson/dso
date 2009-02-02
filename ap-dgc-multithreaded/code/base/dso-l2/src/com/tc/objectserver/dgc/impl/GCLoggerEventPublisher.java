/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;

public class GCLoggerEventPublisher extends GarbageCollectorEventListenerAdapter {

  private final GCLogger gcLogger;

  public GCLoggerEventPublisher(TCLogger logger, boolean verboseGC) {
    gcLogger = new GCLogger(logger, verboseGC);
  }

  @Override
  public void garbageCollectorStart(GarbageCollectionInfo info) {
    gcLogger.log_GCStart(info.getIteration(), info.isFullGC());
  }

  @Override
  public void garbageCollectorMark(GarbageCollectionInfo info) {
    gcLogger.log_markStart(info.getBeginObjectCount());
  }

  @Override
  public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
    gcLogger.log_markResults(info.getPreRescueCount());
  }

  @Override
  public void garbageCollectorRescue1Complete(GarbageCollectionInfo info) {
    gcLogger.log_rescue_complete(1, info.getRescue1Count());
  }

  @Override
  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    gcLogger.log_quiescing();
  }

  @Override
  public void garbageCollectorPaused(GarbageCollectionInfo info) {
    gcLogger.log_paused();
  }

  @Override
  public void garbageCollectorRescue2Start(GarbageCollectionInfo info) {
    gcLogger.log_rescue_start(2, info.getCandidateGarbageCount());
  }

  @Override
  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    gcLogger.log_sweep(info.getDeleted());
    gcLogger.log_notifyGCComplete();
  }

  @Override
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    gcLogger.log_GCSweepCompleted(info.getIteration(), info.getDeleted().size(), info.getDeleted().size(), info
        .getDeleteStageTime());
  }

  @Override
  public void garbageCollectorDelete(GarbageCollectionInfo info) {
    gcLogger.log_sweepStart(info.getIteration(), info.getDeleted().size());
  }

  @Override
  public void garbageCollectorCycleCompleted(GarbageCollectionInfo info) {
    gcLogger.log_GCComplete(info, info.getRescueTimes());
  }

  public void garbageCollectorCanceled(GarbageCollectionInfo info) {
    gcLogger.log_GCCanceled(info);
  }

}
