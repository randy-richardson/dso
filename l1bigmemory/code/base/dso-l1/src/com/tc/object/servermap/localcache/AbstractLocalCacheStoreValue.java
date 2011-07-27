/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelfStore;
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
  private final ObjectID mapID;

  public AbstractLocalCacheStoreValue(Object id, Object value, ObjectID mapID) {
    this.id = id;
    this.value = value;
    this.mapID = mapID;
  }

  public Object getId() {
    return id;
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

  public Object getValueObject(TCObjectSelfStore tcObjectSelfStore, L1ServerMapLocalCacheStore store) {
    if (value instanceof ObjectID) {
      return tcObjectSelfStore.getByIdFromStore((ObjectID) value, store);
    } else {
      return value;
    }
  }

  /**
   * Returns true if this is cached value for eventual consistency
   */
  public boolean isEventualConsistentValue() {
    return false;
  }

  /**
   * Returns true if this is cached value for incoherent/bulk-load
   */
  public boolean isIncoherentValue() {
    return false;
  }

  /**
   * Returns true if this is cached value for strong consistency
   */
  public boolean isStrongConsistentValue() {
    return false;
  }

  /**
   * Returns true only if {@link #isIncoherentValue()} and has been incoherent for too long
   */
  public boolean isIncoherentTooLong() {
    return false;
  }

  /**
   * Returns this object as {@link LocalCacheStoreStrongValue}. Use only when {@link #isStrongConsistentValue()} is
   * true, otherwise will throw ClassCastException
   */
  public LocalCacheStoreStrongValue asStrongValue() {
    return (LocalCacheStoreStrongValue) this;
  }

  /**
   * Returns this object as {@link LocalCacheStoreEventualValue}. Use only when {@link #isEventualConsistentValue()} is
   * true, otherwise will throw ClassCastException
   */
  public LocalCacheStoreEventualValue asEventualValue() {
    return (LocalCacheStoreEventualValue) this;
  }

  /**
   * Returns this object as {@link LocalCacheStoreIncoherentValue}. Use only when {@link #isIncoherentValue()} is true,
   * otherwise will throw ClassCastException
   */
  public LocalCacheStoreIncoherentValue asIncoherentValue() {
    return (LocalCacheStoreIncoherentValue) this;
  }

  /**
   * Use only when {@link #isStrongConsistentValue()} is true. Returns the lock Id
   */
  public LockID getLockId() {
    throw new UnsupportedOperationException("This should only be called for Strong consistent cached values");
  }

  /**
   * Use only when {@link #isEventualConsistentValue()} is true. Returns the object id
   */
  public ObjectID getObjectId() {
    if (value instanceof ObjectID) { return (ObjectID) value; }
    return ObjectID.NULL_ID;
  }

  @Override
  public String toString() {
    return "AbstractLocalCacheStoreValue [id=" + id + ", mapID=" + mapID + ", value=" + value + "]";
  }
}
