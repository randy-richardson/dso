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

import java.util.List;

public class MockMessageTransportFactory implements MessageTransportFactory {

  public MessageTransport transport;
  public int              callCount;

  @Override
  public MessageTransport createNewTransport() {
    callCount++;
    return transport;
  }

  @Override
  public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List transportListeners) {
    callCount++;
    if (transport != null) transport.initConnectionID(connectionID);
    return transport;
  }

  @Override
  public MessageTransport createNewTransport(ConnectionID connectionId, TCConnection connection,
                                             TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List transportListeners) {
    callCount++;
    if (transport != null) transport.initConnectionID(connectionId);
    return transport;
  }
}
