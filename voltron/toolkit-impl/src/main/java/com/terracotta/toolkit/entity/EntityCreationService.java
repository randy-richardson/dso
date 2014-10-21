package com.terracotta.toolkit.entity;

import org.terracotta.toolkit.entity.Entity;
import org.terracotta.toolkit.entity.EntityConfiguration;

/**
 * @author twu
 */
public interface EntityCreationService {
  <T extends Entity> T create(final EntityClientEndpoint endpoint, EntityConfiguration configuration);
}
