/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import junit.framework.TestCase;

public class TickerTokenMessageTest extends TestCase {
  
  public void testMessageAndFactory() {
    TestTickerTokenFactory factory = new TestTickerTokenFactory();
    TestTickerToken triggerToken = factory.createTriggerToken(0, 1);
    
    TestTickerTokenMessage message = factory.createMessage(triggerToken);
    
    assertEquals(triggerToken, message.getTickerToken());
    
    TestTickerToken token  = factory.createToken(message);
    
    assertEquals(message.getTickerToken(), token);
    
    
    TestTickerToken triggerToken2 = factory.createTriggerToken(1, 3);
    
    message.init(triggerToken2);
    
    TestTickerToken token2 = message.getTickerToken();
    
    assertEquals(1, token2.getPrimaryID());
    assertEquals(3, token2.getPrimaryTickValue());
    
  }

}
