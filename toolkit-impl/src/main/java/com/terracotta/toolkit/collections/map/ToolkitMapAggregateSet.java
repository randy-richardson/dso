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
package com.terracotta.toolkit.collections.map;

import com.terracotta.toolkit.util.collections.AggregateMapIterator;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class ToolkitMapAggregateSet<E> extends AbstractSet<E> {

  protected final AggregateServerMap map;
  // contains the keys for which txn was in progress for add when the set was created.
  protected final Set<E>             txnInProgressForAdd = new HashSet<E>();
  // contains the keys for which txn was in progress for add or remove when the set was created.
  protected final Set<E>             filterSet           = new HashSet<E>();

  public ToolkitMapAggregateSet(AggregateServerMap map) {
    this.map = map;
    map.getAnyServerMap().addTxnInProgressKeys(txnInProgressForAdd, filterSet);
    filterSet.addAll(txnInProgressForAdd);
  }

  public static class ClusteredMapAggregateKeySet<K, V> extends ToolkitMapAggregateSet<K> {

    public ClusteredMapAggregateKeySet(AggregateServerMap<K, V> map) {
      super(map);
    }

    @Override
    public boolean contains(final Object o) {
      return map.containsKey(o);
    }

    @Override
    public boolean remove(final Object o) {
      return map.remove(o) != null;
    }

    @Override
    public Iterator<K> iterator() {
      return new AggregateMapIterator<K>(map.iterator(), txnInProgressForAdd.iterator()) {

        @Override
        public Iterator<K> getClusterMapIterator(InternalToolkitMap aMap) {
          return aMap.keySet(filterSet).iterator();
        }
      };
    }

    @Override
    public int size() {
      return map.size();
    }

  }

  public static class ClusteredMapAggregateEntrySet<K, V> extends ToolkitMapAggregateSet<Map.Entry<K, V>> {

    public ClusteredMapAggregateEntrySet(AggregateServerMap<K, V> map) {
      super(map);
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {

      Map entryMap = new HashMap(map.getAll(txnInProgressForAdd));
      // remove the entries that might have been removed later after creation of the entryset.
      for (Iterator iterator = entryMap.entrySet().iterator(); iterator.hasNext();) {
        Entry<K, V> entry = (Entry<K, V>) iterator.next();
        if (entry.getValue() == null) {
          iterator.remove();
        }
      }
      return new AggregateMapIterator<Map.Entry<K, V>>(map.iterator(), entryMap.entrySet()
          .iterator()) {

        @Override
        public Iterator<Map.Entry<K, V>> getClusterMapIterator(InternalToolkitMap aMap) {
          return aMap.entrySet(filterSet).iterator();
        }
      };
    }

    @Override
    public boolean contains(final Object o) {
      if (!(o instanceof Map.Entry)) { return false; }
      final Map.Entry<K, V> e = (Map.Entry<K, V>) o;
      final Object value = map.get(e.getKey());
      return value != null && value.equals(e.getValue());
    }

    @Override
    public boolean remove(final Object o) {
      if (!(o instanceof Map.Entry)) { return false; }
      final Map.Entry<K, V> e = (Map.Entry<K, V>) o;
      final Object value = map.get(e.getKey());
      if (value != null && value.equals(e.getValue())) {
        return map.remove(e.getKey()) != null;
      } else {
        return false;
      }
    }

    @Override
    public int size() {
      return map.size();
    }

  }

  public static class ClusteredMapAggregatedValuesCollection<K, V> extends AbstractCollection<V> {

    private final AggregateServerMap map;
    // contains the keys for which txn was in progress for add when the set was created.
    private final Set<K>             txnInProgressForAdd = new HashSet<K>();
    // contains the keys for which txn was in progress for add or remove when the set was created.
    private final Set<K>             filterSet           = new HashSet<K>();

    public ClusteredMapAggregatedValuesCollection(AggregateServerMap<K, V> map) {
      this.map = map;
      map.getAnyServerMap().addTxnInProgressKeys(txnInProgressForAdd, filterSet);
      filterSet.addAll(txnInProgressForAdd);
    }

    @Override
    public Iterator<V> iterator() {
      Map entryMap = new HashMap(map.getAll(txnInProgressForAdd));
      // remove the entries that might have been removed later after creation of the entryset.
      for (Iterator iterator = entryMap.entrySet().iterator(); iterator.hasNext();) {
        Entry<K, V> entry = (Entry<K, V>) iterator.next();
        if (entry.getValue() == null) {
          iterator.remove();
        }
      }
      return new AggregateMapIterator<V>(map.iterator(), entryMap.values().iterator()) {

        @Override
        public Iterator<V> getClusterMapIterator(InternalToolkitMap aMap) {
          return aMap.values(filterSet).iterator();
        }

      };
    }

    @Override
    public int size() {
      return map.size();
    }
  }
}
