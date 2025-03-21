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
package com.terracotta.toolkit.express;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class URLConfigUtil {

  private static final Pattern pattern = Pattern.compile("\\$\\{.+?\\}");

  /**
   * Parses the input string for ${.*} patterns and expands individual pattern based on the system property
   * 
   * @param urlConfig ${tc_active}, ${tc_passive_1}, ${tc_passive_2}, ..
   * @return activeHost:9510, passive1Host:9510, passive2Host:9510, ..
   */
  public static String translateSystemProperties(final String urlConfig) {

    String rv = "";
    String[] urlConfigSources = urlConfig.split(",");

    for (String source : urlConfigSources) {
      source = source.trim();

      Set<String> properties = extractPropertyTokens(source);

      for (String token : properties) {
        String leftTrimmed = token.replaceAll("\\$\\{", "");
        String trimmedToken = leftTrimmed.replaceAll("\\}", "");

        String property = System.getProperty(trimmedToken);
        if (property != null) {
          String propertyWithQuotesProtected = Matcher.quoteReplacement(property);
          source = source.replaceAll("\\$\\{" + trimmedToken + "\\}", propertyWithQuotesProtected);
        }
      }

      if (source != null) {
        rv = rv + (rv == "" ? "" : ",") + source;
      }
    }
    return rv;
  }

  /**
   * Extracts properties of the form ${...}
   * 
   * @param sourceString
   * @return a Set of properties
   */
  static Set<String> extractPropertyTokens(String sourceString) {
    Set<String> propertyTokens = new HashSet<String>();
    Matcher matcher = pattern.matcher(sourceString);
    while (matcher.find()) {
      String token = matcher.group();
      propertyTokens.add(token);
    }
    return propertyTokens;
  }

  public static String getUsername(final String embeddedTcConfig) {
    final String translated = translateSystemProperties(embeddedTcConfig);
    final String[] split = translated.split(",");
    String username = null;
    for (String s : split) {
      final int index = s.indexOf('@');
      if (index != -1) {
        String tmpUsername = s.substring(0, index).trim();
        if (username != null && !username.equals(tmpUsername)) {
          throw new AssertionError(format("Invalid configuration: different username found in Terracotta connection URLs " +
                                          "- %s and %s",username, tmpUsername));
        }
        username = tmpUsername;
        try {
          username = URLDecoder.decode(username, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
          // cannot happen
        }
      }
    }
    return username;
  }

}
