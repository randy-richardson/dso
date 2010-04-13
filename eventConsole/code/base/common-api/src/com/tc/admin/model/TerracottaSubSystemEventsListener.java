/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.stats.TerracottaSubSystemEventStats;

import java.util.EventListener;

public interface TerracottaSubSystemEventsListener extends EventListener {
  void statusUpdate(TerracottaSubSystemEventStats eventsStats);
}
