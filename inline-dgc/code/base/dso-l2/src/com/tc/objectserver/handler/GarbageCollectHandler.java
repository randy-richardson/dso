/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.DeleteObjectManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.DelayedGarbageCollectContext;
import com.tc.objectserver.context.GarbageCollectContext;
import com.tc.objectserver.context.InlineGCContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.ThreadUtil;

import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class GarbageCollectHandler extends AbstractEventHandler {

  private final Timer          timer     = new Timer();
  private final boolean        fullGCEnabled;
  private final boolean        youngGCEnabled;
  private final long           fullGCInterval;
  private final long           youngGCInterval;
  private final LifeCycleState gcState   = new GCState();
  private volatile boolean     gcRunning = false;
  private GarbageCollector     collector;
  private ObjectManager        objectManager;
  private DeleteObjectManager  deleteObjectManager;
  private Sink                 gcSink;

  public GarbageCollectHandler(final ObjectManagerConfig objectManagerConfig) {
    this.fullGCEnabled = objectManagerConfig.doGC();
    this.youngGCEnabled = objectManagerConfig.isYoungGenDGCEnabled();
    this.fullGCInterval = objectManagerConfig.gcThreadSleepTime();
    this.youngGCInterval = objectManagerConfig.getYoungGenDGCFrequencyInMillis();
  }

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof DelayedGarbageCollectContext) {
      DelayedGarbageCollectContext delayedGarbageCollectContext = (DelayedGarbageCollectContext) context;
      scheduleDGC(delayedGarbageCollectContext.getType(), delayedGarbageCollectContext.getDelay(),
                  delayedGarbageCollectContext.reschedule());
    } else if (context instanceof GarbageCollectContext) {
      GarbageCollectContext gcc = (GarbageCollectContext) context;
      gcRunning = true;
      collector.doGC(gcc.getType());
      gcRunning = false;
      scheduleInlineGC(); // give inline gc a chance to run before another full/young gc is started
      if (gcc.reschedule() && gcc.getType() == GCType.FULL_GC && fullGCEnabled) {
        scheduledFullGC(gcc.reschedule());
      } else if (gcc.reschedule() && gcc.getType() == GCType.YOUNG_GEN_GC && youngGCEnabled) {
        scheduleYoungGC(gcc.reschedule());
      }
    } else if (context instanceof InlineGCContext) {
      collector.waitToStartInlineGC();
      final SortedSet<ObjectID> objectsToDelete = deleteObjectManager.nextObjectsToDelete();
      objectManager.deleteObjects(new DGCResultContext(objectsToDelete));
      collector.notifyGCComplete();
      deleteObjectManager.deleteMoreObjectsIfNecessary();
    } else {
      throw new AssertionError("Unknown context type: " + context.getClass().getName());
    }
  }

  public void scheduleInlineGC() {
    gcSink.add(new InlineGCContext());
  }

  public void scheduleYoungGC(final boolean reschedule) {
    scheduleDGC(GCType.YOUNG_GEN_GC, youngGCInterval, reschedule);
  }

  public void scheduledFullGC(final boolean reschedule) {
    scheduleDGC(GCType.FULL_GC, fullGCInterval, reschedule);
  }

  public void scheduleDGC(final GCType type, final long delay, final boolean reschedule) {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        gcSink.add(new GarbageCollectContext(type, reschedule));
      }
    }, delay);
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    collector = scc.getObjectManager().getGarbageCollector();
    collector.setState(gcState);
    objectManager = scc.getObjectManager();
    deleteObjectManager = scc.getDeleteObjectManager();
    gcSink = scc.getStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE).getSink();
  }

  private class GCState implements LifeCycleState {
    private volatile boolean stopRequested = false;

    public void start() {
      if (fullGCEnabled) {
        if (youngGCEnabled) {
          scheduleYoungGC(true);
        }
        scheduledFullGC(true);
        collector.setPeriodicEnabled(true);
      }
    }

    public boolean isStopRequested() {
      return stopRequested;
    }

    public boolean stopAndWait(long waitTime) {
      stopRequested = true;
      // Purge the sink of any scheduled gc's, this needs to be equivalent to stopping the garbage collector thread.
      gcSink.clear();
      long start = System.nanoTime();
      while (gcRunning) {
        ThreadUtil.reallySleep(1000);
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) > waitTime) { return false; }
      }
      return true;
    }
  }
}
