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

import com.google.common.collect.Maps;
import com.tc.license.ProductID;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * provides the sessionIDs
 * 
 * @author steve
 */
class ChannelManagerImpl implements ChannelManager, ChannelEventListener, ServerMessageChannelFactory {
  private static final TCLogger                 logger              = TCLogging.getLogger(ChannelManager.class);
  private static final MessageChannelInternal[] EMPTY_CHANNEL_ARARY = new MessageChannelInternal[] {};

  private final Map<ChannelID, MessageChannelInternal> channels;
  private final boolean                         transportDisconnectRemovesChannel;
  private final ServerMessageChannelFactory     channelFactory;
  private final List                            eventListeners      = new CopyOnWriteArrayList();

  public ChannelManagerImpl(boolean transportDisconnectRemovesChannel, ServerMessageChannelFactory channelFactory) {
    this.channels = Maps.newHashMap();
    this.transportDisconnectRemovesChannel = transportDisconnectRemovesChannel;
    this.channelFactory = channelFactory;
  }

  @Override
  public MessageChannelInternal createNewChannel(ChannelID id, final ProductID productId) {
    MessageChannelInternal channel = channelFactory.createNewChannel(id, productId);
    synchronized (this) {
      channels.put(channel.getChannelID(), channel);
      channel.addListener(this);
    }
    return channel;
  }

  private void fireChannelCreatedEvent(MessageChannel channel) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      ChannelManagerEventListener eventListener = (ChannelManagerEventListener) iter.next();
      eventListener.channelCreated(channel);
    }
  }

  private void fireChannelRemovedEvent(MessageChannel channel) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      ChannelManagerEventListener eventListener = (ChannelManagerEventListener) iter.next();
      eventListener.channelRemoved(channel);
    }
  }

  @Override
  public synchronized MessageChannelInternal getChannel(ChannelID id) {
    return channels.get(id);
  }

  @Override
  public synchronized MessageChannelInternal[] getChannels() {
    return channels.values().toArray(EMPTY_CHANNEL_ARARY);
  }

  @Override
  public synchronized void closeAllChannels() {
    MessageChannelInternal[] channelsCopy = getChannels();
    for (MessageChannelInternal element : channelsCopy) {
      element.close();
    }
    Assert.assertEquals(0, channels.size());
  }

  @Override
  public synchronized Set getAllChannelIDs() {
    return new HashSet(channels.keySet());
  }

  @Override
  public synchronized boolean isValidID(ChannelID channelID) {
    if (channelID == null) { return false; }

    final MessageChannel channel = getChannel(channelID);

    if (channel == null) {
      logger.warn("no channel found for " + channelID);
      return false;
    }

    return true;
  }

  @Override
  public void notifyChannelEvent(ChannelEvent event) {
    MessageChannel channel = event.getChannel();

    if (ChannelEventType.CHANNEL_CLOSED_EVENT.matches(event)) {
      removeChannel(channel);
    } else if (ChannelEventType.TRANSPORT_DISCONNECTED_EVENT.matches(event)) {
      if (this.transportDisconnectRemovesChannel) {
        channel.close();
      }
    } else if (ChannelEventType.TRANSPORT_CONNECTED_EVENT.matches(event)) {
      fireChannelCreatedEvent(channel);
    }
  }

  private void removeChannel(MessageChannel channel) {
    boolean notfound;
    synchronized (this) {
      notfound = (channels.remove(channel.getChannelID()) == null);
    }
    if (notfound) {
      logger.warn("Remove non-exist channel:" + channel.getChannelID());
      return;
    }
    fireChannelRemovedEvent(channel);
  }

  @Override
  public synchronized void addEventListener(ChannelManagerEventListener listener) {
    if (listener == null) { throw new IllegalArgumentException("listener must be non-null"); }
    this.eventListeners.add(listener);
  }

}
