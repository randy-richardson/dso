package com.terracotta.toolkit.entity;

import org.terracotta.toolkit.entity.EntityConfiguration;

import com.tc.abortable.AbortedOperationException;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObjectSelfImpl;

import java.io.Serializable;
import java.util.concurrent.Future;

/**
 * @author twu
 */
public class EntityClientEndpoint extends TCObjectSelfImpl {
  // TODO: Handle saving configuration here?
  private String typeName;

  public EntityClientEndpoint() {
  }

  public EntityClientEndpoint(final String typeName, EntityConfiguration entityConfiguration) {
    this.typeName = typeName;
  }

  void setEntityConfiguration(EntityConfiguration entityConfiguration) {
    // Should only call this during mmode
  }

  public Future<?> asyncInvoke(final LogicalOperation method, final boolean returnsValue, Object... parameters) throws AbortedOperationException {
    return getTCClass().getObjectManager().getTransactionManager().asyncInvoke(this, method, returnsValue, parameters);
  }

  EntityConfiguration getEntityConfiguration() {
    return null;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

  public InvocationBuilder beginInvoke() {
    return new InvocationBuilder();
  }

  public class InvocationBuilder {
    private boolean invoked = false;
    private boolean returnsValue = false;
    private Serializable payload;

    // TODO: fill in durability/consistency options here.

    public synchronized InvocationBuilder returnsValue(boolean returnsValue) {
      this.returnsValue = returnsValue;
      return this;
    }

    public synchronized InvocationBuilder payload(Serializable serializable) {
      checkInvoked();
      payload = serializable;
      return this;
    }

    public synchronized Future<?> invoke() throws AbortedOperationException {
      checkInvoked();
      invoked = true;
      return asyncInvoke(LogicalOperation.INVOKE_WITH_PAYLOAD, returnsValue, payload);
    }

    private void checkInvoked() {
      if (invoked) {
        throw new IllegalStateException("Already invoked");
      }
    }
  }
}
