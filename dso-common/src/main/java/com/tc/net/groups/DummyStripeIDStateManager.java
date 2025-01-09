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
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.StripeID;

import java.util.HashMap;
import java.util.Map;

public class DummyStripeIDStateManager implements StripeIDStateManager {

  @Override
  public StripeID getStripeID(GroupID gid) {
    return StripeID.NULL_ID;
  }

  @Override
  public Map<GroupID, StripeID> getStripeIDMap(boolean fromAACoordinator) {
    return new HashMap();
  }

  @Override
  public boolean isStripeIDMatched(GroupID gid, StripeID stripeID) {
    return true;
  }

  @Override
  public void registerForStripeIDEvents(StripeIDEventListener listener) {
    // NOP
  }

  @Override
  public boolean verifyOrSaveStripeID(GroupID gid, StripeID stripeID, boolean overwrite) {
    return true;
  }

}
