/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.Queue;

public class TestTickerManager extends TickerManager {

  private final Queue queue;
  
  public TestTickerManager(int id, int timerPeriod, TickerTokenFactory factory, Queue queue) {
    super(id, timerPeriod, factory);
    this.queue = queue;
  }

  @Override
  public void sendMessage(TickerToken token) {
    queue.add(token);
  }

}
