/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.util.TickerTokenFactory;
import com.tc.util.TickerTokenManager;
import com.tc.util.TickerToken;
import com.tc.util.msg.TickerTokenMessage;

public class TickerTokenMessageHandler extends AbstractEventHandler {

  private TickerTokenManager tickerManager;
  private TickerTokenFactory tickerFactory;
  
  public TickerTokenMessageHandler(TickerTokenManager tickerManager, TickerTokenFactory tickerFactory) {
    this.tickerManager = tickerManager;
    this.tickerFactory = tickerFactory;
  }
  
  TickerTokenFactory getFactory() {
    return tickerFactory;
  }
  
  @Override
  public void handleEvent(EventContext context) {
    if(context instanceof TickerTokenMessage) {
      TickerTokenMessage message = (TickerTokenMessage)context;
      TickerToken token = tickerFactory.createToken(message);
      tickerManager.receive(token);
    }
   
  }

}
