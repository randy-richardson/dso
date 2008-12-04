/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.util;

import junit.framework.TestCase;

public class TickerTokenTest extends TestCase {
  
  private TickerContextManager manager;

  @Override
  protected void setUp() throws Exception {
    manager = new TickerContextManager<TestTickerContext, TestTickerToken>();
  }
  
  public void testTickerTokens() {
   // manager.checkTicker(tickerContext, currentToken);
    TestTickerContext tickerContext = new TestTickerContext(5);
    TestTickerToken firstToken = new TestTickerToken(true, 1);
    manager.startTicker(tickerContext, firstToken);
    
    for(int i = 0; i < 5; i++) {
      TestTickerToken currentToken = new TestTickerToken(1);
      manager.checkTicker(tickerContext, currentToken);
    }
    
    
  }
  
  

}
