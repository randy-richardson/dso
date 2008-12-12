/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.util.TickerFactory;
import com.tc.util.TickerManager;
import com.tc.util.TickerToken;
import com.tc.util.msg.TickerTokenMessage;

public class TickerHandler extends AbstractEventHandler {

  private TickerManager tickerManager;
  private TickerFactory tickerFactory;
  
  public TickerHandler(TickerManager tickerManager, TickerFactory tickerFactory) {
    this.tickerManager = tickerManager;
    this.tickerFactory = tickerFactory;
  }
  
  @Override
  public void handleEvent(EventContext context) {
    if(context instanceof TickerTokenMessage) {
      TickerTokenMessage message = (TickerTokenMessage)context;
      TickerToken token = tickerFactory.createToken(message);
      tickerManager.recieve(token);
    }
   
  }

}
