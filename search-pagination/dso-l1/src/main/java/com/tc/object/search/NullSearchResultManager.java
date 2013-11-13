/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.search;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.SearchRequestID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.search.SearchQueryResults;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.aggregator.Aggregator;

import java.util.List;

public class NullSearchResultManager implements SearchResultManager {

  @Override
  public void addResponse(SessionID sessionID, SearchRequestID requestID, GroupID group,
                          List<IndexQueryResult> queryResults,
                          long totalResultCount, List<Aggregator> aggregators, NodeID nodeID, boolean anyCriteriaMatched) {
    //
  }

  @Override
  public void addErrorResponse(SessionID sessionID, SearchRequestID requestID, GroupID group, String errorMessage,
                               NodeID nodeID) {
    //
  }

  @Override
  public SearchQueryResults<IndexQueryResult> loadResults(final SearchRequestID requestID, final String cacheName,
                                                          int start, int size, GroupID from) {
    return null;
  }

  @Override
  public void pause(NodeID remoteNode, int disconnected) {
    //
  }

  @Override
  public void unpause(NodeID remoteNode, int disconnected) {
    //
  }

  @Override
  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    //
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    //
  }

  @Override
  public void cleanup() {
    //
  }

  @Override
  public void releaseResults(SearchRequestID request) {
    //
  }

  @Override
  public int getOpenResultSetCount() {
    return 0;
  }

}
