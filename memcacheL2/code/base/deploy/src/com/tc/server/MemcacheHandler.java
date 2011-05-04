/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.net.protocol.MemcacheEventContext;

public class MemcacheHandler extends AbstractEventHandler {

  @Override
  public void handleEvent(EventContext context) {
    if (!(context instanceof MemcacheEventContext)) { throw new AssertionError("Unexpected message : " + context); }
    System.err.println("XXX MemCache Handler: " + ((MemcacheEventContext) context).getData());
  }

}
