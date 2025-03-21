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

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

public class MockServerMessageTransport extends ServerMessageTransport {
  public final NoExceptionLinkedQueue sendToConnectionCalls = new NoExceptionLinkedQueue();

  public MockServerMessageTransport(ConnectionID connectionId, TCConnection conn,
                                    TransportHandshakeErrorHandler handshakeErrorHandler,
                                    TransportHandshakeMessageFactory messageFactory) {
    super(connectionId, conn, handshakeErrorHandler, messageFactory);
  }

  @Override
  public void sendToConnection(TCNetworkMessage message) {
    sendToConnectionCalls.put(message);
  }

}
