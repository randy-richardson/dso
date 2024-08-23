/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.config.schema.listen;

import org.apache.xmlbeans.XmlObject;

/**
 * A mock {@link ConfigurationChangeListener}, for use in tests.
 */
public class MockConfigurationChangeListener implements ConfigurationChangeListener {

  private int       numConfigurationChangeds;
  private XmlObject lastOldConfig;
  private XmlObject lastNewConfig;

  public MockConfigurationChangeListener() {
    reset();
  }

  public void reset() {
    this.numConfigurationChangeds = 0;
    this.lastOldConfig = null;
    this.lastNewConfig = null;
  }

  @Override
  public void configurationChanged(XmlObject oldConfig, XmlObject newConfig) {
    ++this.numConfigurationChangeds;
    this.lastOldConfig = oldConfig;
    this.lastNewConfig = newConfig;
  }

  public XmlObject getLastNewConfig() {
    return lastNewConfig;
  }

  public XmlObject getLastOldConfig() {
    return lastOldConfig;
  }

  public int getNumConfigurationChangeds() {
    return numConfigurationChangeds;
  }

}
