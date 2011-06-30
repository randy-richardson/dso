/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;

public abstract class AbstractLocalCacheStoreValue {
  /**
   * This corresponds to a ObjectID/LockID
   */
  protected final Object id;
  /**
   * this is the value object <br>
   * TODO: make this Serializable. This would be a SerializedEntry for the serialized caches.
   */
  protected final Object value;

  public AbstractLocalCacheStoreValue(Object id, Object value) {
    this.id = id;
    this.value = value;
  }

  public Object getId() {
    return id;
  }

  public Object getValue() {
    return value;
  }

  public boolean isEventualConsistentValue() {
    return false;
  }

  public boolean isIncoherentValue() {
    return false;
  }

  public boolean isStrongConsistentValue() {
    return false;
  }

  public boolean isIncoherentTooLong() {
    return false;
  }

  public LocalCacheStoreStrongValue asStrongValue() {
    return (LocalCacheStoreStrongValue) this;
  }

  public LocalCacheStoreEventualValue asEventualValue() {
    return (LocalCacheStoreEventualValue) this;
  }

  public LocalCacheStoreIncoherentValue asIncoherentValue() {
    return (LocalCacheStoreIncoherentValue) this;
  }

  public LockID getLockId() {
    throw new UnsupportedOperationException("This should only be called for Strong consistent cached values");
  }

  public ObjectID getObjectId() {
    throw new UnsupportedOperationException("This should only be called for Eventual consistent cached values");
  }

}
