/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.connect;

import com.tc.management.TerracottaOperatorEventsMBean;

import java.io.Serializable;

import javax.management.Notification;
import javax.management.NotificationFilter;

class OperatorEventsFilter implements NotificationFilter, Serializable {
  public boolean isNotificationEnabled(Notification notification) {
    if (notification.getType().equals(TerracottaOperatorEventsMBean.TERRACOTTA_OPERATOR_EVENT)) return true;
    return false;
  }
}
