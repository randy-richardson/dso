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

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.List;

public class MockMessageTransport implements MessageTransport {
  public ConnectionID connectionId;
  public NetworkLayer receiveLayer;

  public List         listeners = new ArrayList();

  @Override
  public ConnectionID getConnectionId() {
    return this.connectionId;
  }

  @Override
  public void addTransportListeners(List toAdd) {
    listeners.addAll(toAdd);
  }

  @Override
  public void addTransportListener(MessageTransportListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeTransportListeners() {
    listeners.clear();
  }

  @Override
  public void setSendLayer(NetworkLayer layer) {
    throw new ImplementMe();
  }

  @Override
  public void setReceiveLayer(NetworkLayer layer) {
    this.receiveLayer = layer;
  }

  @Override
  public void send(TCNetworkMessage message) {
    throw new ImplementMe();
  }

  @Override
  public void receive(TCByteBuffer[] msgData) {
    throw new ImplementMe();
  }

  @Override
  public boolean isConnected() {
    throw new ImplementMe();
  }

  @Override
  public NetworkStackID open() {
    throw new ImplementMe();
  }

  @Override
  public void close() {
    throw new ImplementMe();
  }

  @Override
  public void attachNewConnection(TCConnection connection) {
    throw new ImplementMe();
  }

  @Override
  public void receiveTransportMessage(WireProtocolMessage message) {
    throw new ImplementMe();
  }

  public final NoExceptionLinkedQueue sendToConnectionCalls = new NoExceptionLinkedQueue();

  @Override
  public void sendToConnection(TCNetworkMessage message) {
    sendToConnectionCalls.put(message);
  }

  @Override
  public TCSocketAddress getRemoteAddress() {
    throw new ImplementMe();
  }

  @Override
  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  @Override
  public void setAllowConnectionReplace(boolean b) {
    throw new ImplementMe();
  }

  @Override
  public short getStackLayerFlag() {
    throw new ImplementMe();
  }

  @Override
  public String getStackLayerName() {
    throw new ImplementMe();
  }

  @Override
  public NetworkLayer getReceiveLayer() {
    throw new ImplementMe();
  }

  @Override
  public short getCommunicationStackFlags(NetworkLayer parentLayer) {
    return NetworkLayer.TYPE_TEST_MESSAGE;
  }

  @Override
  public String getCommunicationStackNames(NetworkLayer parentLayer) {
    throw new ImplementMe();
  }

  @Override
  public void setRemoteCallbackPort(int callbackPort) {
    //
  }

  @Override
  public int getRemoteCallbackPort() {
    throw new ImplementMe();
  }

  @Override
  public void initConnectionID(ConnectionID cid) {
    connectionId = cid;
  }

  @Override
  public void reset() {
    throw new ImplementMe();
  }
}
