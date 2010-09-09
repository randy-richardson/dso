/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;


public interface UpdateCheckConfig extends NewConfig {
  boolean isEnabled();

  int periodDays();
}
