package com.terracotta.toolkit.collections.map;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.toolkit.collections.ClusteredMap;

import com.tc.object.TCObjectSelfImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author twu
 */
public class TerracottaClusteredMap<K, V> extends TCObjectSelfImpl implements ClusteredMap<K, V> {
  private final EntityClientEndpoint endpoint;

  public TerracottaClusteredMap(final EntityClientEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public void release() {
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
    try {
      return (V) endpoint.beginInvoke().payload("get " + key).returnsValue(true).invoke().get();
    } catch (Exception e) {
      throw new RuntimeException("oh crap.", e);
    }
  }

  @Override
  public V put(final K key, final V value) {
    try {
      return (V) endpoint.beginInvoke().payload("put " + key + " " + value).invoke().get();
    } catch (Exception e) {
      throw new RuntimeException("oh crap.", e);
    }
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

}
