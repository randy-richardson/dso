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
package com.tc.lang;

/**
 * The purpose of this class to execute a startup action (ie. "start the server", or "start the client", etc) in a
 * thread in the specified thread group. The side effect of doing this is that any more threads spawned by the startup
 * action will inherit the given thread group. It is somewhat fragile, and sometimes impossible (see java.util.Timer) to
 * be explicit about the thread group when spawning threads
 */
public class StartupHelper {

  private final StartupAction action;
  private final ThreadGroup threadGroup;

  public StartupHelper(ThreadGroup threadGroup, StartupAction action) {
    this.threadGroup = threadGroup;
    this.action = action;
  }

  public void startUp() {
    Runnable r = () -> {
      try {
        action.execute();
      } catch (Throwable t) {
        threadGroup.uncaughtException(Thread.currentThread(), t);
        throw new RuntimeException(t);
      }
    };

    Thread th = new Thread(threadGroup, r);
    th.start();

    try {
      th.join();
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }

  public interface StartupAction {
    void execute() throws Throwable;
  }

}
