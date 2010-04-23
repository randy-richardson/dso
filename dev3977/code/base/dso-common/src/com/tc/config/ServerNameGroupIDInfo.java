/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.GroupID;

public interface ServerNameGroupIDInfo {

  boolean containsServerName(String name);

  GroupID getGroupIDFromServerName(String name);

}