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
package com.tc.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/*
 * LazyMap delay underneath Map creation to save memory. Map created only when more than one entry added. Not supporting
 * null key.
 */
public abstract class LazyMap<K, V> implements Map<K, V> {
  public static final int   MAP_SIZE    = 2;
  public static final float LOAD_FACTOR = 1F;

  private Map<K, V>         map         = null;
  private K                 key         = null;
  private V                 value       = null;

  abstract Map<K, V> create();

  private boolean isSameKey(Object aKey) {
    if (key == null) {
      return false;
    } else {
      return key.equals(aKey);
    }
  }

  private boolean isEntryExist() {
    return (key != null);
  }

  @Override
  public V put(K aKey, V aValue) {
    if (aKey == null) throw new NullPointerException("LazyHash not support null key");

    if (map != null) { return map.put(aKey, aValue); }

    if (!isEntryExist()) {
      this.key = aKey;
      this.value = aValue;
      return null;
    } else if (isSameKey(aKey)) {
      // add the same key
      V v = value;
      value = aValue;
      return v;
    } else {
      // when more than one entry added
      map = create();
      map.put(key, value);
      key = null;
      value = null;
      return map.put(aKey, aValue);
    }
  }

  @Override
  public V get(Object aKey) {
    if (map != null) { return map.get(aKey); }

    if (isSameKey(aKey)) {
      return value;
    } else {
      return null;
    }
  }

  @Override
  public void clear() {
    if (map != null) {
      map = null;
    }

    key = null;
    value = null;
  }

  @Override
  public boolean containsKey(Object aKey) {
    if (map != null) { return map.containsKey(aKey); }

    return isSameKey(aKey);
  }

  @Override
  public boolean containsValue(Object aValue) {
    if (map != null) { return map.containsValue(aValue); }

    if (key != null) {
      if (value == null) {
        return (aValue == null);
      } else {
        return value.equals(aValue);
      }
    }
    return false;
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    if (map != null) { return map.entrySet(); }

    return new LazyEntrySet<K, V>(this);
  }

  @Override
  public boolean isEmpty() {
    if (map != null) { return map.isEmpty(); }

    return (!isEntryExist());
  }

  @Override
  public Set<K> keySet() {
    if (map != null) { return map.keySet(); }

    return new LazyKeySet<K>(this);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    if (m.size() == 0) return;
    if (map == null) {
      map = create();
      if (isEntryExist()) {
        map.put(key, value);
        key = null;
        value = null;
      }
    }
    // same as map.putAll(m) but loop to abort null key
    for (Iterator<? extends Map.Entry<? extends K, ? extends V>> i = m.entrySet().iterator(); i.hasNext();) {
      Map.Entry<? extends K, ? extends V> e = i.next();
      if (e.getKey() == null) throw new NullPointerException("LazyHash not support null key");
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public V remove(Object aKey) {
    if (map != null) { return map.remove(aKey); }

    if (isSameKey(aKey)) {
      V v = value;
      key = null;
      value = null;
      return v;
    } else {
      return null;
    }
  }

  @Override
  public int size() {
    if (map != null) { return map.size(); }

    return (isEntryExist()) ? 1 : 0;
  }

  @Override
  public Collection<V> values() {
    if (map != null) { return map.values(); }

    Collection<V> c = new LazyCollection<V>(this);
    return c;
  }

  // helper methods for Lazy Iterators

  private K getEntryKey() {
    return (isEntryExist()) ? key : null;
  }

  private V getEntryValue() {
    return (isEntryExist()) ? value : null;
  }

  private SimpleEntry<K, V> getEntry() {
    return (isEntryExist()) ? new SimpleEntry<K, V>(this) : null;
  }

  private void removeEntry() {
    key = null;
    value = null;
  }

  private boolean isMapCreated() {
    return (map != null);
  }

  private V setValue(V aValue) {
    V old = this.value;
    this.value = aValue;
    return old;
  }

  /*
   * LazyCollection -- for LazyMap.values() supporting Collection view.
   */
  private static class LazyCollection<V> extends AbstractCollection<V> {
    private final LazyMap map;

    LazyCollection(LazyMap map) {
      this.map = map;
    }

    @Override
    public Iterator<V> iterator() {
      if (map.isMapCreated()) {
        return map.values().iterator();
      } else {
        return new LazyValueIterator<V>(map);
      }
    }

    @Override
    public int size() {
      return map.size();
    }

  }

  /*
   * LazyKeySet -- for LazyMap.keySet(), supporting Set view.
   */
  private static class LazyKeySet<K> extends AbstractSet<K> {
    private final LazyMap map;

    LazyKeySet(LazyMap map) {
      this.map = map;
    }

    @Override
    public Iterator<K> iterator() {
      if (map.isMapCreated()) {
        return map.keySet().iterator();
      } else {
        return new LazyKeyIterator<K>(map);
      }
    }

    @Override
    public int size() {
      return map.size();
    }

  }

  /*
   * LazyEntrySet -- for LazyMap.entrySet(), supporting Set view.
   */
  private static class LazyEntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> {
    private final LazyMap map;

    LazyEntrySet(LazyMap map) {
      this.map = map;
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      if (map.isMapCreated()) {
        return map.entrySet().iterator();
      } else {
        return new LazyEntryIterator<Map.Entry<K, V>>(map);
      }
    }

    @Override
    public int size() {
      return map.size();
    }

  }

  /*
   * LazyIterator -- Iterator for LazyMap when one or none entry in the map. methods go back to backup Map to support
   * view.
   */
  private static abstract class LazyIterator<T> implements Iterator<T> {
    private final LazyMap map;
    private int           index = 0;
    private int           expectedMapSize;

    LazyIterator(LazyMap map) {
      this.map = map;
      expectedMapSize = map.size();
    }

    @Override
    public boolean hasNext() {
      return (map.size() == 1 && index == 0);
    }

    private boolean isCurrentAvailable() {
      return (map.size() == 1 && index == 1);
    }

    Object getKey() {
      return map.getEntryKey();
    }

    Object getValue() {
      return map.getEntryValue();
    }

    Object getEntry() {
      return map.getEntry();
    }

    @Override
    public void remove() {
      if (index == 0 || map.size() == 0) { throw new IllegalStateException(); }
      if (expectedMapSize != map.size()) { throw new ConcurrentModificationException(); }
      if (isCurrentAvailable()) {
        map.removeEntry();
        expectedMapSize = map.size();
      }
    }

    @Override
    public T next() {
      if (expectedMapSize != map.size()) { throw new ConcurrentModificationException(); }
      ++index;
      if (isCurrentAvailable()) {
        return (T) getObject();
      } else {
        throw new NoSuchElementException();
      }
    }

    public abstract Object getObject();
  }

  /*
   * LazyKeyInterator -- Iterator for LazyMap.keySet().
   */
  private static class LazyKeyIterator<T> extends LazyIterator<T> {

    LazyKeyIterator(LazyMap map) {
      super(map);
    }

    @Override
    public Object getObject() {
      return getKey();
    }

  }

  /*
   * LazyValueIterator -- Iterator for LazyMap.values().
   */
  private static class LazyValueIterator<T> extends LazyIterator<T> {

    LazyValueIterator(LazyMap map) {
      super(map);
    }

    @Override
    public Object getObject() {
      return getValue();
    }

  }

  /*
   * LazyEntryIterator -- Iterator for LazyMap.entrySet().
   */
  private static class LazyEntryIterator<T> extends LazyIterator<T> {

    LazyEntryIterator(LazyMap map) {
      super(map);
    }

    @Override
    public Object getObject() {
      return getEntry();
    }

  }

  /*
   * SimpleEntry
   */
  private static class SimpleEntry<K, V> implements Entry<K, V> {
    private final LazyMap<K, V> map;

    public SimpleEntry(LazyMap<K, V> map) {
      this.map = map;
    }

    @Override
    public K getKey() {
      K key = map.getEntryKey();
      if (key == null) { throw new ConcurrentModificationException(); }
      return key;
    }

    @Override
    public V getValue() {
      return map.getEntryValue();
    }

    @Override
    public V setValue(V value) {
      return map.setValue(value);
    }

    private static boolean eq(Object o1, Object o2) {
      return o1 == null ? o2 == null : o1.equals(o2);
    }

    @Override
    public boolean equals(Object o) {
      K key = getKey();
      V value = map.getEntryValue();
      if (!(o instanceof Map.Entry)) return false;
      Map.Entry e = (Map.Entry) o;
      return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    @Override
    public int hashCode() {
      K key = getKey();
      V value = map.getEntryValue();
      return key.hashCode() ^ (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
      return getKey() + "=" + map.getEntryValue();
    }

  }

  /*
   * LazyHashMap -- Delay the creation of HashMap when 2 or more entries added to save memory for none or one entry Map.
   */
  public static class LazyHashMap<K, V> extends LazyMap<K, V> {
    @Override
    Map<K, V> create() {
      return new HashMap<K, V>(MAP_SIZE, LOAD_FACTOR);
    }
  }

  /*
   * LazyLinkedHashMap -- Delay the creation of LinkedHashMap to save memory for one ore one entry LinkedhashMap.
   */
  public static class LazyLinkedHashMap<K, V> extends LazyMap<K, V> {
    @Override
    Map<K, V> create() {
      return new LinkedHashMap<K, V>(MAP_SIZE, LOAD_FACTOR);
    }
  }

}
