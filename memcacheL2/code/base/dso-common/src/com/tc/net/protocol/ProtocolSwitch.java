/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.core.TCConnection;
import com.tc.util.Assert;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Inspects the first few bytes from a socket to decide if it should stay in the TC comms stack, or if it should be
 * handed off to Jetty. Evolving this switch behaviour to more protocols should probably involve refactoring the generic
 * comms code, not just this class
 */
public class ProtocolSwitch implements TCProtocolAdaptor {

  private static final String[]   HTTP_METHODS         = new String[] { "GET", "POST", "HEAD", "PUT", "OPTIONS",
      "DELETE", "TRACE", "CONNECT"                    };
  private static final String[]   MEMCACHE_METHODS     = new String[] { "get", "set", "add" };

  private static final Set        METHODS              = new HashSet(Arrays.asList(HTTP_METHODS));
  private static final Set        MEMCACHE_METHODS_MAP = new HashSet(Arrays.asList(MEMCACHE_METHODS));

  // The longest HTTP method is 7 chars, +1 for the space
  private static final int        INSPECT              = 8;

  private static final int        PROTOCOL_UNKNOWN     = 0;
  private static final int        PROTOCOL_NOT_HTTP    = 1;
  private static final int        PROTOCOL_HTTP        = 2;
  private static final int        PROTOCOL_MEMCACHE    = 3;

  private volatile int            protocol             = PROTOCOL_UNKNOWN;
  private final TCByteBuffer[]    buffer               = new TCByteBuffer[] { TCByteBufferFactory
                                                           .wrap(new byte[INSPECT]) };
  private final TCProtocolAdaptor delegate;
  private final Sink              httpSink;

  public ProtocolSwitch(TCProtocolAdaptor delegate, Sink httpSink) {
    this.delegate = delegate;
    this.httpSink = httpSink;
  }

  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
    switch (protocol) {
      case PROTOCOL_NOT_HTTP: {
        delegate.addReadData(source, data, length);
        return;
      }

      case PROTOCOL_MEMCACHE: {
        delegate.addReadData(source, data, length);
        return;
      }

      case PROTOCOL_UNKNOWN: {
        Assert.assertEquals(1, data.length);
        TCByteBuffer buf = data[0];
        if (buf.hasRemaining()) {
          // didn't get enough bytes yet to make a decision
          return;
        }

        buf.flip();
        boolean isHttp = isHttp(buf);
        buf.rewind();
        boolean isMemcache = isMemcache(buf);
        buf.rewind();

        if (isHttp) {
          protocol = PROTOCOL_HTTP;
          final Socket socket;
          try {
            socket = source.detach();
          } catch (IOException e) {
            throw new TCProtocolException(e);
          }
          httpSink.add(new HttpConnectionContext(socket, buf));
          return;
        }

        if (isMemcache) {
          protocol = PROTOCOL_MEMCACHE;
          delegate.addReadData(source, data, length);
          return;
        }

        protocol = PROTOCOL_NOT_HTTP;
        feedDataToDelegate(source, buf);
        return;
      }
      default:
        throw new AssertionError("Protocol is " + protocol);
    }

    // unreachable
  }

  private void feedDataToDelegate(TCConnection source, TCByteBuffer src) throws TCProtocolException {
    while (src.hasRemaining()) {
      int count = 0;

      TCByteBuffer[] readBuffers = delegate.getReadBuffers();
      for (TCByteBuffer dest : readBuffers) {
        int len = Math.min(src.remaining(), dest.remaining());
        count += len;
        for (int j = 0; j < len; j++) {
          dest.put(src.get());
        }
        if (!src.hasRemaining()) {
          break;
        }
      }

      delegate.addReadData(source, readBuffers, count);
    }
  }

  private static boolean isMemcache(TCByteBuffer buf) {
    byte[] bytes = new byte[buf.limit()];
    buf.get(bytes);

    final String s;
    try {
      s = new String(bytes);
    } catch (Exception e) {
      return false;
    }

    int spaceIndex = s.indexOf(' ');
    if (spaceIndex < 0) { return false; }

    String token = s.substring(0, spaceIndex);

    System.err.println("XXX isMem : " + token + " - " + Arrays.toString(bytes));
    return MEMCACHE_METHODS_MAP.contains(token);
  }

  private static boolean isHttp(TCByteBuffer buf) {
    // Assert.assertEquals(INSPECT, buf.limit());
    byte[] bytes = new byte[buf.limit()];
    buf.get(bytes);

    final String s;
    try {
      s = new String(bytes);
    } catch (Exception e) {
      return false;
    }

    int spaceIndex = s.indexOf(' ');
    if (spaceIndex < 0) { return false; }

    String token = s.substring(0, spaceIndex);

    // best I can tell, HTTP methods are case sensitive, so I'm not uppercase'ing the token here
    return METHODS.contains(token);
  }

  public TCByteBuffer[] getReadBuffers() {
    switch (protocol) {
      case PROTOCOL_NOT_HTTP: {
        return delegate.getReadBuffers();
      }
      case PROTOCOL_UNKNOWN: {
        return buffer;
      }
      default:
        throw new AssertionError("Protocol is " + protocol);
    }

    // unreachable
  }
}
