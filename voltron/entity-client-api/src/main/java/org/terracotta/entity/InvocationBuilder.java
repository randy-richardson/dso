package org.terracotta.entity;

import java.io.Serializable;
import java.util.concurrent.Future;

/**
 * @author twu
 */
public interface InvocationBuilder {
  InvocationBuilder returnsValue(boolean returnsValue);

  InvocationBuilder payload(Serializable serializable);

  Future<?> invoke();
}
