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
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.RemoteResourceManager;
import com.tc.object.msg.ResourceManagerThrottleMessage;

/**
 * @author tim
 */
public class ResourceManagerMessageHandler extends AbstractEventHandler {

  private final RemoteResourceManager remoteResourceManager;

  public ResourceManagerMessageHandler(final RemoteResourceManager remoteResourceManager) {
    this.remoteResourceManager = remoteResourceManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ResourceManagerThrottleMessage) {
      ResourceManagerThrottleMessage msg = (ResourceManagerThrottleMessage)context;
      remoteResourceManager.handleThrottleMessage(msg.getGroupID(), msg.getThrowException(), msg.getThrottle());
    } else {
      throw new AssertionError("Wrong context received in Resource Management Message sink. Type " + context.getClass());
    }
  }
}
