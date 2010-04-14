/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

public class TerracottaOperatorEventNode extends ComponentNode {
  private final ApplicationContext  appContext;
  protected IClusterModel           clusterModel;
  protected TerracottaOperatorEventsPanel clusterEventsStatsPanel;

  public TerracottaOperatorEventNode(ApplicationContext appContext, IClusterModel clusterModel) {
    super();
    this.appContext = appContext;
    this.clusterModel = clusterModel;
    setLabel(appContext.getMessage("cluster.events"));
    setIcon(DSOHelper.getHelper().getGCIcon());
  }

  IClusterModel getClusterModel() {
    return clusterModel;
  }

  protected TerracottaOperatorEventsPanel createOperatorEventsStatsPanel() {
    return new TerracottaOperatorEventsPanel(appContext, clusterModel);
  }

  @Override
  public Component getComponent() {
    if (clusterEventsStatsPanel == null) {
      clusterEventsStatsPanel = createOperatorEventsStatsPanel();
    }
    return clusterEventsStatsPanel;
  }

  @Override
  public void tearDown() {
    if (clusterEventsStatsPanel != null) {
      clusterEventsStatsPanel.tearDown();
      clusterEventsStatsPanel = null;
    }
    super.tearDown();
  }
}
