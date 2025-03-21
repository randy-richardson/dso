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
package com.tc.objectserver.locks;

/**
 * Thrown by server side locks when an illegal attempt to wait/notify is performed
 */
public class TCIllegalMonitorStateException extends IllegalStateException {

  public TCIllegalMonitorStateException() {
    super();
  }

  public TCIllegalMonitorStateException(String message) {
    super(message);
  }

  public TCIllegalMonitorStateException(Throwable cause) {
    super(cause);
  }

  public TCIllegalMonitorStateException(String message, Throwable cause) {
    super(message, cause);
  }

}