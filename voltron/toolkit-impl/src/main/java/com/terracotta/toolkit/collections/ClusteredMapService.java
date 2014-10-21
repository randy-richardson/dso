package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ClusteredMap;
import org.terracotta.toolkit.entity.EntityConfiguration;

import com.terracotta.toolkit.entity.EntityClientEndpoint;
import com.terracotta.toolkit.entity.EntityCreationService;

/**
 * @author twu
 */
public interface ClusteredMapService extends EntityCreationService {
  ClusteredMap<?, ?> create(EntityClientEndpoint endpoint, EntityConfiguration configuration);
}
