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
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.InBandMoveToNextSink;
import com.tc.config.HaConfig;
import com.tc.license.ProductID;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.context.NodeStateEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.TransactionBatchManager;

public class ChannelLifeCycleHandler extends AbstractEventHandler implements DSOChannelManagerEventListener {
  private final TransactionBatchManager transactionBatchManager;
  private final CommunicationsManager   commsManager;
  private final DSOChannelManager       channelMgr;
  private final HaConfig                haConfig;

  private TCLogger                      logger;
  private Sink                          channelSink;
  private Sink                          hydrateSink;
  private Sink                          processTransactionSink;

  public ChannelLifeCycleHandler(final CommunicationsManager commsManager,
                                 final TransactionBatchManager transactionBatchManager,
                                 final DSOChannelManager channelManager, final HaConfig haConfig) {
    this.commsManager = commsManager;
    this.transactionBatchManager = transactionBatchManager;
    this.channelMgr = channelManager;
    this.haConfig = haConfig;
  }

  @Override
  public void handleEvent(final EventContext context) {
    NodeStateEventContext event = (NodeStateEventContext) context;

    switch (event.getType()) {
      case NodeStateEventContext.CREATE: {
        nodeConnected(event.getNodeID(), event.getProductId());
        break;
      }
      case NodeStateEventContext.REMOVE: {
        nodeDisconnected(event.getNodeID(), event.getProductId());
        break;
      }
      default: {
        throw new AssertionError("unknown event: " + event.getType());
      }
    }
  }

  /**
   * These methods are called for both L1 and L2 when this server is in active mode. For L1s we go thru the cleanup of
   * sinks (@see below), for L2s group events will trigger this eventually.
   */
  private void nodeDisconnected(final NodeID nodeID, final ProductID productId) {
    broadcastClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_DISCONNECTED, nodeID, productId);
    if (commsManager.isInShutdown()) {
      logger.info("Ignoring transport disconnect for " + nodeID + " while shutting down.");
    } else {
      logger.info(": Received transport disconnect.  Shutting down client " + nodeID);
      transactionBatchManager.shutdownNode(nodeID);
    }
  }

  private void nodeConnected(final NodeID nodeID, final ProductID productId) {
    broadcastClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_CONNECTED, nodeID, productId);
    transactionBatchManager.nodeConnected(nodeID);
  }

  private void broadcastClusterMembershipMessage(final int eventType, final NodeID nodeID, final ProductID productId) {
    // only broadcast cluster membership messages for L1 nodes when the current server is the active coordinator
    if (haConfig.isActiveCoordinatorGroup() && NodeID.CLIENT_NODE_TYPE == nodeID.getNodeType()) {
      MessageChannel[] channels = channelMgr.getActiveChannels();
      for (MessageChannel channel : channels) {
        if (!channelMgr.getClientIDFor(channel.getChannelID()).equals(nodeID)) {
          ClusterMembershipMessage cmm = (ClusterMembershipMessage) channel
              .createMessage(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE);
          cmm.initialize(eventType, nodeID, productId);
          cmm.send();
        }
      }
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.logger = scc.getLogger(ChannelLifeCycleHandler.class);
    channelSink = scc.getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE).getSink();
    hydrateSink = scc.getStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK).getSink();
    processTransactionSink = scc.getStage(ServerConfigurationContext.PROCESS_TRANSACTION_STAGE).getSink();
  }

  @Override
  public void channelCreated(final MessageChannel channel) {
    channelSink.add(new NodeStateEventContext(NodeStateEventContext.CREATE, new ClientID(channel.getChannelID()
        .toLong()), channel.getProductId()));
  }

  @Override
  public void channelRemoved(final MessageChannel channel) {
    // We want all the messages in the system from this client to reach its destinations before processing this request.
    // esp. hydrate stage and process transaction stage. This goo is for that.
    final NodeStateEventContext disconnectEvent = new NodeStateEventContext(NodeStateEventContext.REMOVE,
                                                                            channel.getRemoteNodeID(), channel.getProductId());
    InBandMoveToNextSink context1 = new InBandMoveToNextSink(disconnectEvent, channelSink, channel.getRemoteNodeID());
    InBandMoveToNextSink context2 = new InBandMoveToNextSink(context1, processTransactionSink, channel.getRemoteNodeID());
    hydrateSink.add(context2);
  }

}
