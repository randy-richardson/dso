/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.UpdateCheck;

public class UpdateCheckConfigObject extends BaseNewConfigObject implements UpdateCheckConfig {

  public UpdateCheckConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(UpdateCheck.class);
  }

  public UpdateCheck getUpdateCheck() {
    return (UpdateCheck) this.context.bean();
  }

}
