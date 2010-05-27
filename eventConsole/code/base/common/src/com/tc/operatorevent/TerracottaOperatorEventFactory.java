/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.net.NodeID;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;

import java.text.MessageFormat;

public class TerracottaOperatorEventFactory {

  public static TerracottaOperatorEvent createLongGCOperatorEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getLongGCMessage(), arguments));
  }

  public static TerracottaOperatorEvent createDGCStartedEvent() {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, TerracottaOperatorEventResources
        .getDGCStartedMessage());
  }

  public static TerracottaOperatorEvent createDGCFinishedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, MessageFormat
        .format(TerracottaOperatorEventResources.getDGCFinishedMessage(), arguments));
  }

  public static TerracottaOperatorEvent createDGCCanceledEvent() {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, TerracottaOperatorEventResources
        .getDGCCanceledMessage());
  }

  public static TerracottaOperatorEvent createNodeConnectedEvent(NodeID nodeID) {
    Object[] arguments = { nodeID.toString(), "joined" };
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), arguments));
  }
  
  public static TerracottaOperatorEvent createNodeDisconnectedEvent(NodeID nodeID) {
    Object[] arguments = { nodeID.toString(), "left" };
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), arguments));
  }

  public static TerracottaOperatorEvent createLockGCEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.LOCK_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getLockGCMessage(), arguments));
  }

  public static TerracottaOperatorEvent createMoveToPassiveStandByEvent() {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, TerracottaOperatorEventResources
        .getMoveToPassiveStandByMessage());
  }

  public static TerracottaOperatorEvent createHighMemoryUsageEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getHighMemoryUsageMessage(), arguments));
  }
}
