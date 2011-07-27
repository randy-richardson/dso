/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.PeriodicDGCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.Util;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.NullLifeCycleState;
import com.tc.util.concurrent.StoppableThread;
import com.tc.util.sequence.DGCSequenceProvider;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 */
public class MarkAndSweepGarbageCollector implements GarbageCollector {

  static final TCLogger                        logger                     = TCLogging
                                                                              .getLogger(MarkAndSweepGarbageCollector.class);

  private static final LifeCycleState          NULL_LIFECYCLE_STATE       = new NullLifeCycleState();
  private static final int                     WAIT_INTERVAL              = 10 * 1000;
  private static final int                     WAIT_LOG_THRESHOLD         = 10 * 1000;

  private final GarbageCollectionInfoPublisher gcPublisher;
  private final ObjectManagerConfig            objectManagerConfig;
  private final ClientStateManager             stateManager;
  private final ObjectManager                  objectManager;
  private final DGCSequenceProvider            dgcSequenceProvider;

  private volatile State                       state                      = GC_SLEEP;
  private volatile ChangeCollector             referenceCollector         = ChangeCollector.NULL_CHANGE_COLLECTOR;
  private volatile YoungGenChangeCollector     youngGenReferenceCollector = YoungGenChangeCollector.NULL_YOUNG_CHANGE_COLLECTOR;
  private volatile LifeCycleState              gcState                    = new NullLifeCycleState();
  private volatile boolean                     started                    = false;
  private volatile boolean                     periodicEnabled            = false;

  public MarkAndSweepGarbageCollector(final ObjectManagerConfig objectManagerConfig, final ObjectManager objectMgr,
                                      final ClientStateManager stateManager,
                                      final GarbageCollectionInfoPublisher gcPublisher,
                                      DGCSequenceProvider dgcSequenceProvider) {
    this.objectManagerConfig = objectManagerConfig;
    this.objectManager = objectMgr;
    this.stateManager = stateManager;
    this.gcPublisher = gcPublisher;
    this.dgcSequenceProvider = dgcSequenceProvider;
    addListener(new GCLoggerEventPublisher(new GCLogger(logger, objectManagerConfig.verboseGC())));
  }

  public void doGC(final GCType type) {
    GCHook hook = null;
    switch (type) {
      case FULL_GC:
        hook = new FullGCHook(this, this.objectManager, this.stateManager);
        break;
      case YOUNG_GEN_GC:
        hook = new YoungGCHook(this, this.objectManager, this.stateManager, this.youngGenReferenceCollector);
        break;
    }
    final MarkAndSweepGCAlgorithm gcAlgo = new MarkAndSweepGCAlgorithm(this, hook, this.gcPublisher, this.gcState,
                                                                       this.dgcSequenceProvider.getNextId());
    gcAlgo.doGC();
  }

  public boolean deleteGarbage(final PeriodicDGCResultContext periodicDGCResultContext) {
    if (requestGCDeleteStart()) {
      this.youngGenReferenceCollector.removeGarbage(periodicDGCResultContext.getGarbageIDs());
      this.objectManager.notifyGCComplete(periodicDGCResultContext);
      notifyGCComplete();
      return true;
    }
    return false;
  }

  public void startMonitoringReferenceChanges() {
    this.referenceCollector = new NewReferenceCollector();
    this.youngGenReferenceCollector.startMonitoringChanges();
  }

  public void stopMonitoringReferenceChanges() {
    this.referenceCollector = ChangeCollector.NULL_CHANGE_COLLECTOR;
    this.youngGenReferenceCollector.stopMonitoringChanges();
  }

  public void changed(final ObjectID changedObject, final ObjectID oldReference, final ObjectID newReference) {
    this.referenceCollector.changed(changedObject, oldReference, newReference);
  }

  public void notifyObjectCreated(final ObjectID id) {
    this.youngGenReferenceCollector.notifyObjectCreated(id);
  }

  public void notifyNewObjectInitalized(final ObjectID id) {
    this.youngGenReferenceCollector.notifyObjectInitalized(id);
  }

  public void notifyObjectsEvicted(final Collection evicted) {
    this.youngGenReferenceCollector.notifyObjectsEvicted(evicted);
  }

  public void addNewReferencesTo(final Set rescueIds) {
    this.referenceCollector.addNewReferencesTo(rescueIds);
  }

  /**
   * Used for Tests.
   */
  ObjectIDSet collect(final GCHook hook, final Filter filter, final Collection rootIds,
                      final ObjectIDSet managedObjectIds) {
    return collect(hook, filter, rootIds, managedObjectIds, NULL_LIFECYCLE_STATE);
  }

  /**
   * Used for Tests.
   */
  ObjectIDSet collect(final GCHook hook, final Filter traverser, final Collection roots,
                      final ObjectIDSet managedObjectIds, final LifeCycleState lstate) {
    final MarkAndSweepGCAlgorithm gcAlgo = new MarkAndSweepGCAlgorithm(this, hook, this.gcPublisher, this.gcState,
                                                                       this.dgcSequenceProvider.getNextId());
    return gcAlgo.collect(traverser, roots, managedObjectIds, lstate);
  }

  public void start() {
    if (this.objectManagerConfig.isYoungGenDGCEnabled()) {
      this.youngGenReferenceCollector = new YoungGenChangeCollectorImpl();
    }
    this.started = true;
    this.gcState.start();
  }

  public void stop() {
    this.started = false;
    int count = 0;
    while (!this.gcState.stopAndWait(5000) && (count < 6)) {
      count++;
      logger.warn("DGC Thread did not stop");
    }
  }

  public boolean isStarted() {
    return this.started;
  }

  public void setPeriodicEnabled(final boolean periodicEnabled) {
    this.periodicEnabled = periodicEnabled;
  }

  public boolean isPeriodicEnabled() {
    return periodicEnabled;
  }

  public void setState(final StoppableThread st) {
    this.gcState = st;
  }

  public void addListener(final GarbageCollectorEventListener listener) {
    this.gcPublisher.addListener(listener);
  }

  public synchronized void waitToStartGC() {
    boolean isInterrupted = false;
    long lastLogTime = System.nanoTime();
    final long startTime = System.nanoTime();
    while (!requestGCStart()) {
      try {
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastLogTime) > WAIT_LOG_THRESHOLD) {
          logger.info("Waited " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + "ms to start DGC.");
          lastLogTime = System.nanoTime();
        }
        wait(WAIT_INTERVAL);
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public synchronized boolean requestGCStart() {
    if (this.started && this.state == GC_SLEEP) {
      this.state = GC_RUNNING;
      return true;
    }
    // Can't start DGC
    return false;
  }

  public synchronized void enableGC() {
    if (GC_DISABLED == this.state) {
      this.state = GC_SLEEP;
      notify();
    } else {
      logger.warn("DGC is already enabled : " + this.state);
    }
  }

  public synchronized void waitToDisableGC() {
    boolean isInterrupted = false;
    long lastLogTime = System.nanoTime();
    final long startTime = System.nanoTime();
    while (!requestDisableGC()) {
      try {
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastLogTime) > WAIT_LOG_THRESHOLD) {
          logger.info("Waited " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + "ms to disable DGC.");
          lastLogTime = System.nanoTime();
        }
        wait(WAIT_INTERVAL);
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public synchronized boolean requestDisableGC() {
    if (GC_SLEEP == this.state) {
      this.state = GC_DISABLED;
      return true;
    }
    // DGC is already running, can't be disabled
    return false;
  }

  public synchronized void notifyReadyToGC() {
    if (this.state == GC_PAUSING) {
      this.state = GC_PAUSED;
    }
  }

  public void notifyGCComplete() {
    this.state = GC_SLEEP;
  }

  public synchronized void waitToStartInlineGC() {
    boolean isInterrupted = false;
    long lastLogTime = System.nanoTime();
    final long startTime = System.nanoTime();
    while (!requestGCDeleteStart()) {
      try {
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastLogTime) > WAIT_LOG_THRESHOLD) {
          logger.info("Waited " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
                      + "ms to start inline DGC.");
          lastLogTime = System.nanoTime();
        }
        wait(WAIT_INTERVAL);
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  /**
   * In Active server, state transitions from GC_PAUSED to GC_DELETE and in the passive server, state transitions from
   * GC_SLEEP to GC_DELETE.
   */
  private synchronized boolean requestGCDeleteStart() {
    if (this.state == GC_SLEEP || this.state == GC_PAUSED) {
      this.state = GC_DELETE;
      return true;
    }
    return false;
  }

  public void requestGCPause() {
    this.state = GC_PAUSING;
  }

  public boolean isPausingOrPaused() {
    State localState = this.state;
    return GC_PAUSED == localState || GC_PAUSING == localState;
  }

  public boolean isPaused() {
    return this.state == GC_PAUSED;
  }

  public boolean isDisabled() {
    return GC_DISABLED == this.state;
  }

  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    return out.print(getClass().getName()).print("[").print(this.state).print("]");
  }
}
