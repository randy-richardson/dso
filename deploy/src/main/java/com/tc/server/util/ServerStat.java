/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.server.util;

import com.tc.cli.CommandLineBuilder;
import com.tc.cli.ManagementToolUtil;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class ServerStat {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private static final int MAX_TRIES = 10;
  private static final int RETRY_INTERVAL = 2000;

  private static final String UNKNOWN                 = "unknown";
  private static final String NEWLINE                 = System.getProperty("line.separator");

  private final String        host;
  private final String        hostName;

  private final int                 port;
  private final boolean             connected;
  private final String              groupName;
  private final String              errorMessage;
  private final String              initialState;
  private final String              state;
  private final String              role;
  private final String              health;

  private ServerStat(String host, int port, String error) {
    this.errorMessage = error;
    this.connected = false;
    this.port = port;
    this.groupName = UNKNOWN;
    this.initialState = UNKNOWN;
    this.state = UNKNOWN;
    this.role = UNKNOWN;
    this.health = UNKNOWN;
    this.host = host;
    this.hostName = null;
  }

  private ServerStat(String host, String hostAlias, int port, String groupName, String initialState, String state,
                     String role, String health) {
    this.host = host;
    this.hostName = hostAlias;
    this.port = port;
    this.groupName = groupName;
    this.initialState = initialState;
    this.state = state;
    this.role = role;
    this.health = health;
    this.connected = true;
    this.errorMessage = "";
  }

  public String getInitialState() {
    return initialState;
  }

  public String getState() {
    return state;
  }

  public String getRole() {
    return role;
  }

  public String getHealth() {
    return health;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Finds and returns the name of the group which this server belongs to.
   */
  public String getGroupName() {
    if (!connected) return UNKNOWN;
    return groupName;
  }

  @Override
  public String toString() {
    String serverId = hostName != null ? hostName : host;
    StringBuilder sb = new StringBuilder();
    sb.append(serverId + ".health: " + getHealth()).append(NEWLINE);
    sb.append(serverId + ".role: " + getRole()).append(NEWLINE);
    sb.append(serverId + ".initialState: " + getInitialState()).append(NEWLINE);
    sb.append(serverId + ".state: " + getState()).append(NEWLINE);
    sb.append(serverId + ".port: " + port).append(NEWLINE);
    sb.append(serverId + ".group name: " + getGroupName()).append(NEWLINE);
    if (!connected) {
      sb.append(serverId + ".error: " + errorMessage).append(NEWLINE);
    }
    return sb.toString();
  }

  public static void main(String[] args) throws Exception {
    String usage = " server-stat -s host1,host2" + NEWLINE + "       server-stat -s host1:9540,host2:9540" + NEWLINE
                   + "       server-stat -f /path/to/tc-config.xml" + NEWLINE;

    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ServerStat.class.getName(), args);
    ManagementToolUtil.addConnectionOptionsTo(commandLineBuilder, true);
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.setUsageMessage(usage);
    commandLineBuilder.parse();

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie();
    }

    for (WebTarget target : ManagementToolUtil.getTargets(commandLineBuilder, true)) {
      System.out.println(getStats(target));
    }
  }

  public static ServerStat getStats(WebTarget target) throws IOException {
    Response response;
    String host = target.getUri().getHost();
    int port = target.getUri().getPort();
    String hostPort = host + ":" + port;
    for (int i = 0; i < MAX_TRIES; i++) {
      try {
        response = target.path("/tc-management-api/v2/local/stat").request(APPLICATION_JSON_TYPE).get();
      } catch (RuntimeException e) {
        Throwable rootCause = getRootCause(e);
        consoleLogger.info("Failed to issue status request to " + hostPort + ": " + rootCause.getMessage() + "; retrying.");
        ThreadUtil.reallySleep(RETRY_INTERVAL);
        continue;
      }

      try {
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
          Map<String, String> map = response.readEntity(Map.class);
          return new ServerStat(host, map.get("name"), port, map.get("serverGroupName"), map.get("initialState"),
            map.get("state"), map.get("role"), map.get("health"));
        } else if (response.getStatus() == 401) {
          throw new IOException("Authentication error while connecting to " + hostPort + ", check username/password and try again.");
        } else if (response.getStatus() == 404) {
          consoleLogger.warn("Got a 404 while connecting to " + hostPort + ". Management service might not be started " +
            "yet; retrying.");
          ThreadUtil.reallySleep(RETRY_INTERVAL);
        } else {
          Map<String, ?> errorResponse = response.readEntity(Map.class);
          consoleLogger.error(errorResponse.get("stackTrace"));
          throw new IOException(format("Error getting status for server %s: %s", hostPort, errorResponse.get("error")));
        }
      } finally {
        try {
          response.close();
        } catch (Exception ignore) {}
      }
    }
    throw new IOException(format("Unable to get status for %s after %d tries", hostPort, MAX_TRIES));
  }

  private static Throwable getRootCause(Throwable e) {
    Throwable t = e;
    while (t != null) {
      e = t;
      t = t.getCause();
    }
    return e;
  }

  public static ServerStat getStats(String host, int port, String username, String password,
                                    boolean secured, boolean ignoreUntrusted)
      throws KeyManagementException, NoSuchAlgorithmException, IOException {
    return getStats(ManagementToolUtil.targetFor(host, port, username, password, secured, ignoreUntrusted));
  }
}
