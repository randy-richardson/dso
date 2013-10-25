/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;

public interface SearchResultsRequestMessage extends SearchRequestMessage {

  /**
   * Starting offset
   */
  public int getStart();

  /**
   * Desired page size
   */
  public int getPageSize();

  /**
   * Initialize this message
   */
  public void initialize(final String cacheName, ClientID clientId, SearchRequestID reqId, int start, int pageSize);

}
