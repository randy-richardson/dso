/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.search;

import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.SearchRequestID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;
import com.tc.search.SearchQueryResults;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.aggregator.Aggregator;

import java.util.List;

public interface SearchResultManager extends ClientHandshakeCallback {
  void addResponse(final SessionID sessionID, final SearchRequestID requestID, final GroupID group,
                                  final List<IndexQueryResult> queryResults, final long totalResultCount,
                                  final List<Aggregator> aggregators, final NodeID nodeID, boolean anyCriteriaMatched);

  void addErrorResponse(final SessionID sessionID, final SearchRequestID requestID, final GroupID group,
                                       final String errorMessage, final NodeID nodeID);

  SearchQueryResults<IndexQueryResult> loadResults(final SearchRequestID requestID, final String cacheName, int start,
                                                   int size, GroupID from) throws AbortedOperationException;

  void releaseResults(SearchRequestID request) throws AbortedOperationException;

  int getOpenResultSetCount();
}
