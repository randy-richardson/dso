/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XObjectTableModel;
import com.tc.operatorevent.stats.TerracottaOperatorEventsStatsImpl;
import com.tc.stats.TerracottaOperatorEventStats;

public class TerracottaOperatorEventsTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = { "EventTime", "NodeId", "EventType", "EventSubSystem", "EventMessage" };

  private final String[]        HEADERS = { "cluster.events.timeOfEvent", "cluster.events.node",
      "cluster.events.eventType", "cluster.events.system", "cluster.events.message" };

  public TerracottaOperatorEventsTableModel(ApplicationContext appContext) {
    super();
    configure(TerracottaOperatorEventsStatsImpl.class, FIELDS, appContext.getMessages(HEADERS));
  }

  public void addEventsStats(TerracottaOperatorEventStats eventsStats) {
    add(eventsStats);
    int rowCount = getRowCount();
    fireTableRowsInserted(rowCount, rowCount);
  }

}
