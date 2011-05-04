/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;

public class MemcacheEventContext implements EventContext {

  private final TCByteBuffer[] data;
  private final int            length;

  public MemcacheEventContext(TCByteBuffer[] data, int length) {
    this.data = data;
    this.length = length;
  }

  public TCByteBuffer[] getData() {
    return data;
  }

  public int getLength() {
    return length;
  }

}
