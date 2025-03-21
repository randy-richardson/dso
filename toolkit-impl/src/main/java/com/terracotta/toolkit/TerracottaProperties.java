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
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.ToolkitProperties;

import com.tc.platform.PlatformService;
import com.tc.properties.TCProperties;

public class TerracottaProperties implements ToolkitProperties {
  private final TCProperties delegate;

  public TerracottaProperties(PlatformService platformService) {
    this.delegate = platformService.getTCProperties();
  }

  @Override
  public Boolean getBoolean(String key, Boolean defaultValue) {
    String str = getProperty(key);
    if (str == null) {
      return defaultValue;
    } else {
      return Boolean.valueOf(str);
    }
  }

  @Override
  public Boolean getBoolean(String key) {
    return getBoolean(key, Boolean.FALSE);
  }

  @Override
  public Integer getInteger(String key, Integer defaultValue) {
    try {
      return Integer.valueOf(getProperty(key));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @Override
  public Integer getInteger(String key) {
    return getInteger(key, null);
  }

  @Override
  public Long getLong(String key, Long defaultValue) {
    try {
      return Long.valueOf(getProperty(key));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @Override
  public Long getLong(String key) {
    return getLong(key, null);
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    String value = getProperty(key);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  @Override
  public String getProperty(String key) {
    return delegate.getProperty(key, true);
  }

  @Override
  public void setProperty(String key, String value) {
    delegate.setProperty(key, value);
  }
}
