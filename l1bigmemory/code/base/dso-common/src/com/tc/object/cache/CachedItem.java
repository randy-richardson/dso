/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.cache.ExpirableEntry;
import com.tc.local.cache.store.DisposeListener;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class CachedItem {

  private final DisposeListener                                                 listener;
  /*
   * Could be a LockID or an ObjectID that this CacheItem is mapped to in RemoteServerMapManagerImpl or it could be null
   * for incoherent items
   */
  private final Object                                                          id;
  private final Object                                                          key;
  private final Object                                                          value;

  private volatile CachedItemState                                              state;
  private static final AtomicReferenceFieldUpdater<CachedItem, CachedItemState> refUpdater = AtomicReferenceFieldUpdater
                                                                                               .newUpdater(
                                                                                                           CachedItem.class,
                                                                                                           CachedItemState.class,
                                                                                                           "state");

  private volatile boolean                                                      expired    = false;
  private CachedItem                                                            next;

  public CachedItem(final Object id, final DisposeListener listener, final Object key, final Object value,
                    CachedItemInitialization initializeState) {
    this.listener = listener;
    this.id = id;
    this.key = key;
    this.value = value;
    switch (initializeState) {
      case WAIT_FOR_ACK:
        this.state = CachedItemState.UNACKED_ACCESSED;
        break;
      case NO_WAIT_FOR_ACK:
        this.state = CachedItemState.ACKED_ACCESSED;
        break;
      case REMOVE_ON_TXN_COMPLETE:
        this.state = CachedItemState.REMOVE_ON_TXN_COMPLETE;
        break;
      default:
        throw new AssertionError("Unknow state - " + initializeState);
    }
  }

  public Object getID() {
    return this.id;
  }

  public Object getKey() {
    return this.key;
  }

  public ExpirableEntry getExpirableEntry() {
    if (this.value instanceof ExpirableEntry) { return (ExpirableEntry) this.value; }
    return null;
  }

  public Object getValue() {
    accessed();
    return this.value;
  }

  public void dispose() {
    this.listener.disposed(this);
  }

  public CachedItem getNext() {
    return this.next;
  }

  public void setNext(final CachedItem next) {
    this.next = next;
  }

  public boolean getAndClearAccessed() {
    final boolean isAccessed = state.isAccessed();
    clearAccessed();
    return isAccessed;
  }

  public boolean isExpired() {
    return this.expired;
  }

  public void markExpired() {
    this.expired = true;
  }

  public DisposeListener getListener() {
    return listener;
  }

  private void accessed() {
    boolean success = false;
    do {
      final CachedItemState lstate = state;
      success = update(lstate, lstate.accessed());
    } while (!success);
  }

  private void clearAccessed() {
    boolean success = false;
    do {
      final CachedItemState lstate = state;
      success = update(lstate, lstate.clearAccessed());
    } while (!success);
  }

  private boolean update(CachedItemState previous, CachedItemState newState) {
    return refUpdater.compareAndSet(this, previous, newState);
  }

  public static enum CachedItemInitialization {
    WAIT_FOR_ACK, NO_WAIT_FOR_ACK, REMOVE_ON_TXN_COMPLETE;
  }

}