/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import com.tc.object.metadata.NVPair;
import com.tc.search.aggregator.Aggregator;

import java.util.List;

public interface GroupedQueryResult extends IndexQueryResult {

  public List<Aggregator> getAggregators();

  public List<NVPair> getGroupedAttributes();
}
