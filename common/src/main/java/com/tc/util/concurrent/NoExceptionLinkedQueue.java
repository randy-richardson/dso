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
package com.tc.util.concurrent;

import com.tc.util.Util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NoExceptionLinkedQueue extends LinkedBlockingQueue {

  @Override
  public void put(Object o) {
    boolean interrupted = false;
    while (true) {
      try {
        super.put(o);
        Util.selfInterruptIfNeeded(interrupted);
        return;
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
  }

  public boolean offer(Object o, long l) {
    try {
      return super.offer(o, l, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  public Object poll(long arg0) {
    try {
      return super.poll(arg0, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  @Override
  public Object take() {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return super.take();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(interrupted);
    }
  }

}
