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
package com.tc.net.protocol.delivery;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class L2ReconnectConfigImpl extends AbstractReconnectConfig {

  private static final String NAME = "L2->L2 Reconnect Config";

  public L2ReconnectConfigImpl() {
    super(TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED),
          TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT),
          TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_SENDQUEUE_CAP),
          TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_MAX_DELAYEDACKS),
          TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_SEND_WINDOW), NAME);
  }

}
