/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.TestCase;

public class TickerTokenManagerTest extends TestCase {

  private static final int NUMBER_OF_HANDLERS = 10;

  public void test() {
    List<TickerTuple> tuples = createTickerTuples();

    TestTickerTokenManager tickerTokenManager = tuples.get(0).getTickerTokenManager();
    TestTickerTokenCompleteHandler completeHandler = new TestTickerTokenCompleteHandler(tickerTokenManager, tuples);
    tickerTokenManager.addTickerTokenCompleteHandler(TestTickerToken.class, completeHandler);
    tickerTokenManager.startTicker();

  }

  private static class TestTickerTokenCompleteHandler implements TickerTokenCompleteHandler<TestTickerToken> {

    private final TestTickerTokenManager manager;
    private final List<TickerTuple> tuples;

    public TestTickerTokenCompleteHandler(TestTickerTokenManager manager, List<TickerTuple> tuples) {
      this.manager = manager;
      this.tuples = tuples;
    }

    public void complete(TestTickerToken token) {
      assertEquals(token.getTokenStateMap().size(), NUMBER_OF_HANDLERS);

      for (Iterator<Boolean> iter = token.getTokenStateMap().values().iterator(); iter.hasNext();) {
        assertFalse(iter.next());
      }
      
      assertEquals(manager.getId(), token.getPrimaryID());
      
      for(Iterator<TickerTuple> iter = tuples.iterator(); iter.hasNext();) {
        TickerTuple tuple = iter.next();
        assertEquals(tuple.getMessageQueue().size(), 1);
      }
    }

  }

  private List<TickerTuple> createTickerTuples() {
    List<TickerTuple> tickerTuples = new ArrayList<TickerTuple>();

    for (int i = 0; i < NUMBER_OF_HANDLERS; i++) {
      final Queue<TestTickerTokenMessage> mQueue = new LinkedBlockingQueue<TestTickerTokenMessage>();
      final TestTickerTokenManager manager = new TestTickerTokenManager(i, 100, new TestTickerTokenFactory(), mQueue, NUMBER_OF_HANDLERS);
      TickerTuple tickerTuple = new TickerTuple(mQueue, manager);
      tickerTuples.add(tickerTuple);
    }
    return tickerTuples;
  }

  private static class TickerTuple {

    private final Queue<TestTickerTokenMessage> messageQueue;
    private final TestTickerTokenManager        tickerTokenManager;

    public TickerTuple(Queue<TestTickerTokenMessage> messageQueue, TestTickerTokenManager tickerTokenManager) {
      this.messageQueue = messageQueue;
      this.tickerTokenManager = tickerTokenManager;
    }

    public Queue<TestTickerTokenMessage> getMessageQueue() {
      return messageQueue;
    }

    public TestTickerTokenManager getTickerTokenManager() {
      return tickerTokenManager;
    }

  }

}
