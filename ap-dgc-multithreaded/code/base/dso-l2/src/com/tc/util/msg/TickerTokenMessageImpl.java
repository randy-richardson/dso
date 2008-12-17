/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.TickerToken;

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
  
  public T getTickerToken() {
    return this.tickerToken;
  }

  public void init(T aTickerToken) {
    this.tickerToken = aTickerToken;
  }

  protected void basicDeserializeFrom(TCByteBufferInput in) {
    //
  }

  protected void basicSerializeTo(TCByteBufferOutput out) {
    //
  }


}

