package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.collections.ClusteredMap;

import com.tc.object.applicator.BaseApplicator;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.applicator.SelfApplicable;
import com.tc.object.TCObjectSelfImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author twu
 */
public class TerracottaClusteredMap<K, V> extends TCObjectSelfImpl implements ClusteredMap<K, V>, SelfApplicable {
  @Override
  public void drop() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean isEmpty() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean containsKey(final Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean containsValue(final Object value) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public V get(final Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public V put(final K key, final V value) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public V remove(final Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Set<K> keySet() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Collection<V> values() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public ChangeApplicator getApplicator() {
    return new BaseApplicator(null);
  }
}
