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
package com.tc.config.schema.listen;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A set of {@link ConfigurationChangeListener}s.
 */
public class ConfigurationChangeListenerSet implements ConfigurationChangeListener {

  // This must be declared as a HashSet, not just a Set, so that we can clone it (below).
  private final HashSet changeListeners;

  public ConfigurationChangeListenerSet() {
    this.changeListeners = new HashSet();
  }

  public synchronized void addListener(ConfigurationChangeListener listener) {
    Assert.assertNotNull(listener);
    this.changeListeners.add(listener);
  }

  public synchronized void removeListener(ConfigurationChangeListener listener) {
    Assert.assertNotNull(listener);
    this.changeListeners.remove(listener);
  }

  @Override
  public void configurationChanged(XmlObject oldConfig, XmlObject newConfig) {
    Set dup;

    synchronized (this) {
      dup = (Set) this.changeListeners.clone();
    }

    Iterator iter = dup.iterator();
    while (iter.hasNext()) {
      ((ConfigurationChangeListener) iter.next()).configurationChanged(oldConfig, newConfig);
    }
  }

}
