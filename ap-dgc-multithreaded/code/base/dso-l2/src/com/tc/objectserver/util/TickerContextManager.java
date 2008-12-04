/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.util;

import com.tc.util.TCTimer;
import com.tc.util.TCTimerImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

public class TickerContextManager< K extends AbstractTickerContext, T extends TickerToken > {
  
  private static final int TIMER_PERIOD = 100;
  private final Map<K, TCTimer> timerMap = new HashMap<K, TCTimer>();
  
  public void startTicker(K tickerContext, T firstToken ) {
    TCTimer timer = new TCTimerImpl("Ticker Timer", false);
    TickerTask<K,T> task = new TickerTask<K,T>(tickerContext, firstToken);
    timer.schedule(task, TIMER_PERIOD, TIMER_PERIOD);
    timerMap.put(tickerContext, timer);
  }
  
  public void checkTicker(K tickerContext, T currentToken ) {
    if(tickerContext.checkComplete(currentToken)) {
      timerMap.get(tickerContext).cancel();
    } 
  }
  
  private static class TickerTask<K extends AbstractTickerContext, T extends TickerToken > extends TimerTask {

    private final K tickerContext;
    private final T firstToken;

    private TickerTask(K tickerContext, T firstToken) {
      this.tickerContext = tickerContext;
      this.firstToken = firstToken;
    }

    public void run() {   
      tickerContext.firstToken(firstToken);
    }

  }
}
