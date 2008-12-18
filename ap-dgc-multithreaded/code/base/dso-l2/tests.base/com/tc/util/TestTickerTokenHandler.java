/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

public class TestTickerTokenHandler implements TickerTokenHandler<TestTickerToken> {
   
  private final int id;
  private final TickerTokenHandlerDelegate delegate = new TickerTokenHandlerDelegate();

  public TestTickerTokenHandler(int id) {
    this.id = id;
  }
  
  public TestTickerToken processToken(TestTickerToken token) {
    token.collectToken(id, delegate.isDirtyAndClear());
    return token;
  }

}
