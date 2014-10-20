package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.collections.ClusteredMap;
import org.terracotta.toolkit.collections.ClusteredMapService;
import org.terracotta.toolkit.entity.EntityConfiguration;

/**
 * @author twu
 */
public class TerracottaClusteredMapService implements ClusteredMapService {
  @Override
  public ClusteredMap create(final EntityConfiguration configuration) {
    return new TerracottaClusteredMap();
  }
}
