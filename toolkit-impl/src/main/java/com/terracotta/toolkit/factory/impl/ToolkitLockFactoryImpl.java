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
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.util.collections.WeakValueMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ToolkitLockFactoryImpl implements ToolkitObjectFactory<ToolkitLockImpl> {
  public static final String                  INTERNAL_LOCK_TYPE = "INTERNAL_LOCK_TYPE";
  private final WeakValueMap<ToolkitLockImpl> localCache;
  private final PlatformService               platformService;
  private final Lock                          lock;
  private static final String                 DELIMITER          = "|";

  public ToolkitLockFactoryImpl(ToolkitFactoryInitializationContext context) {
    this.localCache = context.getWeakValueMapManager().createWeakValueMap();
    this.platformService = context.getPlatformService();
    this.lock = new ReentrantLock();
  }

  @Override
  public ToolkitLockImpl getOrCreate(String name, Configuration config) {
    ToolkitLockImpl cachedLock = null;
    lock.lock();
    try {
      ToolkitLockTypeInternal requiredLockType = ToolkitLockTypeInternal.valueOf(config.getString(INTERNAL_LOCK_TYPE));
      cachedLock = localCache.get(getQualifiedlockName(name, requiredLockType));
      if (cachedLock == null) {
        cachedLock = createLock(name, requiredLockType);
      }
    } finally {
      lock.unlock();
    }
    return cachedLock;
  }

  private String getQualifiedlockName(String name, ToolkitLockTypeInternal lockType) {
    return name + DELIMITER + lockType.name();
  }

  private ToolkitLockImpl createLock(String name, ToolkitLockTypeInternal internalLockType) {
    ToolkitLockImpl toolkitLock;
    toolkitLock = new ToolkitLockImpl(platformService, name, internalLockType);
    localCache.put(getQualifiedlockName(name, internalLockType), toolkitLock);
    return toolkitLock;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.LOCK;
  }

}
