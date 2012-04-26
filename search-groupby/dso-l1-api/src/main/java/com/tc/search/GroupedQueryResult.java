/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import com.tc.object.metadata.NVPair;

import java.util.List;

public interface GroupedQueryResult extends IndexQueryResult {

  public List<?> getAggregatorResults();

  public List<NVPair> getGroupedAttributes();
}
