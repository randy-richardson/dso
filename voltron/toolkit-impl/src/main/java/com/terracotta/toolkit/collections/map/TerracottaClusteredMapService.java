package com.terracotta.toolkit.collections.map;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityCreationService;
import org.terracotta.toolkit.collections.ClusteredMap;
import org.terracotta.toolkit.entity.EntityConfiguration;

/**
 * @author twu
 */
public class TerracottaClusteredMapService implements EntityCreationService<ClusteredMap> {
  @Override
  public boolean handlesEntityType(final Class<ClusteredMap> cls) {
    System.out.println("Testing " + cls);
    return cls == ClusteredMap.class;
  }

  @Override
  public ClusteredMap create(final EntityClientEndpoint endpoint, final EntityConfiguration configuration) {
    return new TerracottaClusteredMap(endpoint);
  }
}
