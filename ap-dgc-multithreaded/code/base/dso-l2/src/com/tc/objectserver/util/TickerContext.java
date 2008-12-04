/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.util;


/**
 * this interface is to collect all tokens and update these content.
 */
public interface TickerContext<K extends TickerToken> {
  
  public void collectToken(K k);

}
