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
package com.tc.net.protocol.transport;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.concurrent.ThreadUtil;

public class TransportHandshakeErrorHandlerForL1 implements TransportHandshakeErrorHandler {

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  @Override
  public void handleHandshakeError(final TransportHandshakeErrorContext e) {
    if (e.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) {
      consoleLogger.error(e.getMessage());
    } else if (e.getErrorType() == TransportHandshakeError.ERROR_RECONNECTION_REJECTED) {
      // do not log here because ClientChannelEventController will be logging this event as
      // TRANSPORT_RECONNECTION_REJECTED_EVENT
    } else {
      consoleLogger.error(e);
    }

    /**
     * These errors don't need sleep time before the next connect attempt. 1. ERROR_RECONNECTION_REJECTED: we want to
     * throw the rejected event asap so that rejoin will be attempted immediately. 2. ERROR_MAX_CONNECTION_EXCEED,
     * ERROR_STACK_MISMATCH : Client will be anyway killed at top layer by DOClient. However, Invalid ConnectionID and
     * other generic errors can be given some sleep time before the next connection attempt.
     */

    switch (e.getErrorType()) {
      case TransportHandshakeError.ERROR_STACK_MISMATCH:
      case TransportHandshakeError.ERROR_MAX_CONNECTION_EXCEED:
      case TransportHandshakeError.ERROR_RECONNECTION_REJECTED:
        // no sleep;
        break;
      default:
        ThreadUtil.reallySleep(30 * 1000);
    }

    switch (e.getErrorType()) {
      case TransportHandshakeError.ERROR_STACK_MISMATCH:
      case TransportHandshakeError.ERROR_MAX_CONNECTION_EXCEED:
        consoleLogger.error("Crashing the client due to handshake errors.");
        break;
    }

  }

}
