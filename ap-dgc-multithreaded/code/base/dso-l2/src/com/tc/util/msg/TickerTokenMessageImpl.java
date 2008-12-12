/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.msg;

import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.TickerToken;

import java.io.ObjectInput;
import java.io.ObjectOutput;

public abstract class TickerTokenMessageImpl<T extends TickerToken>  extends AbstractGroupMessage implements TickerTokenMessage<T> {
  
  protected T tickerToken;
  
  public TickerTokenMessageImpl() {
    super(-1);
  }
  
  public TickerTokenMessageImpl(int type) {
    super(type);
  }
  
  public TickerTokenMessageImpl(int type, MessageID requestID) {
    super(type, requestID);
  }
  
  @Override
  protected void basicReadExternal(int msgType, ObjectInput in) {
    //
  }

  @Override
  protected void basicWriteExternal(int msgType, ObjectOutput out) {
    //
  }

  public T getTickerToken() {
    return this.tickerToken;
  }

  public void init(T aTickerToken) {
    this.tickerToken = aTickerToken;
  }



}

