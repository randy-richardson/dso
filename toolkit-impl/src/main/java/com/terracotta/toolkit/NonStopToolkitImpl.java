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

import org.terracotta.toolkit.ToolkitFeature;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.ToolkitFeatureTypeInternal;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedMap;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.feature.NonStopInternalFeature;
import org.terracotta.toolkit.monitoring.OperatorEventLevel;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.abortable.AbortableOperationManager;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.nonstop.AbstractToolkitObjectLookupAsync;
import com.terracotta.toolkit.nonstop.NonStopClusterListener;
import com.terracotta.toolkit.nonstop.NonStopConfigRegistryImpl;
import com.terracotta.toolkit.nonstop.NonStopConfigurationLookup;
import com.terracotta.toolkit.nonstop.NonStopContextImpl;
import com.terracotta.toolkit.nonstop.NonStopLockImpl;
import com.terracotta.toolkit.nonstop.NonStopManagerImpl;
import com.terracotta.toolkit.nonstop.NonstopTimeoutBehaviorResolver;
import com.terracotta.toolkit.nonstop.ToolkitLockLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.util.concurrent.FutureTask;

public class NonStopToolkitImpl implements ToolkitInternal {
  protected final NonStopManagerImpl                 nonStopManager;
  protected final NonStopConfigRegistryImpl          nonStopConfigManager          = new NonStopConfigRegistryImpl();
  private final NonstopTimeoutBehaviorResolver       nonstopTimeoutBehaviorFactory = new NonstopTimeoutBehaviorResolver();

  private final AbortableOperationManager            abortableOperationManager;
  protected final NonStopClusterListener             nonStopClusterListener;
  private final NonStopFeature                       nonStopFeature;
  private final NonStopInternalFeature               nonStopInternalFeature;
  private final ToolkitInitializer                   toolkitInitializer;
  private final NonStopContextImpl                   context;
  private final NonStopClusterInfo                   nonStopClusterInfo;
  private final NonStopInitializationService         nonStopInitiailzationService;
  private final NonStopManagementInternalFeatureImpl managementInternalFeature;
  private final String                               uuid;

  public NonStopToolkitImpl(FutureTask<ToolkitInternal> toolkitDelegateFutureTask,
                            AbortableOperationManager abortableOperationManager, String uuid) {
    this.abortableOperationManager = abortableOperationManager;
    this.uuid = uuid;
    this.nonStopManager = new NonStopManagerImpl(abortableOperationManager);
    this.nonStopConfigManager.registerForType(NonStopConfigRegistryImpl.DEFAULT_CONFIG,
                                              NonStopConfigRegistryImpl.SUPPORTED_TOOLKIT_TYPES
                                                  .toArray(new ToolkitObjectType[0]));
    this.nonStopFeature = new NonStopFeatureImpl(this, abortableOperationManager);
    this.toolkitInitializer = new AsyncToolkitInitializer(toolkitDelegateFutureTask, abortableOperationManager);
    this.nonStopClusterInfo = new NonStopClusterInfo(toolkitInitializer);
    this.nonStopClusterListener = new NonStopClusterListener(abortableOperationManager, nonStopClusterInfo);
    this.context = new NonStopContextImpl(nonStopManager, nonStopConfigManager, abortableOperationManager,
                                          nonstopTimeoutBehaviorFactory, toolkitInitializer,
                                          nonStopClusterListener);
    this.nonStopInternalFeature = new NonStopInternalFeatureImpl(context);

    this.nonStopInitiailzationService = new NonStopInitializationService(context);
    this.managementInternalFeature = new NonStopManagementInternalFeatureImpl();
  }

  public NonStopToolkitImpl(ToolkitInternal toolkit, AbortableOperationManager abortableOperationManager, String uuid) {
    this.abortableOperationManager = abortableOperationManager;
    this.uuid = uuid;
    this.nonStopManager = new NonStopManagerImpl(abortableOperationManager);
    this.nonStopConfigManager.registerForType(NonStopConfigRegistryImpl.DEFAULT_CONFIG,
                                              NonStopConfigRegistryImpl.SUPPORTED_TOOLKIT_TYPES
                                                  .toArray(new ToolkitObjectType[0]));
    this.nonStopFeature = new NonStopFeatureImpl(this, abortableOperationManager);
    this.toolkitInitializer = new SyncToolkitInitializer(toolkit);
    this.nonStopClusterInfo = new NonStopClusterInfo(toolkitInitializer);
    this.nonStopClusterListener = new NonStopClusterListener(abortableOperationManager, nonStopClusterInfo);
    this.context = new NonStopContextImpl(nonStopManager, nonStopConfigManager, abortableOperationManager,
                                          nonstopTimeoutBehaviorFactory, toolkitInitializer, nonStopClusterListener);
    this.nonStopInternalFeature = new NonStopInternalFeatureImpl(context);

    this.nonStopInitiailzationService = new NonStopInitializationService(context);
    this.managementInternalFeature = new NonStopManagementInternalFeatureImpl();
  }

  public void setPlatformService(Object platformService) {
    this.managementInternalFeature.setPlatformService((PlatformService) platformService);
  }

  private ToolkitInternal getInitializedToolkit() {
    return toolkitInitializer.getToolkit();
  }

  @Override
  public <E> ToolkitList<E> getList(final String name, final Class<E> klazz) {
    final AbstractToolkitObjectLookupAsync<ToolkitList<E>> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitList<E>>(
        name, abortableOperationManager) {
      @Override
      public ToolkitList<E> lookupObject() {
        return getInitializedToolkit().getList(name, klazz);
      }
    };
    return getNonStopProxy(name, toolkitObjectLookup, ToolkitObjectType.LIST, ToolkitListInternal.class);
  }

  @Override
  public <K, V> ToolkitMap<K, V> getMap(final String name, final Class<K> keyKlazz, final Class<V> valueKlazz) {
    final AbstractToolkitObjectLookupAsync<ToolkitMap> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitMap>(
        name, abortableOperationManager) {
      @Override
      public ToolkitMap<K, V> lookupObject() {
        return getInitializedToolkit().getMap(name, keyKlazz, valueKlazz);
      }
    };
    return getNonStopProxy(name, toolkitObjectLookup, ToolkitObjectType.MAP, ToolkitMap.class);
  }

  @Override
  public <K extends Comparable<? super K>, V> ToolkitSortedMap<K, V> getSortedMap(final String name,
                                                                                  final Class<K> keyKlazz,
                                                                                  final Class<V> valueKlazz) {
    final AbstractToolkitObjectLookupAsync<ToolkitSortedMap> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitSortedMap>(
        name, abortableOperationManager) {
      @Override
      public ToolkitSortedMap<K, V> lookupObject() {
        return getInitializedToolkit().getSortedMap(name, keyKlazz, valueKlazz);
      }
    };
    return getNonStopProxy(name, toolkitObjectLookup, ToolkitObjectType.SORTED_MAP, ToolkitSortedMap.class);
  }

  @Override
  public <E> ToolkitBlockingQueue<E> getBlockingQueue(String name, int capacity, Class<E> klazz) {
    return getInitializedToolkit().getBlockingQueue(name, capacity, klazz);
  }

  @Override
  public <E> ToolkitBlockingQueue<E> getBlockingQueue(String name, Class<E> klazz) {
    return getInitializedToolkit().getBlockingQueue(name, klazz);
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return nonStopClusterInfo;
  }

  @Override
  public ToolkitLock getLock(String name) {
    return getLock(name, ToolkitLockTypeInternal.WRITE);
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock(final String name) {
    final AbstractToolkitObjectLookupAsync<ToolkitReadWriteLock> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitReadWriteLock>(
        name, abortableOperationManager) {
      @Override
      public ToolkitReadWriteLock lookupObject() {
        return getInitializedToolkit().getReadWriteLock(name);
      }
    };
    return getNonStopProxy(name, toolkitObjectLookup, ToolkitObjectType.READ_WRITE_LOCK, ToolkitReadWriteLock.class);
  }

  @Override
  public <E> ToolkitNotifier<E> getNotifier(final String name, final Class<E> klazz) {
    final AbstractToolkitObjectLookupAsync<ToolkitNotifier<E>> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitNotifier<E>>(
        name, abortableOperationManager) {
      @Override
      public ToolkitNotifier<E> lookupObject() {
        return getInitializedToolkit().getNotifier(name, klazz);
      }
    };
    return getNonStopProxy(name, toolkitObjectLookup, ToolkitObjectType.NOTIFIER, ToolkitNotifier.class);
  }

  @Override
  public ToolkitAtomicLong getAtomicLong(final String name) {
    final AbstractToolkitObjectLookupAsync<ToolkitAtomicLong> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitAtomicLong>(
        name, abortableOperationManager) {
      @Override
      public ToolkitAtomicLong lookupObject() {
        return getInitializedToolkit().getAtomicLong(name);
      }
    };
    return getNonStopProxy(name, toolkitObjectLookup, ToolkitObjectType.ATOMIC_LONG, ToolkitAtomicLong.class);
  }

  @Override
  public ToolkitBarrier getBarrier(String name, int parties) {
    return getInitializedToolkit().getBarrier(name, parties);
  }

  @Override
  public void fireOperatorEvent(OperatorEventLevel level, String applicationName, String eventMessage) {
    getInitializedToolkit().fireOperatorEvent(level, applicationName, eventMessage);
  }

  @Override
  public <E extends Comparable<? super E>> ToolkitSortedSet<E> getSortedSet(final String name, final Class<E> klazz) {

    final AbstractToolkitObjectLookupAsync<ToolkitSortedSet<E>> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitSortedSet<E>>(
        name, abortableOperationManager) {
      @Override
      public ToolkitSortedSet<E> lookupObject() {
        return getInitializedToolkit().getSortedSet(name, klazz);
      }
    };
    return getNonStopProxy(name, toolkitObjectLookup, ToolkitObjectType.SORTED_SET, ToolkitSortedSet.class);
  }

  @Override
  public <E> ToolkitSet<E> getSet(final String name, final Class<E> klazz) {
    final AbstractToolkitObjectLookupAsync<ToolkitSet<E>> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitSet<E>>(
        name, abortableOperationManager) {
      @Override
      public ToolkitSet<E> lookupObject() {
        return getInitializedToolkit().getSet(name, klazz);
      }
    };
    return getNonStopProxy(name, toolkitObjectLookup, ToolkitObjectType.SET, ToolkitSet.class);
  }

  @Override
  public <V> ToolkitCache<String, V> getCache(final String name, final Configuration configuration, final Class<V> klazz) {
    NonStopConfigurationLookup nonStopConfigurationLookup = getNonStopConfigurationLookup(name, ToolkitObjectType.CACHE);

    final AbstractToolkitObjectLookupAsync<ToolkitCache> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitCache>(
        name, abortableOperationManager) {
      @Override
      public ToolkitCache<String, V> lookupObject() {
        return getInitializedToolkit().getCache(name, configuration, klazz);
      }
    };

    nonStopInitiailzationService.initialize(toolkitObjectLookup, nonStopConfigurationLookup.getNonStopConfiguration());

    return ToolkitInstanceProxy.newNonStopProxy(nonStopConfigurationLookup, context, toolkitObjectLookup,
                                                ToolkitCacheImpl.class.getInterfaces());
  }

  @Override
  public <V> ToolkitCache<String, V> getCache(String name, Class<V> klazz) {
    return getCache(name, null, klazz);
  }

  @Override
  public <V> ToolkitStore<String, V> getStore(final String name, final Configuration configuration, final Class<V> klazz) {
    NonStopConfigurationLookup nonStopConfigurationLookup = getNonStopConfigurationLookup(name, ToolkitObjectType.STORE);

    final AbstractToolkitObjectLookupAsync<ToolkitStore> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitStore>(
        name, abortableOperationManager) {
      @Override
      public ToolkitStore<String, V> lookupObject() {
        return getInitializedToolkit().getStore(name, configuration, klazz);
      }
    };

    nonStopInitiailzationService.initialize(toolkitObjectLookup, nonStopConfigurationLookup.getNonStopConfiguration());

    return ToolkitInstanceProxy.newNonStopProxy(nonStopConfigurationLookup, context, toolkitObjectLookup,
                                                ToolkitStore.class);
  }

  @Override
  public <V> ToolkitStore<String, V> getStore(String name, Class<V> klazz) {
    return getStore(name, null, klazz);
  }

  @Override
  public void shutdown() {
    nonStopManager.shutdown();
    ToolkitInternal toolkit = null;
    try {
      toolkit = getInitializedToolkit();
    } catch (Exception e) {
      // Ignore if toolkit failed to initialize
    }
    if (toolkit != null) {
      toolkit.shutdown();
    }
    nonStopInitiailzationService.shutdown();
  }

  public NonStopConfigurationRegistry getNonStopConfigurationToolkitRegistry() {
    return nonStopConfigManager;
  }

  public void start(NonStopConfiguration configuration) {
    nonStopConfigManager.registerForThread(configuration);

    if (configuration.isEnabled()) {
      nonStopManager.begin(configuration.getTimeoutMillis());
    }
  }

  public void stop() {
    NonStopConfiguration configuration = nonStopConfigManager.deregisterForThread();

    if (configuration != null && configuration.isEnabled()) {
      nonStopManager.finish();
    }
  }

  @Override
  public ToolkitLock getLock(final String name, final ToolkitLockTypeInternal lockType) {
    NonStopConfigurationLookup nonStopConfigurationLookup = getNonStopConfigurationLookup(name, ToolkitObjectType.LOCK);

    ToolkitLockLookup toolkitObjectLookup = new ToolkitLockLookup(toolkitInitializer, name, lockType);
    return new NonStopLockImpl(context, nonStopConfigurationLookup, toolkitObjectLookup);
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    getInitializedToolkit().registerBeforeShutdownHook(hook);
  }

  @Override
  public void waitUntilAllTransactionsComplete() {
    try {
      getInitializedToolkit().waitUntilAllTransactionsComplete();
    } catch (ToolkitAbortableOperationException e) {
      throw new NonStopException(e);
    }
  }

  @Override
  public ToolkitLogger getLogger(String name) {
    return getInitializedToolkit().getLogger(name);
  }

  @Override
  public String getClientUUID() {
    return uuid;
  }

  @Override
  public ToolkitProperties getProperties() {
    return getInitializedToolkit().getProperties();
  }

  @Override
  public <T extends ToolkitFeature> T getFeature(ToolkitFeatureType<T> type) {
    if (type == ToolkitFeatureType.NONSTOP) { return (T) nonStopFeature; }
    return getInitializedToolkit().getFeature(type);
  }

  @Override
  public <T extends ToolkitFeature> T getFeature(ToolkitFeatureTypeInternal<T> type) {
    if (type == ToolkitFeatureTypeInternal.NONSTOP) { return (T) nonStopInternalFeature; }
    if (type == ToolkitFeatureTypeInternal.MANAGEMENT) { return (T) managementInternalFeature; }
    return getInitializedToolkit().getFeature(type);
  }

  private NonStopConfigurationLookup getNonStopConfigurationLookup(final String name, final ToolkitObjectType objectType) {
    return new NonStopConfigurationLookup(context, objectType, name);
  }

  private <T extends ToolkitObject> T getNonStopProxy(final String name,
                                                      final AbstractToolkitObjectLookupAsync<T> toolkitObjectLookup,
                                                      final ToolkitObjectType objectType, Class clazz) {
    NonStopConfigurationLookup nonStopConfigurationLookup = getNonStopConfigurationLookup(name, objectType);

    nonStopInitiailzationService.initialize(toolkitObjectLookup, nonStopConfigurationLookup.getNonStopConfiguration());

    return ToolkitInstanceProxy.newNonStopProxy(nonStopConfigurationLookup, context, toolkitObjectLookup, clazz);
  }
}
