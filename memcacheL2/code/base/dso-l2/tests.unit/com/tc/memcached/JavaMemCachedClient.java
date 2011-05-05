/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.memcached;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import com.tc.util.concurrent.ThreadUtil;

public class JavaMemCachedClient {
  private final String KEY_PREFIX   = "key-";
  private final String VALUE_PREFIX = "spy-memcache-value-";

  public static void main(String[] args) throws Exception {
    new JavaMemCachedClient().start(args);
  }

  private void start(String args[]) throws Exception {
    MemcachedClient c = new MemcachedClient(AddrUtil.getAddresses("localhost:11211"));

    if (args.length > 0) {
      doPut(c);
    }

    doGet(c);
  }

  private void doGet(MemcachedClient c) {
    for (int i = 1; i <= 5; i++) {
      System.err.println("Got value for key: " + KEY_PREFIX + i + " , With value: " + c.get(KEY_PREFIX + i));
    }
  }

  private void doPut(MemcachedClient c) {
    ThreadUtil.reallySleep(5000);

    for (int i = 1; i <= 5; i++) {
      System.err.println("Putting key: " + KEY_PREFIX + i + " , With value: " + VALUE_PREFIX + i);
      c.set(KEY_PREFIX + i, 1000, VALUE_PREFIX + i);
    }

    ThreadUtil.reallySleep(5000);
  }
}
