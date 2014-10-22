package com.tc.objectserver.managedobject;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author twu
 */
public class EntityManagedObjectState extends LogicalManagedObjectState {
  private static final TCLogger logger = TCLogging.getLogger(EntityManagedObjectState.class);

  private final Map<String, String> stuff = new HashMap<String, String>();

  public EntityManagedObjectState() {
    super(0);
  }

  @Override
  protected LogicalChangeResult applyLogicalAction(final ObjectID objectID, final ApplyTransactionInfo applyInfo, final LogicalOperation method, final Object[] params) {
    if (method == LogicalOperation.CREATE_ENTITY) {
      logger.info("Creating type " + params[0]);
    } else if (method == LogicalOperation.INVOKE_WITH_PAYLOAD) {
      String[] invocation = ((UTF8ByteDataHolder) params[0]).asString().split(" ");
      if ("put".equals(invocation[0])) {
        stuff.put(invocation[1], invocation[2]);
      } else if ("get".equals(invocation[0])) {
        return new LogicalChangeResult(stuff.get(invocation[1]));
      }
    }
    return LogicalChangeResult.SUCCESS;
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
