package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.collections.ClusteredMap;
import com.terracotta.toolkit.collections.ClusteredMapService;
import com.terracotta.toolkit.entity.EntityClientEndpoint;

import org.terracotta.toolkit.entity.EntityConfiguration;

/**
 * @author twu
 */
public class TerracottaClusteredMapService implements ClusteredMapService {
  @Override
  public ClusteredMap create(final EntityClientEndpoint endpoint, final EntityConfiguration configuration) {
    return new TerracottaClusteredMap(endpoint);
  }
}
