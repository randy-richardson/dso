/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.Queue;

public class TestTickerTokenManager extends TickerTokenManager<TestTickerToken, TestTickerTokenMessage> {
  
  private final Queue<TestTickerTokenMessage> mQueue;
  
  public TestTickerTokenManager(int id, int timerPeriod, TickerTokenFactory factory, Queue<TestTickerTokenMessage> mQueue, int tokenCount) {
    super(id, timerPeriod, factory, tokenCount);
    this.mQueue = mQueue;
  }

  @Override
  public boolean evaluateComplete(TestTickerToken token) {
     return true;
  }

  @Override
  public void sendMessage(TestTickerTokenMessage message) {
    mQueue.add(message);
  }
  
}