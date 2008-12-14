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

public abstract class TickerManager {

  private final int id;
  private final int timerPeriod;
  private final Map<Integer, TCTimer> timerMap = Collections.synchronizedMap(new HashMap<Integer, TCTimer>());
  protected final TickerFactory factory;
  private final Map<Class, TallyHandler> tallyTokenMap = Collections.synchronizedMap(new HashMap<Class, TallyHandler>());
  private final Map<Class, TickerCompleteListener> completeTickerMap = Collections.synchronizedMap(new HashMap<Class, TickerCompleteListener>());
  private final Counter tickValue = new Counter();

  public TickerManager(int id, int timerPeriod,TickerFactory factory) {
    this.id = id;
    this.factory = factory;
    this.timerPeriod = timerPeriod;
  }
  
  public TickerFactory getFactory() {
    return this.factory;
  }

  public int getId() {
    return id;
  }

  public void addTallyHandler(Class tokenClass, TallyHandler handler) {
    tallyTokenMap.put(tokenClass, handler);
  }

  public void addCompleteListener(Class tokenClass, TickerCompleteListener listener) {
    completeTickerMap.put(tokenClass, listener);
  }

  public void startTicker() {
    TCTimer timer = new TCTimerImpl("Ticker Timer", false);
    TickerTask task = new TickerTask(this.tickValue, this, factory, timerMap, timer);
    timer.schedule(task, timerPeriod, timerPeriod);
  }

  public void send(TickerToken token) {
    TallyHandler handler = tallyTokenMap.get(token.getClass());
    Assert.assertNotNull(handler);
    token.collectToken(id, handler.isDirtyAndClear());
    TickerTokenMessage message = factory.createMessage(token);
    sendMessage(message);
  }

  public abstract void sendMessage(TickerTokenMessage message);

  public void recieve(TickerToken token) {
    int cid = token.getID();
    if (cid == this.id) {
      boolean dirty = false;
      for (Iterator<Boolean> iter = token.getTokenStateMap().values().iterator(); iter.hasNext();) {
        if (iter.next().booleanValue()) {
          dirty = true;
        }
      }
      if(!dirty && evaluateComplete(token)) {
        complete( token );
      }
    }
    send(token);
  }
  
  public abstract boolean evaluateComplete(TickerToken token);
  
  private void complete(TickerToken token ) {
    TCTimer t = timerMap.remove(token.getTickValue());
    System.out.println("Timer value: " + t + " tickValue: " + token.getTickValue());
    if(t != null) {
      
      t.cancel();
    }
    completeTickerMap.get(token.getClass()).complete();
    
    
  }

  private static class TickerTask extends TimerTask {

    private final TickerManager      manager;
    private final TickerFactory factory;
    private final Map                timerMap;
    private final TCTimer            timer;
    private final Counter            tickValue;

    private TickerTask(Counter tickValue, TickerManager manager, TickerFactory factory, Map timerMap, TCTimer timer) {
      this.tickValue = tickValue;
      this.manager = manager;
      this.factory = factory;
      this.timer = timer;
      this.timerMap = timerMap;
    }

    public void run() {
      TickerToken token = factory.createTriggerToken(manager.getId(),  tickValue.increment());   
      System.out.println("Put into timer map: tickValue: " + token.getTickValue() + " timer: " + timer);
      timerMap.put(token.getTickValue(), timer);
      manager.send(token);
    }

  }
}
