package com.tc.admin.model;

import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.stats.TerracottaOperatorEventsStatsImpl;
import com.tc.stats.TerracottaOperatorEventStats;

import java.util.Date;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.event.EventListenerList;

public class OperatorEventsListener implements NotificationListener {
  
  private final EventListenerList listenerList;

  public OperatorEventsListener(EventListenerList listenerList) {
    this.listenerList = listenerList;
  }

  public void handleNotification(Notification notification, Object handback) {
    TerracottaOperatorEvent tcOperatorEvent = (TerracottaOperatorEvent) notification.getSource();
    fireOperatorEvent(tcOperatorEvent);
  }

  private void fireOperatorEvent(TerracottaOperatorEvent tcOperatorEvent) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TerracottaOperatorEventsListener.class) {
        TerracottaOperatorEventStats tcOperatorEventStats = new TerracottaOperatorEventsStatsImpl(
                                                                                                  new Date(
                                                                                                           tcOperatorEvent
                                                                                                               .getEventTime())
                                                                                                      .toString(),
                                                                                                  tcOperatorEvent
                                                                                                      .getEventType()
                                                                                                      .name(),
                                                                                                  tcOperatorEvent
                                                                                                      .getEventSubsystem()
            .name(), tcOperatorEvent.getNodeId(), tcOperatorEvent.getEventMessage());
        ((TerracottaOperatorEventsListener) listeners[i + 1]).statusUpdate(tcOperatorEventStats);
      }
    }
  }

}