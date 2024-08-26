/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.util.properties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemPropertyCache {

  private static final Map<String, String> cache = new ConcurrentHashMap<>();

  private SystemPropertyCache() {}

  /**
   * Retrieves the value of the specified system property.
   * The value is cached after the first retrieval to avoid repeated access, ensuring consistent use of the initial value
   * throughout the application's runtime.
   * <p>
   *  * Note: While this caching prevents changes to the system property from affecting the application after the first access,
   *  * it does not inherently prevent attackers from modifying the system property at the source.
   *
   * @param propertyName the name of the system property
   * @return the cached value of the specified system property, or null if the property is not set
   */
  public static String getProperty(String propertyName) {
    return cache.computeIfAbsent(propertyName, System::getProperty);
  }
}
