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
package com.tc.management;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link TCManagementEvent} payload that originates from a server.
 *
 * @author Ludovic Orban
 */
public class TSAManagementEventPayload implements Serializable {

  private String type;
  private final Map<String, Object> attributes = new HashMap<String, Object>();

  public TSAManagementEventPayload() {
  }

  public TSAManagementEventPayload(String type) {
    this.type = type;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public String getType() {
    return type;
  }

  public TCManagementEvent toManagementEvent() {
    return new TCManagementEvent(this, getType());
  }

}
