package com.terracotta.toolkit.entity;

import org.terracotta.connection.entity.EntityConfiguration;
import org.terracotta.entity.EndpointListener;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;

import com.google.common.util.concurrent.Futures;
import com.tc.abortable.AbortedOperationException;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObjectSelfImpl;

import java.util.concurrent.Future;

/**
 * @author twu
 */
public class EntityClientEndpointImpl extends TCObjectSelfImpl implements EntityClientEndpoint {
  // TODO: Handle saving configuration here?
  private String typeName;

  public EntityClientEndpointImpl() {
  }

  public EntityClientEndpointImpl(final String typeName, EntityConfiguration entityConfiguration) {
    this.typeName = typeName;
  }

  @Override
  public void setEntityConfiguration(EntityConfiguration entityConfiguration) {
    // Should only call this during mmode
  }

  private Future<?> asyncInvoke(final LogicalOperation method, final boolean returnsValue, Object... parameters) throws AbortedOperationException {
    return getTCClass().getObjectManager().getTransactionManager().asyncInvoke(this, method, returnsValue, parameters);
  }

  @Override
  public EntityConfiguration getEntityConfiguration() {
    return null;
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

  @Override
  public void registerListener(final EndpointListener listener) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public InvocationBuilder beginInvoke() {
    return new InvocationBuilderImpl();
  }

  private class InvocationBuilderImpl implements InvocationBuilder {
    private boolean invoked = false;
    private boolean returnsValue = false;
    private byte[] payload;

    // TODO: fill in durability/consistency options here.

    @Override
    public synchronized InvocationBuilderImpl returnsValue(boolean returnsValue) {
      this.returnsValue = returnsValue;
      return this;
    }

    @Override
    public synchronized InvocationBuilderImpl payload(byte[] payload) {
      checkInvoked();
      this.payload = payload;
      return this;
    }

    @Override
    public synchronized Future<?> invoke() {
      checkInvoked();
      invoked = true;
      try {
        return asyncInvoke(LogicalOperation.INVOKE_WITH_PAYLOAD, returnsValue, new Object[] { payload });
      } catch (AbortedOperationException e) {
        return Futures.immediateFailedFuture(e);
      }
    }

    private void checkInvoked() {
      if (invoked) {
        throw new IllegalStateException("Already invoked");
      }
    }
  }
}
