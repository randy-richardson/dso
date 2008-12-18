/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.util.msg.TickerTokenMessage;

public class TestTickerTokenMessage implements TickerTokenMessage<TestTickerToken> {

  private TestTickerToken tickerToken;

  public TestTickerToken getTickerToken() {
    return this.tickerToken;
  }

  public void init(TestTickerToken token) {
    this.tickerToken = token;
  }

}