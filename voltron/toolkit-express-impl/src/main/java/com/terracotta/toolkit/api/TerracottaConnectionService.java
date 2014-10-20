package com.terracotta.toolkit.api;

import org.terracotta.toolkit.connection.Connection;
import org.terracotta.toolkit.connection.ConnectionException;
import org.terracotta.toolkit.connection.ConnectionService;

import com.terracotta.toolkit.client.TerracottaClientConfig;
import com.terracotta.toolkit.client.TerracottaClientConfigParams;
import com.terracotta.toolkit.express.TerracottaInternalClient;
import com.terracotta.toolkit.express.TerracottaInternalClientStaticFactory;

import java.net.URI;
import java.util.Properties;

/**
 * @author twu
 */
public class TerracottaConnectionService implements ConnectionService {
  private static final String SCHEME = "terracotta";
  private static final String PLATFORM_SERVICE_CLASS_NAME = "com.tc.platform.PlatformService";
  private static final String CONNECTION_CLASS_NAME = "com.terracotta.toolkit.TerracottaConnection";

  @Override
  public boolean handlesURI(final URI uri) {
    return SCHEME.equals(uri.getScheme());
  }

  @Override
  public Connection connect(final URI uri, final Properties properties) throws ConnectionException {
    if (!handlesURI(uri)) {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    // TODO: Make use of those properties

    TerracottaClientConfig clientConfig = new TerracottaClientConfigParams().isUrl(true)
        .tcConfigSnippetOrUrl(uri.getHost() + ":" + uri.getPort()).newTerracottaClientConfig();
    final TerracottaInternalClient client = TerracottaInternalClientStaticFactory.getOrCreateTerracottaInternalClient(clientConfig);
    client.init();
    try {
      return client.instantiate(CONNECTION_CLASS_NAME,
          new Class[] { client.loadClass(PLATFORM_SERVICE_CLASS_NAME), Runnable.class },
          new Object[] { client.getPlatformService(), new Runnable() {
            @Override
            public void run() {
              client.shutdown();
            }
          } });
    } catch (Exception e) {
      throw new ConnectionException(e);
    }
  }
}
