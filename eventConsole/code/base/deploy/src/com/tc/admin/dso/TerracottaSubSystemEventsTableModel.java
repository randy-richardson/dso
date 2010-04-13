/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XObjectTableModel;
import com.tc.stats.TerracottaSubSystemEventStats;
import com.tc.tcevent.stats.TerracottaSubSystemEventsStatsImpl;

public class TerracottaSubSystemEventsTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = { "EventTime", "NodeID", "TCEventType", "System", "Message" };

  private final String[]        HEADERS = { "cluster.events.timeOfEvent", "cluster.events.node",
      "cluster.events.eventType", "cluster.events.system", "cluster.events.message" };

  public TerracottaSubSystemEventsTableModel(ApplicationContext appContext) {
    super();
    configure(TerracottaSubSystemEventsStatsImpl.class, FIELDS, appContext.getMessages(HEADERS));
  }

  public void addEventsStats(TerracottaSubSystemEventStats eventsStats) {
    add(eventsStats);
    int rowCount = getRowCount();
    fireTableRowsInserted(rowCount, rowCount);
  }

}
