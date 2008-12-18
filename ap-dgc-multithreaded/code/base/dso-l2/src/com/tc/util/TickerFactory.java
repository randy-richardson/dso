/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.util.msg.TickerTokenMessage;

public interface TickerFactory<T extends TickerToken, M extends TickerTokenMessage> {

  public T createTriggerToken(int id, int tickValue);
  
  public T createToken(M message);

  public M createMessage(T token);
}
