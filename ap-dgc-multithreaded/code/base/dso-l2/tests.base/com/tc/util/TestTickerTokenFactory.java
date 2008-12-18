/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

public class TestTickerTokenFactory implements TickerTokenFactory<TestTickerToken, TestTickerTokenMessage> {

  public TestTickerTokenMessage createMessage(TestTickerToken token) {
    TestTickerTokenMessage message = new TestTickerTokenMessage();
    message.init(token);
    return message;
  }

  public TestTickerToken createToken(TestTickerTokenMessage message) {
    return message.getTickerToken();
  }

  public TestTickerToken createTriggerToken(int id, int tickValue) {
    return new TestTickerToken(id, tickValue);
  }

}
