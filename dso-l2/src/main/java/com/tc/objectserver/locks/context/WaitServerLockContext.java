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
package com.tc.objectserver.locks.context;

import com.tc.net.ClientID;
import com.tc.object.locks.ThreadID;

import java.util.TimerTask;

public class WaitServerLockContext extends LinkedServerLockContext {
  private TimerTask  task;
  private final long timeout;

  public WaitServerLockContext(ClientID clientID, ThreadID threadID, long timeout) {
    this(clientID, threadID, timeout, null);
  }

  public WaitServerLockContext(ClientID clientID, ThreadID threadID, long timeout, TimerTask task) {
    super(clientID, threadID);
    this.timeout = timeout;
    this.task = task;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimerTask(TimerTask task) {
    this.task = task;
  }

  public TimerTask getTimerTask() {
    return task;
  }
}
