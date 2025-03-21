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
package com.terracotta.management.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.OperatorEventEntityV2;
import com.terracotta.management.resource.services.utils.TimeStringParser;
import com.terracotta.management.service.OperatorEventsServiceV2;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class OperatorEventsServiceImplV2 implements OperatorEventsServiceV2 {
  private static final Logger LOG = LoggerFactory.getLogger(OperatorEventsServiceImplV2.class);
  private final ServerManagementServiceV2 serverManagementService;

  public OperatorEventsServiceImplV2(ServerManagementServiceV2 serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public ResponseEntityV2<OperatorEventEntityV2> getOperatorEvents(Set<String> serverNames, String sinceWhen, String eventTypes, String eventLevels, boolean read) throws ServiceExecutionException {
    Set<String> acceptableLevels = null;
    if (eventLevels != null) {
      acceptableLevels = new HashSet<String>(Arrays.asList(eventLevels.split(",")));
    }
    
    Set<String> acceptableTypes = null;
    if (eventTypes != null) {
      acceptableTypes = new HashSet<String>(Arrays.asList(eventTypes.split(",")));
    }

    if (sinceWhen == null) {
      return serverManagementService.getOperatorEvents(serverNames, null, acceptableTypes, acceptableLevels, read);
    } else {
      try {
        return serverManagementService.getOperatorEvents(serverNames, TimeStringParser.parseTime(sinceWhen), acceptableTypes, acceptableLevels, read);
      } catch (NumberFormatException nfe) {
        throw new ServiceExecutionException("Illegal time string: [" + sinceWhen + "]", nfe);
      }
    }
  }
  @Override
  public boolean markOperatorEvents(Collection<OperatorEventEntityV2> operatorEventEntities, boolean read) throws ServiceExecutionException {
    boolean result = true;
    for (OperatorEventEntityV2 operatorEventEntity : operatorEventEntities) {
      try {
        result &= serverManagementService.markOperatorEvent(operatorEventEntity, read);
      } catch (Exception e) {
        result = false;
        LOG.debug("Failed to mark operator event: " + operatorEventEntity, e);
      }
    }
    return result;
  }

  @Override
  public Map<String, Integer> getUnreadCount(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getUnreadOperatorEventCount(serverNames);
  }
}
