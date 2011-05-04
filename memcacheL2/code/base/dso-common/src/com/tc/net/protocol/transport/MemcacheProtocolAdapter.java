/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.TCProtocolException;

import java.util.ArrayList;

public class MemcacheProtocolAdapter implements TCProtocolAdaptor {

  private final Sink                    memcacheSink;
  private final ArrayList<TCByteBuffer> dataBuffers;

  public MemcacheProtocolAdapter(Sink memcacheSink) {
    this.memcacheSink = memcacheSink;
    this.dataBuffers = new ArrayList<TCByteBuffer>();
  }

  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
    //
  }

  public TCByteBuffer[] getReadBuffers() {
    return TCByteBufferFactory.getFixedSizedInstancesForLength(true, 128);
  }

}
