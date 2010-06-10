/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

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

  public static TerracottaOperatorEvent createNodeConnectedEvent(String nodeName) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), new Object[] { nodeName, "joined" }));
  }

  public static TerracottaOperatorEvent createNodeDisconnectedEvent(String nodeName) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), new Object[] { nodeName, "left" }));
  }

  public static TerracottaOperatorEvent createLockGCEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.LOCK_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getLockGCMessage(), arguments));
  }

  public static TerracottaOperatorEvent createHighMemoryUsageEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getHighMemoryUsageMessage(), arguments));
  }

  public static TerracottaOperatorEvent createOOODisconnectEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.ERROR, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getOOODisconnectMessage(), arguments));
  }

  public static TerracottaOperatorEvent createOOOConnectedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getOOOConnectMessage(), arguments));
  }

  public static TerracottaOperatorEvent createClusterNodeStateChangedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getClusterNodeStateChangedMessage(), arguments));
  }

  /**
   * zap events
   */
  public static TerracottaOperatorEvent createZapRequestReceivedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.CRITICAL, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getZapRequestReceivedMessage(), arguments));
  }

  public static TerracottaOperatorEvent getZapRequestAcceptedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.CRITICAL, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getZapRequestAcceptedMessage(), arguments));
  }
}
