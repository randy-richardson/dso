/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.util.Assert;
import com.terracottatech.config.Client;

import java.io.File;

/**
 * The standard implementation of {@link CommonL1Config}.
 */
public class CommonL1ConfigObject extends BaseConfigObject implements CommonL1Config {

  public CommonL1ConfigObject(ConfigContext context) {
    super(context);
    Assert.assertNotNull(context);

    this.context.ensureRepositoryProvides(Client.class);
  }

  public File logsPath() {
    final Client client = (Client) this.context.bean();
    return new File(client.getLogs());
  }

}
