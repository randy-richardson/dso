package com.tc.objectserver.managedobject;

import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.ServerEntity;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.dna.impl.UTF8ByteDataHolder;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author twu
 */
public class EntityManagedObjectState extends LogicalManagedObjectState {
  private static final TCLogger logger = TCLogging.getLogger(EntityManagedObjectState.class);

  private ServerEntity serverEntity;

  public EntityManagedObjectState() {
    super(0);
  }

  @Override
  protected LogicalChangeResult applyLogicalAction(final ObjectID objectID, final ApplyTransactionInfo applyInfo, final LogicalOperation method, final Object[] params) {

    if (method == LogicalOperation.CREATE_ENTITY) {
      String typeName = ((UTF8ByteDataHolder) params[0]).asString();
      ServiceLoader<EntityServerService> loader = ServiceLoader.load(EntityServerService.class,
          EntityManagedObjectState.class.getClassLoader());
      for (EntityServerService entityServerService : loader) {
        if (entityServerService.handlesEntityType(typeName)) {
          serverEntity = entityServerService.createEntity(null);
          return LogicalChangeResult.SUCCESS;
        }
      }
      // TODO: give back an error indicating we can't handle this object type
      throw new RuntimeException("Can't find entity type");
    } else if (method == LogicalOperation.INVOKE_WITH_PAYLOAD) {
      if (serverEntity == null) {
        // TODO: throw some object not initialized error back to the client
        return LogicalChangeResult.FAILURE;
      }
      return new LogicalChangeResult(serverEntity.invoke((byte[]) params[0]));
    }
    // TODO: throw unknown operation exception back
    return LogicalChangeResult.FAILURE;
  }

  @Override
  protected void addAllObjectReferencesTo(final Set refs) {

  }

  @Override
  protected void basicWriteTo(final ObjectOutput out) throws IOException {

  }

  @Override
  protected boolean basicEquals(final LogicalManagedObjectState o) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {

  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNA.DNAType type) {

  }

  @Override
  public byte getType() {
    return (byte) 0x50; // magic number, very bad idea.
  }

}
