/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.util.collections;

import com.tc.logging.TCLogger;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class LoggingBlockingQueueTest {

  @Test
  public void testLoggingThresholdBehaviors() throws InterruptedException {
    TCLogger mockLogger = mock(TCLogger.class);
    BlockingQueue<Boolean> queue = new LoggingBlockingQueue<>(new LinkedBlockingQueue<>(), 10, mockLogger, "%s");

    for (int i = 0; i < 9; i++) {
      queue.put(Boolean.TRUE);
    }
    verifyZeroInteractions(mockLogger);
    queue.put(Boolean.TRUE);
    verify(mockLogger, times(1)).info("10");

    for (int i = 0; i < 9; i++) {
      queue.put(Boolean.TRUE);
    }
    verifyZeroInteractions(mockLogger);
    queue.put(Boolean.TRUE);
    verify(mockLogger, times(1)).info("20");

    for (int i = 0; i < 9; i++) {
      queue.take();
    }
    verifyZeroInteractions(mockLogger);
    queue.take();
    verify(mockLogger, times(2)).info("10");

    for (int i = 0; i < 9; i++) {
      queue.take();
    }
    verifyZeroInteractions(mockLogger);
    queue.take();
    verify(mockLogger, times(1)).info("0");
  }
}