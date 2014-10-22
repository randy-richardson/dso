package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

/**
 * @author twu
 */
public class EntityManagedObjectState extends AbstractManagedObjectState {
  private static final TCLogger logger = TCLogging.getLogger(EntityManagedObjectState.class);

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo applyInfo) throws IOException {
    while (cursor.next()) {
      final LogicalAction logicalAction = cursor.getLogicalAction();
      if (logicalAction.getLogicalOperation() == LogicalOperation.CREATE_ENTITY) {
        logger.info("Creating type " + logicalAction.getParameters()[0]);
      } else if (logicalAction.getLogicalOperation() == LogicalOperation.INVOKE_WITH_PAYLOAD) {
        logger.info("Invocation with params " + logicalAction.getParameters()[0]);
      }
    }
  }

  @Override
  public Set<ObjectID> getObjectReferences() {
    return Collections.emptySet();
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {

  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNA.DNAType type) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public byte getType() {
    return (byte) 0x50; // magic number, very bad idea.
  }

  @Override
  public String getClassName() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void writeTo(final ObjectOutput o) throws IOException {

  }
}
