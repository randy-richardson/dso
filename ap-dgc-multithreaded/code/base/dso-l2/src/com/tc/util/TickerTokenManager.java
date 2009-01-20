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
import java.util.Map;
import java.util.TimerTask;

public abstract class TickerTokenManager {

  private final static int                     CLEAN_TICKS        = 10;
  private final int                            id;
  private final int                            timerPeriod;
  private final Map<Class, TCTimer>            timerMap           = Collections
                                                                      .synchronizedMap(new HashMap<Class, TCTimer>());

  private final Map<Class, TickerTokenFactory> factoryMap         = Collections
                                                                      .synchronizedMap(new HashMap<Class, TickerTokenFactory>());
  private final Map<Class, TickerTokenHandler> tokenHandlerMap    = Collections
                                                                      .synchronizedMap(new HashMap<Class, TickerTokenHandler>());
  private final Map<Class, CompleteHandler>    completeHandlerMap = Collections
                                                                      .synchronizedMap(new HashMap<Class, CompleteHandler>());
  private final Counter                        tickValue          = new Counter();
  private final Counter                        cleanCount         = new Counter(CLEAN_TICKS);
  private final int                            tokenCount;

  public TickerTokenManager(int id, int timerPeriod, int tokenCount) {
    this.id = id;
    this.timerPeriod = timerPeriod;
    this.tokenCount = tokenCount;
  }

  public int getId() {
    return id;
  }

  public void registerTickerTokenHandler(Class tokenClass, TickerTokenHandler handler) {
    tokenHandlerMap.put(tokenClass, handler);
  }

  public void registerTickerTokenFactory(Class tokenClass, TickerTokenFactory factory) {
    factoryMap.put(tokenClass, factory);
  }

  public CompleteHandler startTicker(Class tickerTokenType) {
    TCTimer timer = new TCTimerImpl(tickerTokenType.getName() + " Ticker Timer", false);
    TickerTask task = new TickerTask(this.tickValue, this.tokenCount, this, getTickerTokenFactory(tickerTokenType),
                                     timerMap, timer);
    timer.schedule(task, timerPeriod, timerPeriod);
    CompleteHandler handler = new CompleteHandler();
    completeHandlerMap.put(tickerTokenType, handler);
    return handler;
  }

  public void send(TickerToken token) {
    TickerTokenMessage message = getTickerTokenFactory(token.getClass()).createMessage(token);
    sendMessage(message);
  }

  public abstract void sendMessage(TickerTokenMessage message);

  public void receive(TickerToken token) {
    TickerTokenHandler handler = getTickerTokenHandler(token.getClass());
    handler.processToken(token);

    int cid = token.getPrimaryID();
    if (cid == this.id) {
      synchronized (this) {

        if (token.evaluateComplete()) {
          complete(token);
        }
      }
    } else {
      send(token);
    }
  }

  private TickerTokenFactory getTickerTokenFactory(Class tokenClass) {
    TickerTokenFactory factory = factoryMap.get(tokenClass);
    if (factory == null) { throw new AssertionError("factory for token class: " + tokenClass + " is not registered"); }
    return factory;
  }

  private TickerTokenHandler getTickerTokenHandler(Class tokenClass) {
    TickerTokenHandler handler = tokenHandlerMap.get(tokenClass);
    if (handler == null) { throw new AssertionError("token handler for token class: " + tokenClass
                                                    + " is not registered"); }
    return handler;
  }

  private CompleteHandler getTickerTokenCompleteHandler(Class tokenClass) {
    CompleteHandler handler = completeHandlerMap.get(tokenClass);
    if (handler == null) { throw new AssertionError("complete handler for token class: " + tokenClass
                                                    + " is not registered"); }
    return handler;
  }

  private void complete(TickerToken token) {
    if (cleanCount.decrement() <= 0) {
      TCTimer t = timerMap.remove(token.getClass());
      if (t != null) {
        t.cancel();
      }
      getTickerTokenCompleteHandler(token.getClass()).complete();
      cleanCount.reset(CLEAN_TICKS);
    }
  }

  private static class TickerTask extends TimerTask {

    private final TickerTokenManager manager;
    private final TickerTokenFactory factory;
    private final Map                timerMap;
    private final TCTimer            timer;
    private final Counter            tickValue;
    private final int                tokenCount;

    private TickerTask(Counter tickValue, int tokenCount, TickerTokenManager manager, TickerTokenFactory factory,
                       Map timerMap, TCTimer timer) {
      this.tickValue = tickValue;
      this.tokenCount = tokenCount;
      this.manager = manager;
      this.factory = factory;
      this.timer = timer;
      this.timerMap = timerMap;
    }

    public void run() {
      TickerToken token = factory.createTriggerToken(manager.getId(), tickValue.increment(), tokenCount);
      timerMap.put(token.getClass(), timer);
      manager.send(token);
    }

  }
}
