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
package com.tc.object.locks;

import com.tc.abortable.AbortedOperationException;

public interface ClientLockManagerTestMethods {
  /**
   * Called by test code to force a lock gc pass.
   * 
   * @return the count of live locks (post collection)
   */
  public int runLockGc();

  public void wait(LockID lock, WaitListener listener, Object waitObject) throws InterruptedException,
      AbortedOperationException;

  public void wait(LockID lock, WaitListener listener, Object waitObject, long timeout) throws InterruptedException,
      AbortedOperationException;
}
