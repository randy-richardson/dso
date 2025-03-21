/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.config.schema;

import org.apache.commons.lang3.ClassUtils;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ConfigItemListener;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.lang.reflect.Array;

/**
 * A base class for all new config objects.
 */
public class BaseConfigObject implements Config {

  private static final TCLogger logger = TCLogging.getLogger(BaseConfigObject.class);

  protected final ConfigContext context;

  public BaseConfigObject(ConfigContext context) {
    Assert.assertNotNull(context);
    this.context = context;
  }

  private static class IgnoringConfigItemListener implements ConfigItemListener {
    private final ConfigItem item;

    public IgnoringConfigItemListener(ConfigItem item) {
      Assert.assertNotNull(item);
      this.item = item;
    }

    @Override
    public void valueChanged(Object oldValue, Object newValue) {
      logger.warn("The attempt to change the value of " + item + " from " + oldValue + " to " + newValue
                  + " was ignored; runtime changes in this configuration value are not yet supported.");
    }
  }

  @Override
  public void changesInItemIgnored(ConfigItem item) {
    Assert.assertNotNull(item);
    item.addListener(new IgnoringConfigItemListener(item));
  }

  private class ForbiddenConfigItemListener implements ConfigItemListener {
    private final ConfigItem item;

    public ForbiddenConfigItemListener(ConfigItem item) {
      Assert.assertNotNull(item);
      this.item = item;
    }

    private boolean isEqual(Object one, Object two) {
      if (one != null && two != null && one.getClass().isArray() && two.getClass().isArray()
          && one.getClass().getComponentType().equals(two.getClass().getComponentType())) {
        if (Array.getLength(one) != Array.getLength(two)) return false;

        for (int i = 0; i < Array.getLength(one); ++i) {
          if (!isEqual(Array.get(one, i), Array.get(two, i))) return false;
        }

        return true;
      } else if (one != null && two != null) {
        return one.equals(two);
      } else return one == two;
    }

    @Override
    public void valueChanged(Object oldValue, Object newValue) {
      if (oldValue == null) return;
      if (newValue != null && isEqual(oldValue, newValue)) return;

      context.illegalConfigurationChangeHandler().changeFailed(item, oldValue, newValue);
    }
  }

  @Override
  public void changesInItemForbidden(ConfigItem item) {
    Assert.assertNotNull(item);
    item.addListener(new ForbiddenConfigItemListener(item));
  }

  @Override
  public String toString() {
    return ClassUtils.getShortClassName(getClass()) + " around bean:\n" + context.bean();
  }

  @Override
  public XmlObject getBean() {
    return this.context.bean();
  }

  @Override
  public Object syncLockForBean() {
    return this.context.syncLockForBean();
  }

}
