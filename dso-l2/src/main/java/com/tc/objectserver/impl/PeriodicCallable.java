/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictionListener;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.ObjectIDSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author mscott
 */
public class PeriodicCallable implements Callable<SampledRateCounter>, CanCancel, EvictionListener {
    
    private final Set<ObjectID> workingSet;
    private final Set<ObjectID> listeningSet;
    private final ProgressiveEvictionManager evictor;
    private final AtomicBoolean rescheduled = new AtomicBoolean(false);

    private boolean stopped = false;
    private PeriodicEvictionTrigger current;
    
    public PeriodicCallable(ProgressiveEvictionManager evictor, Set<ObjectID> workingSet) {
      this.evictor = evictor;
      this.workingSet = workingSet;
      this.listeningSet = new ObjectIDSet(workingSet);
    }

    @Override
    public boolean cancel() {
      stop();
      evictor.removeEvictionListener(this);
      return true;
    }
    
    private synchronized void stop() {
      stopped = true;
      listeningSet.clear();
      workingSet.clear();
      if ( current != null ) {
        current.stop();
      }
    }

    private synchronized boolean isStopped() {
      return stopped;
    }
    
    private synchronized void setCurrent(PeriodicEvictionTrigger trigger) {
      current = trigger;
    }

    @Override
    public SampledRateCounter call() throws Exception {
      SampledRateCounter counter = new AggregateSampleRateCounter();
      ObjectIDSet rollover = new ObjectIDSet();
      try {
        evictor.addEvictionListener(this);
        for (final ObjectID mapID : workingSet) {
          PeriodicEvictionTrigger trigger = evictor.schedulePeriodicEviction(mapID);
          if ( trigger != null ) {
            setCurrent(trigger);
            counter.increment(trigger.getCount(),trigger.getRuntimeInMillis());
            if ( trigger.filterRatio() > .66f ) {
              rollover.add(mapID);
            }
          } else {
            listeningSet.remove(mapID);
          }
          if ( isStopped() ) {
            return counter;
          }
        }
      } finally {
        boolean reschedule = false;
        synchronized (this) {
          workingSet.clear();
          workingSet.addAll(rollover);
          current = null;
          if ( !stopped && listeningSet.isEmpty() && !rollover.isEmpty() ) {
            reschedule = true;
          }
        }
        if (reschedule && rescheduled.compareAndSet(false, true)) {
          evictor.schedulePeriodicEvictionRun(rollover);
        }
      }

      return counter;
    }

  @Override
  public boolean evictionStarted(ObjectID oid) {
    return false;
  }

  @Override
  public boolean evictionCompleted(ObjectID oid) {
    Set<ObjectID> newWorkingSet = null;
    boolean complete, reschedule = false;
    synchronized (this) {
      listeningSet.remove(oid);
      if ( listeningSet.isEmpty() ) {
        if ( !stopped && current == null && !workingSet.isEmpty() ) {
          newWorkingSet = new ObjectIDSet(workingSet);
          reschedule = true;
        }
        complete = true;
      } else {
        complete = false;
      }
    }
    if (reschedule && rescheduled.compareAndSet(false, true)) {
      evictor.schedulePeriodicEvictionRun(newWorkingSet);
    }
    return complete;
  }
}
