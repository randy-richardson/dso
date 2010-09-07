/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.dynamic.ConfigItem;

public interface OffHeapConfigItem extends ConfigItem {

  boolean isEnabled();

  String getMaxDataSize();

}
