/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import com.tc.object.ObjectID;

public interface NonGroupedQueryResult extends IndexQueryResult {
  /**
   * Entry key.
   * 
   * @return key
   */
  public String getKey();

  /**
   * Entry value.
   * 
   * @return value
   */
  public ObjectID getValue();

}
