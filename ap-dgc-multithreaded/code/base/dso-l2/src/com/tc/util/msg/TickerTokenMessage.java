/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.msg;

import com.tc.net.NodeID;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TickerTokenMessage implements GroupMessage {
  
  private final boolean dirty;
  
  public TickerTokenMessage(boolean dirty) {
    this.dirty = dirty;
  }
  
  public boolean isDirty() {
    return dirty;
  }

  public void dehydrate() {
    //
  }

  public MessageChannel getChannel() {
    return null;
  }

  public NodeID getDestinationNodeID() {
    return null;
  }

  public SessionID getLocalSessionID() {
    return null;
  }

  public TCMessageType getMessageType() {
    return null;
  }

  public NodeID getSourceNodeID() {
    return null;
  }

  public int getTotalLength() {
    return 0;
  }

  public void hydrate() throws IOException, UnknownNameException {
 // 
    if(false)
      throw new IOException();
    if(false)
      throw new UnknownNameException(Class.class,(byte)1);
  }

  public void send() {
   //
  }

  public MessageID getMessageID() {
    return null;
  }

  public int getType() {
     return 0;
  }

  public MessageID inResponseTo() {
    return null;
  }

  public NodeID messageFrom() {
    return null;
  }

  public void setMessageOrginator(NodeID n) {
    //
  }

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
   if(false)
     throw new IOException();
   if(false)
     throw new ClassNotFoundException();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    throw new IOException();
    
  }

}
