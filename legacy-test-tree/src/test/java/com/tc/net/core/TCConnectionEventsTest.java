/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.net.core;

import com.tc.logging.TCLogging;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventCaller;
import com.tc.net.core.event.TCConnectionEventListener;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class TCConnectionEventsTest extends TestCase {

  // Exception in events should NOT be eaten
  public void testExceptions() {
    TCConnectionEventCaller caller = new TCConnectionEventCaller(TCLogging.getTestingLogger(getClass()));

    Listener listener = new Listener();

    List listeners = new ArrayList();
    listeners.add(listener);

    try {
      caller.fireCloseEvent(listeners, null);
      fail();
    } catch (Exception e) {
      failIfNotMyException(e);
    }

    try {
      caller.fireConnectEvent(listeners, null);
      fail();
    } catch (Exception e) {
      failIfNotMyException(e);
    }

    try {
      caller.fireEndOfFileEvent(listeners, null);
      fail();
    } catch (Exception e) {
      failIfNotMyException(e);
    }

    try {
      caller.fireErrorEvent(listeners, null, null, null);
      fail();
    } catch (Exception e) {
      failIfNotMyException(e);
    }

    assertTrue(listener.close);
    assertTrue(listener.connect);
    assertTrue(listener.eof);
    assertTrue(listener.error);
  }

  private static void failIfNotMyException(Exception e) {
    Throwable cause = e.getCause();
    if (cause instanceof MyException) { return; }
    fail("unexpected exception type: " + cause);
  }

  private static class Listener implements TCConnectionEventListener {

    boolean close, connect, eof, error;

    @Override
    public void closeEvent(TCConnectionEvent event) {
      close = true;
      throw new MyException();
    }

    @Override
    public void connectEvent(TCConnectionEvent event) {
      connect = true;
      throw new MyException();
    }

    @Override
    public void endOfFileEvent(TCConnectionEvent event) {
      eof = true;
      throw new MyException();
    }

    @Override
    public void errorEvent(TCConnectionErrorEvent errorEvent) {
      error = true;
      throw new MyException();
    }
  }

  private static class MyException extends RuntimeException {
    //
  }

}
