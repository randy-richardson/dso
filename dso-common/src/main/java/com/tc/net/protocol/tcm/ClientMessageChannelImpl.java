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
package com.tc.net.protocol.tcm;

import com.tc.license.ProductID;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.SecurityInfo;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.JvmIDUtil;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import com.tc.security.PwProvider;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * @author orion
 */

public class ClientMessageChannelImpl extends AbstractMessageChannel implements ClientMessageChannel {
  private static final TCLogger           logger           = TCLogging.getLogger(ClientMessageChannel.class);
  private int                             connectAttemptCount;
  private int                             connectCount;
  private ChannelID                       channelID;
  private final ChannelIDProviderImpl     channelIdProvider;
  private final SessionProvider           sessionProvider;
  private final SecurityInfo              securityInfo;
  private final PwProvider                pwProvider;
  private volatile SessionID              channelSessionID = SessionID.NULL_ID;
  private final ConnectionAddressProvider addressProvider;

  protected ClientMessageChannelImpl(final TCMessageFactory msgFactory, final TCMessageRouter router,
                                     final SessionProvider sessionProvider, final NodeID remoteNodeID,
                                     final SecurityInfo securityInfo, final PwProvider pwProvider,
                                     final ConnectionAddressProvider addressProvider, final ProductID productId) {
    super(router, logger, msgFactory, remoteNodeID, productId);
    this.securityInfo = securityInfo;
    this.pwProvider = pwProvider;
    this.addressProvider = addressProvider;
    this.channelIdProvider = new ChannelIDProviderImpl();
    this.sessionProvider = sessionProvider;
    this.sessionProvider.initProvider(remoteNodeID);
  }

  @Override
  public void reset() {
    init();
  }

  protected void init() {
    ChannelStatus status = getStatus();
    status.reset();
    this.sendLayer.reset();
    setLocalNodeID(ClientID.NULL_ID);
  }

  @Override
  public NetworkStackID open() throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    final ChannelStatus status = getStatus();

    synchronized (status) {
      char[] pw = null;
      if (status.isOpen()) { throw new IllegalStateException("Channel already open"); }
      // initialize the connection ID, using the local JVM ID
      String username = null;
      if (securityInfo.hasCredentials()) {
        username = securityInfo.getUsername();
        Assert.assertNotNull("TCSecurityManager", pwProvider);
        pw = getPassword();
      }
      final ConnectionID cid = new ConnectionID(JvmIDUtil.getJvmID(), (((ClientID) getLocalNodeID()).toLong()),
                                                username, pw, getProductId());
      ((MessageTransport) this.sendLayer).initConnectionID(cid);
      final NetworkStackID id = this.sendLayer.open();
      this.channelID = new ChannelID(id.toLong());
      setLocalNodeID(new ClientID(id.toLong()));
      this.channelIdProvider.setChannelID(this.channelID);
      this.channelSessionID = this.sessionProvider.getSessionID(getRemoteNodeID());
      channelOpened();
      return id;
    }
  }

  @Override
  public NetworkStackID open(char[] pw) throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    return open();
  }

  @Override
  public void reopen() throws Exception {
    reset();
    open();
  }

  private char[] getPassword() {
    char[] password = null;
    if (securityInfo.hasCredentials()) {
      Assert.assertNotNull("TCSecurityManager should not be null", pwProvider);
      // use user-password of first server in the group
      ConnectionInfo connectionInfo = addressProvider.getIterator().next();
      logger.debug("Getting password for " + securityInfo.getUsername());
      password = pwProvider.getPasswordForTC(securityInfo.getUsername(), connectionInfo.getHostname(),
                                           connectionInfo.getPort());
      Assert.assertNotNull("password is null from securityInfo " + securityInfo, password);
    } else {
      logger.debug("Non-secure mode, no password needed");
    }
    return password;
  }

  @Override
  public ChannelID getChannelID() {
    final ChannelStatus status = getStatus();
    synchronized (status) {
      if (!status.isOpen()) {
        logger.warn("Attempt to get the channel ID of an unopened channel - " + this.channelID);
      }
      return this.channelID;
    }
  }

  @Override
  public int getConnectCount() {
    return this.connectCount;
  }

  @Override
  public int getConnectAttemptCount() {
    return this.connectAttemptCount;
  }

  /*
   * Session message filter. To drop old session msgs when session changed.
   */
  @Override
  public void send(final TCNetworkMessage message) {
    if (this.channelSessionID == ((DSOMessageBase) message).getLocalSessionID()) {
      super.send(message);
    } else {
      logger.info("Drop old message: " + ((DSOMessageBase) message).getMessageType() + " Expected "
                  + this.channelSessionID + " but got " + ((DSOMessageBase) message).getLocalSessionID());
    }
  }

  @Override
  public void notifyTransportConnected(final MessageTransport transport) {
    // TODO: review this.
    long channelIdLong = transport.getConnectionId().getChannelID();
    this.channelID = new ChannelID(channelIdLong);
    this.channelIdProvider.setChannelID(this.channelID);
    setLocalNodeID(new ClientID(channelIdLong));
    super.notifyTransportConnected(transport);
    this.connectCount++;
  }

  @Override
  public void notifyTransportDisconnected(final MessageTransport transport, final boolean forcedDisconnect) {
    this.channelSessionID = this.sessionProvider.nextSessionID(getRemoteNodeID());
    logger.info("ClientMessageChannel moves to next session for remote node " + getRemoteNodeID());
    super.notifyTransportDisconnected(transport, forcedDisconnect);
  }

  @Override
  public void notifyTransportConnectAttempt(final MessageTransport transport) {
    super.notifyTransportConnectAttempt(transport);
    this.connectAttemptCount++;
  }

  @Override
  public void notifyTransportClosed(final MessageTransport transport) {
    //
  }

  @Override
  public void notifyTransportClosedOnStart(MessageTransport transport) {
    //  no-op
  }

  @Override
  public ChannelIDProvider getChannelIDProvider() {
    return this.channelIdProvider;
  }

  private static class ChannelIDProviderImpl implements ChannelIDProvider {

    private ChannelID channelID = ChannelID.NULL_ID;

    private synchronized void setChannelID(final ChannelID channelID) {
      this.channelID = channelID;
    }

    @Override
    public synchronized ChannelID getChannelID() {
      return this.channelID;
    }

  }

  // for testing purpose
  protected SessionID getSessionID() {
    return this.channelSessionID;
  }

}
