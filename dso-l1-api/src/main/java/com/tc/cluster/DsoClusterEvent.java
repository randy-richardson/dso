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
package com.tc.cluster;

import com.tcclient.cluster.DsoNode;

/**
 * Indicates that the state of a node in the DSO cluster has changed.
 * <p>
 * Instances of the {@code DsoClusterEvent} are provided as arguments of the {@link DsoClusterListener} methods.
 *
 * @since 3.0.0
 */
public interface DsoClusterEvent {
  /**
   * Retrieves the node that this event talks about.
   *
   * @return the instance of the related node
   */
  public DsoNode getNode();
}