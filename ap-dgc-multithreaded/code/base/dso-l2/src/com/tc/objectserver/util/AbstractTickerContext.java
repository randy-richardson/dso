/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractTickerContext<K extends TickerToken> implements TickerContext<K> {

  protected final Set<K> tokens = new HashSet<K>();
  private final Object   lock   = new Object();
  private final int      numberOfTokens;

  public AbstractTickerContext(int numberOfTokens) {
    this.numberOfTokens = numberOfTokens;
  }

  public void firstToken(K token) {
    collectToken(token);
    passToken();
  }

  public void collectToken(K token) {
    tokens.add(token);
  }

  public void waitUntil() throws InterruptedException {
    synchronized (lock) {
      lock.wait();
    }
  }

  public boolean checkComplete(K currentToken) {
    if (currentToken.isPrimary() && checkTicks() && processCheckComplete()) {
      synchronized (lock) {
        lock.notifyAll();
      }
      return true;
    } else {
      passToken();
    }
    return false;
  }

  private boolean checkTicks() {

    if (tokens.size() < numberOfTokens) { return false; }

    int ticks = 0;
    for (Iterator<K> iter = tokens.iterator(); iter.hasNext();) {
      ticks += iter.next().tick();
    }
    return (ticks % tokens.size()) == 0;
  }

  protected abstract boolean processCheckComplete();

  protected abstract void passToken();

}