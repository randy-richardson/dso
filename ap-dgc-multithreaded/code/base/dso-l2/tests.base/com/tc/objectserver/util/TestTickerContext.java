/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.util;


public class TestTickerContext extends AbstractTickerContext<TestTickerToken> {
  
  public TestTickerContext(int numberOfTokens) {
    super(numberOfTokens);
  }

  private int currentGroupIndex = 0;
  
  
  @Override
  protected void passToken() {
    currentGroupIndex++;
  }

  @Override
  protected boolean processCheckComplete() {
    return true;
  }

}
