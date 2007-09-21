/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.groups.NodeID;
import com.tc.net.protocol.TCNetworkMessage;

public interface ClientMessageChannelMultiplex extends ClientMessageChannel {
  
  public ClientMessageChannel[] getChannels();
  
  public NodeID[] getMultiplexIDs();
  
  public ClientMessageChannel getChannel(NodeID id);
  
  public void broadcast(final TCNetworkMessage message);
  
  public TCMessage createMessage(NodeID id, TCMessageType type);
  
  public ClientMessageChannel getActiveCoordinator();
  
  public ChannelID getActiveActiveChannelID();

}
