/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.runtime;

import com.tc.exception.TCRuntimeException;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.runtime.Os;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TCMemoryManagerImpl implements TCMemoryManager {

  private static final TCLogger logger        = TCLogging.getLogger(TCMemoryManagerImpl.class);
  private static final String          CMS_NAME      = "ConcurrentMarkSweep";
  private static final String          CMS_WARN_MESG = "Terracotta does not recommend ConcurrentMarkSweep Collector.";

  private final List            listeners     = new CopyOnWriteArrayList();

  private final int leastCount;
  private final long sleepInterval;

  private MemoryMonitor         monitor;

  private final TCThreadGroup   threadGroup;

  public TCMemoryManagerImpl(long sleepInterval, int leastCount, TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.sleepInterval = sleepInterval;
    this.leastCount = leastCount;
  }

  public TCMemoryManagerImpl(TCThreadGroup threadGroup) {
    this(3000, 2, threadGroup);
  }

  // CDV-1181 warn if using CMS
  @Override
  public void checkGarbageCollectors() {
    List<GarbageCollectorMXBean> gcmbeans = ManagementFactory.getGarbageCollectorMXBeans();
    boolean foundCMS = false;
    for (GarbageCollectorMXBean mbean : gcmbeans) {
      String gcname = mbean.getName();
      logger.info("GarbageCollector: " + gcname);
      if (CMS_NAME.equals(gcname)) {
        foundCMS = true;
      }
    }
    if (foundCMS) {
      logger.warn(CMS_WARN_MESG);
    }
  }

  @Override
  public void registerForMemoryEvents(MemoryEventsListener listener) {
    listeners.add(listener);
    startMonitorIfNecessary();
  }

  @Override
  public void unregisterForMemoryEvents(MemoryEventsListener listener) {
    listeners.remove(listener);
    stopMonitorIfNecessary();
  }

  private synchronized void stopMonitorIfNecessary() {
    if (listeners.size() == 0) {
      stopMonitorThread();
    }
  }

  /**
   * XXX: Should we wait for the monitor thread to stop completely.
   */
  private void stopMonitorThread() {
    if (monitor != null) {
      monitor.stop();
      monitor = null;
    }
  }

  private synchronized void startMonitorIfNecessary() {
    if (listeners.size() > 0 && monitor == null) {
      this.monitor = new MemoryMonitor(TCRuntime.getJVMMemoryManager(), sleepInterval);
      Thread t = new Thread(this.threadGroup, this.monitor);
      t.setDaemon(true);
      if (Os.isSolaris()) {
        t.setPriority(Thread.MAX_PRIORITY);
        t.setName("TC Memory Monitor(High Priority)");
      } else {
        t.setName("TC Memory Monitor");
      }
      t.start();
    }
  }

  private void fireMemoryEvent(MemoryUsage mu) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      MemoryEventsListener listener = (MemoryEventsListener) i.next();
      listener.memoryUsed(mu);
    }
  }

  public class MemoryMonitor implements Runnable {

    private final JVMMemoryManager manager;
    private volatile boolean       run = true;
    private int                    lastUsed;
    private long                   sleepTime;

    public MemoryMonitor(JVMMemoryManager manager, long sleepInterval) {
      this.manager = manager;
      this.sleepTime = sleepInterval;
    }

    public void stop() {
      run = false;
    }

    @Override
    public void run() {
      logger.debug("Starting Memory Monitor - sleep interval - " + sleepTime);

      // Variable to help keep us from being "spammy" in the logs about problems getting mem pool info
      long lastWarningTS = 0;

      while (run) {
        try {
          Thread.sleep(sleepTime);
          MemoryUsage mu = manager.isMemoryPoolMonitoringSupported() ? manager.getOldGenUsage() : manager.getMemoryUsage();
          fireMemoryEvent(mu);
          adjust(mu);
        } catch (Throwable t) {
          long ts = System.currentTimeMillis();
          // has it been two hours since we nagged in the logs?
          if(ts - lastWarningTS > (2L * 60L * 60L * 1000L)) {
            logger.warn(t);
            logger.warn("Memory Monitor unable to reliably watch memory usage and GC events due to JVM Internal errors.");
            lastWarningTS = ts;
          }
        }
      }
      logger.debug("Stopping Memory Monitor - sleep interval - " + sleepTime);
    }

    private void adjust(MemoryUsage mu) {
      int usedPercentage = mu.getUsedPercentage();
      try {
        if (lastUsed != 0 && lastUsed < usedPercentage) {
          int diff = usedPercentage - lastUsed;
          long l_sleep = this.sleepTime;
          if (diff > leastCount * 1.5 && l_sleep > 1) {
            // decrease sleep time
            this.sleepTime = Math.max(1, l_sleep * leastCount / diff);
            logger.info("Sleep time changed to : " + this.sleepTime);
          } else if (diff < leastCount * 0.5 && l_sleep < sleepInterval) {
            // increase sleep time
            this.sleepTime = Math.min(sleepInterval, l_sleep * leastCount / diff);
            logger.info("Sleep time changed to : " + this.sleepTime);
          }
        }
      } finally {
        lastUsed = usedPercentage;
      }
    }
  }

  @Override
  public synchronized void shutdown() {
    stopMonitorThread();
  }

}
