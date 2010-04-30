/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.stats.TerracottaOperatorEventStats;

import java.util.EventListener;

public interface TerracottaOperatorEventsListener extends EventListener {

  void statusUpdate(TerracottaOperatorEventStats tcOperatorEventStats);

}
