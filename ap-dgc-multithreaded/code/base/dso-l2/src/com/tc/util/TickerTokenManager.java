/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.util.TCTimer;
import com.tc.util.TCTimerImpl;
import com.tc.util.msg.TickerTokenMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

public abstract class TickerTokenManager<T extends TickerToken, M extends TickerTokenMessage> {

  private final static int                             CLEAN_TICKS       = 10;
  private final int                                    id;
  private final int                                    timerPeriod;
  private final Map<Class, TCTimer>                    timerMap          = Collections
                                                                             .synchronizedMap(new HashMap<Class, TCTimer>());
  protected final TickerTokenFactory<T, M>             factory;
  private final Map<Class, TickerTokenHandler>         tokenHandlerMap   = Collections
                                                                             .synchronizedMap(new HashMap<Class, TickerTokenHandler>());
  private final Map<Class, TickerTokenCompleteHandler> completeTickerMap = Collections
                                                                             .synchronizedMap(new HashMap<Class, TickerTokenCompleteHandler>());
  private final Counter                                tickValue         = new Counter();
  private final Counter                                cleanCount        = new Counter(CLEAN_TICKS);

  public TickerTokenManager(int id, int timerPeriod, TickerTokenFactory factory) {
    this.id = id;
    this.factory = factory;
    this.timerPeriod = timerPeriod;
  }

  public TickerTokenFactory getFactory() {
    return this.factory;
  }

  public int getId() {
    return id;
  }

  public void addTickerTokenHandler(Class tokenClass, TickerTokenHandler handler) {
    tokenHandlerMap.put(tokenClass, handler);
  }

  public void addTickerTokenCompleteHandler(Class tokenClass, TickerTokenCompleteHandler handler) {
    completeTickerMap.put(tokenClass, handler);
  }

  public void startTicker() {
    TCTimer timer = new TCTimerImpl("Ticker Timer", false);
    TickerTask task = new TickerTask(this.tickValue, this, factory, timerMap, timer);
    timer.schedule(task, timerPeriod, timerPeriod);
  }

  public void send(T token) {
    TickerTokenHandler handler = tokenHandlerMap.get(token.getClass());
    Assert.assertNotNull(handler);
    handler.processToken(token);
    M message = factory.createMessage(token);
    sendMessage(message);
  }

  public abstract void sendMessage(M message);

  public void receive(T token) {
    int cid = token.getPrimaryID();
    if (cid == this.id) {
      synchronized (this) {
        boolean dirty = false;
        for (Iterator<Boolean> iter = token.getTokenStateMap().values().iterator(); iter.hasNext();) {
          if (iter.next().booleanValue()) {
            dirty = true;
          }
        }
        if (!dirty && evaluateComplete(token)) {
          complete(token);
        }
      }
    } else {
      send(token);
    }
  }

  public abstract boolean evaluateComplete(T token);

  private void complete(T token) {
    if (cleanCount.decrement() <= 0) {
      TCTimer t = timerMap.remove(token.getClass());
      System.out.println("id: " + id + " Timer value: " + t + " tickValue: " + token.getPrimaryTickValue());
      if (t != null) {

        t.cancel();
      }
      completeTickerMap.get(token.getClass()).complete(token);
      cleanCount.reset(CLEAN_TICKS);
    }
  }

  private static class TickerTask<T extends TickerToken, M extends TickerTokenMessage> extends TimerTask {

    private final TickerTokenManager<T, M> manager;
    private final TickerTokenFactory<T, M> factory;
    private final Map                      timerMap;
    private final TCTimer                  timer;
    private final Counter                  tickValue;

    private TickerTask(Counter tickValue, TickerTokenManager manager, TickerTokenFactory factory, Map timerMap,
                       TCTimer timer) {
      this.tickValue = tickValue;
      this.manager = manager;
      this.factory = factory;
      this.timer = timer;
      this.timerMap = timerMap;
    }

    public void run() {
      T token = factory.createTriggerToken(manager.getId(), tickValue.increment());
      // System.out.println("Put into timer map: tickValue: " + token.getPrimaryTickValue() + " timer: " + timer);
      timerMap.put(token.getClass(), timer);
      manager.send(token);
    }

  }
}
