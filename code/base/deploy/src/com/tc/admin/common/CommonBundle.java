/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Font;
import java.text.DecimalFormat;
import java.util.ListResourceBundle;

public class CommonBundle extends ListResourceBundle {
  @Override
  public Object[][] getContents() {
    return new Object[][] { { "forums.url", "http://www.terracotta.org/kit/reflector?kitID={0}&pageID=Forums" },
        { "support.url", "http://www.terracotta.org/kit/reflector?kitID={0}&pageID=SupportServices" },
        { "visit.forums.title", "Visit Terracotta Forums" },
        { "contact.support.title", "Contact Terracotta Technical Support" }, { "waiting", "Waiting..." },
        { "canceled", "Canceled" }, { "cancel", "Cancel" }, { "delete", "Delete" }, { "delete.all", "Delete All" },
        { "ok", "OK" }, { "shutdown", "Shutdown" }, { "details.link.text", "Details >>" },
        { "back.link.text", "<< Back" }, { "connect.elipses", "Connect..." }, { "disconnect", "Disconnect" },
        { "connecting", "Connecting..." }, { "not.connected", "Not connected" }, { "next", "Next" },
        { "previous", "Previous" }, { "refresh", "Refresh" }, { "options.dialog.title", "Options" },
        { "node.environment", "Environment" }, { "node.logging.settings", "Logging Settings" },
        { "node.config", "Config" }, { "node.main", "Main" },
        { "copyright", "Copyright Terracotta, Inc. All rights reserved." }, { "about.prefix", "About " },
        { "system.info", "System Information" }, { "header.label.font", new Font("Dialog", Font.BOLD, 12) },
        { "embedded.chart.label.font", new Font("SanSerif", Font.PLAIN, 9) },
        { "chart.regular.font", new Font("DialogInput", Font.PLAIN, 9) },
        { "chart.large.font", new Font("DialogInput", Font.PLAIN, 10) },
        { "chart.extra-large.font", new Font("DialogInput", Font.PLAIN, 14) },
        { "textarea.font", new Font("Monospaced", Font.PLAIN, 12) },
        { "message.label.font", new Font("Dialog", Font.PLAIN, 14) },
        { "cluster.not.ready.msg", "Cluster is not yet ready for action.  Are all the mirror groups active?" },
        { "initializing", "Initializing..." }, { "dial.text.font", new Font("DialogInput", Font.PLAIN, 14) },
        { "dial.value.font", new Font("Monospaced", Font.PLAIN, 14) },
        { "dial.value.format", new DecimalFormat("#,###") },
        { "connect-dialog.connecting.format", "Connecting to {0} ..." }, { "connect-dialog.username", "Username:" },
        { "connect-dialog.password", "Password:" }, { "connect-dialog.credentials", "Credentials" },
        { "connect-dialog.timed-out", "Timed-out" } };
  }
}
