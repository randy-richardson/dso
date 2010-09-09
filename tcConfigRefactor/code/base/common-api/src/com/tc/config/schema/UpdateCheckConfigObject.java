/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.UpdateCheck;

public class UpdateCheckConfigObject extends BaseNewConfigObject implements UpdateCheckConfig {
  private final boolean isEnabled;
  private final int     periodDays;

  public UpdateCheckConfigObject(ConfigContext context) {
    super(context);

    context.ensureRepositoryProvides(UpdateCheck.class);
    UpdateCheck updateCheck = (UpdateCheck) this.context.bean();
    this.isEnabled = updateCheck.getEnabled();
    this.periodDays = updateCheck.getPeriodDays();
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public int periodDays() {
    return periodDays;
  }
}
