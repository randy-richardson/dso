/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.TestCase;

public class TickerTokenManagerTest extends TestCase {

  private static final int                    NUMBER_OF_HANDLERS = 10;

 
  public void test() {
   createTickerTuples();
   //TODO:
  }

  private List<TickerTuple> createTickerTuples() {
    List<TickerTuple> tickerTuples = new ArrayList<TickerTuple>();

    for (int i = 0; i < NUMBER_OF_HANDLERS; i++) {
      final Queue<TestTickerTokenMessage> mQueue = new LinkedBlockingQueue<TestTickerTokenMessage>();
      final TestTickerTokenManager manager = new TestTickerTokenManager(i, 100, new TestTickerTokenFactory(), mQueue);
      final TestTickerTokenHandler handler = new TestTickerTokenHandler(i);
      final TestTickerTokenCompleteHandler completeHandler = new TestTickerTokenCompleteHandler();
      manager.addTickerTokenHandler(TestTickerToken.class, handler);
      manager.addTickerTokenCompleteHandler(TestTickerToken.class, completeHandler);
      TickerTuple tickerTuple = new TickerTuple(mQueue, manager, completeHandler, handler);
      tickerTuples.add(tickerTuple);
    }
    return tickerTuples;
  }

  private static class TickerTuple {

    public TestTickerTokenCompleteHandler getTickerTokenCompleteHandler() {
      return tickerTokenCompleteHandler;
    }

    private final Queue<TestTickerTokenMessage>  messageQueue;
    private final TestTickerTokenCompleteHandler tickerTokenCompleteHandler;
    private final TestTickerTokenManager         tickerTokenManager;
    private final TestTickerTokenHandler         tickerTokenHandler;

    public TickerTuple(Queue<TestTickerTokenMessage> messageQueue, TestTickerTokenManager tickerTokenManager,
                       TestTickerTokenCompleteHandler tickerTokenCompleteHandler,
                       TestTickerTokenHandler tickerTokenHandler) {
      this.messageQueue = messageQueue;
      this.tickerTokenManager = tickerTokenManager;
      this.tickerTokenCompleteHandler = tickerTokenCompleteHandler;
      this.tickerTokenHandler = tickerTokenHandler;
    }

    public Queue<TestTickerTokenMessage> getMessageQueue() {
      return messageQueue;
    }

    public TestTickerTokenManager getTickerTokenManager() {
      return tickerTokenManager;
    }

    public TestTickerTokenHandler getTickerTokenHandler() {
      return tickerTokenHandler;
    }
  }

}
