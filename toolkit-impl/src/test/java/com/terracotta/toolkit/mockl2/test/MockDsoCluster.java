/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.terracotta.toolkit.mockl2.test;

import com.tc.async.api.Stage;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.DsoClusterTopologyImpl;
import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClusterMetaDataManager;
import com.tcclient.cluster.DsoClusterInternal;
import com.tcclient.cluster.DsoNode;
import com.tcclient.cluster.DsoNodeInternal;
import com.tcclient.cluster.DsoNodeMetaData;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MockDsoCluster implements DsoClusterInternal {
  
  @Override
  public DsoNode waitUntilNodeJoinsCluster() {
    throw new ImplementMe();
  }
  
  @Override
  public void removeClusterListener(DsoClusterListener listener) {
    throw new ImplementMe();
    
  }
  
  @Override
  public boolean isNodeJoined() {
    throw new ImplementMe();
  }
  
  @Override
  public DsoNode getCurrentNode() {
    throw new ImplementMe();
  }
  
  @Override
  public DsoClusterTopology getClusterTopology() {
    MockUtil.logInfo("DSO Cluster : getClusterTopology");
    return new DsoClusterTopologyImpl(this);
  }
  
  @Override
  public boolean areOperationsEnabled() {
    throw new ImplementMe();
  }
  
  @Override
  public void addClusterListener(DsoClusterListener listener) {
 MockUtil.logInfo("DSO Cluster : add Cluster Listener");
    
  }

  @Override
  public void fireThisNodeJoined(ClientID nodeId, ClientID[] clusterMembers) {
    throw new ImplementMe();

  }

  @Override
  public void fireThisNodeLeft() {
    throw new ImplementMe();

  }

  @Override
  public void fireNodeJoined(ClientID nodeId) {
    throw new ImplementMe();

  }

  @Override
  public void fireNodeLeft(ClientID nodeId) {
    throw new ImplementMe();

  }

  @Override
  public void fireOperationsEnabled() {
    throw new ImplementMe();

  }

  @Override
  public void fireOperationsDisabled() {
    throw new ImplementMe();

  }

  @Override
  public void fireNodeError() {
    throw new ImplementMe();

  }

  @Override
  public void notifyDsoClusterListener(DsoClusterEventType eventType, DsoClusterEvent event, DsoClusterListener listener) {
    throw new ImplementMe();

  }

  @Override
  public void cleanup() {
    throw new ImplementMe();

  }

  @Override
  public void init(ClusterMetaDataManager metaDataManager, ClientObjectManager objectManager,
                   Stage dsoClusterEventsStage) {
    throw new ImplementMe();

  }

  @Override
  public void shutdown() {
    throw new ImplementMe();

  }

  @Override
  public DsoNodeMetaData retrieveMetaDataForDsoNode(DsoNodeInternal node) {
    throw new ImplementMe();
  }

  @Override
  public <K> Map<K, Set<DsoNode>> getNodesWithKeys(Map<K, ?> map, Collection<? extends K> keys) {
    throw new ImplementMe();
  }
}
